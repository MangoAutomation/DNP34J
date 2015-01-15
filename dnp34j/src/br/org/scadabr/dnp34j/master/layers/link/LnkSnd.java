package br.org.scadabr.dnp34j.master.layers.link;

import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.LnkFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.common.utils.DnpCrc;
import br.org.scadabr.dnp34j.master.common.utils.Lock;
import br.org.scadabr.dnp34j.master.common.utils.Queue;
import br.org.scadabr.dnp34j.master.common.utils.Utils;
import br.org.scadabr.dnp34j.master.layers.physical.PhyLayer;
import br.org.scadabr.dnp34j.master.layers.transport.TransportLayer;

/**
 * 
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class LnkSnd extends Thread implements LnkFeatures, InitFeatures {
    static final boolean DEBUG = !LNK_QUIET;

    // =============================================================================
    // Attributes
    // =============================================================================
    private Buffer frameRcv;
    private Buffer frameSnd;
    private Buffer previousFrameSnd; // for retries
    private Buffer lnkSndBuffer;
    private Queue lnkSndQueue;
    private Lock lnkSndLock;
    private Lock conLnkSndLock;
    private TransportLayer transportLayer;
    private LnkRcv lnkRcv;
    private PhyLayer phyLayer;
    private int addressToReportTo; // refers to a remote station address

    /**
     * Creates a new LnkSnd object.
     * 
     * @param parent
     *            DOCUMENT ME!
     */
    public LnkSnd() {
        lnkSndBuffer = new Buffer(M);
        lnkSndQueue = new Queue();
        lnkSndLock = new Lock();
        conLnkSndLock = new Lock();
        frameRcv = new Buffer(S);
        frameSnd = new Buffer(S);
        previousFrameSnd = new Buffer(M);

        addressToReportTo = 1;
    }

    // =============================================================================
    // Methods
    // =============================================================================
    // Waiting frames from Link Layer (confirm frames) and Transport layer (Data
    // frames)
    public void run() {
        try {
            while (!lnkRcv.isSTOP()) {
                // something to send (from transport layer)?
                while (!lnkRcv.isSTOP() && lnkSndQueue.empty())
                    // waiting something to send
                    lnkSndLock.waiting(20);

                if (lnkSndQueue.empty())
                    continue;

                // System.out.println("lnkSndLock.lock");
                lnkSndLock.lock();

                // a confirmation may be expected
                int remainingRetries = lnkRcv.getLnkMaxRetries();

                while (!lnkRcv.isSTOP()) {
                    if (conLnkSndLock.waiting(lnkRcv.getLnkTimeout())) {
                        /*
                         * enters here if - no confirmation is expected - or a
                         * confirmation is expected and - is already received -
                         * timeout has not expired
                         * 
                         * -> confirmation received
                         */
                        if (lnkRcv.isLnkConfirm()) {
                            // a confirmation is now expected
                            conLnkSndLock.lock();
                            send(true, CON_DATA);
                        }
                        else {
                            send(true, UNCON_DATA);
                        }

                        break;
                    }

                    if (DEBUG)
                        System.out.println("LnkSnd error");

                    /*
                     * enters here if - confirmation is expected and -
                     * timeout has expired
                     * 
                     * -> confirmation not received
                     */
                    if (remainingRetries > 0) {
                        sendPreviousFrame();
                        remainingRetries--;
                    }
                    else {
                        // notifies this error to application layer
                        lnkRcv.handleConnectionError();

                        break;
                    }
                    // end else
                }

                // end while (retries)
            }

            // end while (run)
        }
        catch (Throwable t) {
            System.out.print("[MasterLnkSnd] ");
            System.out.println(t);
        }
    }

    // end run()

    /**
     * DOCUMENT ME!
     * 
     * @param PRI
     *            DOCUMENT ME!
     * @param FC
     *            DOCUMENT ME!
     */
    public synchronized void send(boolean PRI, byte FC) throws Exception {
        frameSnd.setMarker(0);
        frameSnd.reset();
        frameSnd.writeBytes(lnkRcv.getBASIS());

        // remoteAddress field
        int remoteAddress = lnkRcv.getDNPAddressList()[addressToReportTo];
        frameSnd.getBuffer()[4] = (byte) (remoteAddress & 0xFF);
        frameSnd.getBuffer()[5] = (byte) ((remoteAddress >> 8) & 0x00FF);

        if (PRI) {
            sendPrimaryMessage(FC, remoteAddress);
        }
        else {
            sendSecondaryMessage(FC);
        }
    }

    // i'm the secondary station, and i send a CONFIRM or a RESPOND message
    private void sendSecondaryMessage(byte FC) throws Exception {
        frameSnd.getBuffer()[3] |= FC;
        frameSnd.writeBytes(DnpCrc.makeCRC(frameSnd.value()));

        if (DEBUG) {
            System.out.println("[LinkLayer] send secondary msg" + Utils.Display(frameSnd.value()));
        }

        write(frameSnd.readBytes());
    }

    // i'm the primary station, and i send data requiring (or not) a
    // confirmation
    private void sendPrimaryMessage(byte FC, int remoteAddress) throws Exception {
        frameRcv.reset();

        // get data if it's not a reset link function
        if (FC != RESET_LINK) {
            frameRcv.writeBytes(lnkSndBuffer.readBytes(lnkSndQueue.pop()));
        }

        // length field
        frameSnd.getBuffer()[2] = (byte) (5 + frameRcv.length());

        // control field
        frameSnd.getBuffer()[3] |= (byte) (FC + 0x40 + ((lnkRcv.getSendFcb()[addressToReportTo]) ? 0x20 : 0x00) + ((FC == CON_DATA) ? 0x10
                : 0x00));

        // CRC field (header)
        frameSnd.writeBytes(DnpCrc.makeCRC(frameSnd.value()));
        while (frameRcv.length() > 0) {
            int length = Math.min(16, frameRcv.length());
            byte[] frag = frameRcv.readBytes(length);
            frameSnd.writeBytes(frag);
            frameSnd.writeBytes(DnpCrc.makeCRC(frag));
        }
        if (DEBUG) {
            System.out.println("[LinkLayer] send primary msg" + Utils.Display(frameSnd.value()));
        }

        // store current frame for possible retries
        previousFrameSnd.reset();
        previousFrameSnd.writeBytes(frameSnd.value());
        // send this frame
        write(frameSnd.readBytes());
        // Invert Fcb (Frame Control Bit)
        if (remoteAddress == BROADCAST) {
            for (int i = 1; i < lnkRcv.getDNPAddressList().length; i++) {
                lnkRcv.getSendFcb()[i] = !lnkRcv.getSendFcb()[i];
            }
        }
        else {
            lnkRcv.getSendFcb()[addressToReportTo] = !lnkRcv.getSendFcb()[addressToReportTo];
        }
    }

    // For the purpose of doing retries, send previous frame
    protected void sendPreviousFrame() throws Exception {
        write(previousFrameSnd.value());
    }

    /**
     * Send "final" frame to the remote DNP device "final" means there's no more
     * DNP encapsulation process. Other encapsulation may be processed, with
     * TCP/IP for exemple.
     */
    public synchronized void write(byte[] someByte) throws Exception {
        if (lnkRcv.isSTOP()) {
            return;
        }

        phyLayer.write(someByte);
    }

    /**
     * @return the frameRcv
     */
    public Buffer getFrameRcv() {
        return frameRcv;
    }

    /**
     * @param frameRcv
     *            the frameRcv to set
     */
    public void setFrameRcv(Buffer frameRcv) {
        this.frameRcv = frameRcv;
    }

    /**
     * @return the frameSnd
     */
    public Buffer getFrameSnd() {
        return frameSnd;
    }

    /**
     * @param frameSnd
     *            the frameSnd to set
     */
    public void setFrameSnd(Buffer frameSnd) {
        this.frameSnd = frameSnd;
    }

    /**
     * @return the previousFrameSnd
     */
    public Buffer getPreviousFrameSnd() {
        return previousFrameSnd;
    }

    /**
     * @param previousFrameSnd
     *            the previousFrameSnd to set
     */
    public void setPreviousFrameSnd(Buffer previousFrameSnd) {
        this.previousFrameSnd = previousFrameSnd;
    }

    /**
     * @return the lnkSndBuffer
     */
    public Buffer getLnkSndBuffer() {
        return lnkSndBuffer;
    }

    /**
     * @param lnkSndBuffer
     *            the lnkSndBuffer to set
     */
    public void setLnkSndBuffer(Buffer lnkSndBuffer) {
        this.lnkSndBuffer = lnkSndBuffer;
    }

    /**
     * @return the lnkSndQueue
     */
    public Queue getLnkSndQueue() {
        return lnkSndQueue;
    }

    /**
     * @param lnkSndQueue
     *            the lnkSndQueue to set
     */
    public void setLnkSndQueue(Queue lnkSndQueue) {
        this.lnkSndQueue = lnkSndQueue;
    }

    /**
     * @return the lnkSndLock
     */
    public Lock getLnkSndLock() {
        return lnkSndLock;
    }

    /**
     * @param lnkSndLock
     *            the lnkSndLock to set
     */
    public void setLnkSndLock(Lock lnkSndLock) {
        this.lnkSndLock = lnkSndLock;
    }

    /**
     * @return the conLnkSndLock
     */
    public Lock getConLnkSndLock() {
        return conLnkSndLock;
    }

    /**
     * @param conLnkSndLock
     *            the conLnkSndLock to set
     */
    public void setConLnkSndLock(Lock conLnkSndLock) {
        this.conLnkSndLock = conLnkSndLock;
    }

    /**
     * @return the transportLayer
     */
    public TransportLayer getTransportLayer() {
        return transportLayer;
    }

    /**
     * @param transportLayer
     *            the transportLayer to set
     */
    public void setTransportLayer(TransportLayer transportLayer) {
        this.transportLayer = transportLayer;
    }

    /**
     * @return the lnkRcv
     */
    public LnkRcv getLnkRcv() {
        return lnkRcv;
    }

    /**
     * @param lnkRcv
     *            the lnkRcv to set
     */
    public void setLnkRcv(LnkRcv lnkRcv) {
        this.lnkRcv = lnkRcv;
    }

    /**
     * @return the phyLayer
     */
    public PhyLayer getPhyLayer() {
        return phyLayer;
    }

    /**
     * @param phyLayer
     *            the phyLayer to set
     */
    public void setPhyLayer(PhyLayer phyLayer) {
        this.phyLayer = phyLayer;
    }

    /**
     * @return the addressToReportTo
     */
    public int getAddressToReportTo() {
        return addressToReportTo;
    }

    /**
     * @param addressToReportTo
     *            the addressToReportTo to set
     */
    public void setAddressToReportTo(int addressToReportTo) {
        this.addressToReportTo = addressToReportTo;
    }
}