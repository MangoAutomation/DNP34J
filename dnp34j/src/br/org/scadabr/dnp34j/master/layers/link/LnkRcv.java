package br.org.scadabr.dnp34j.master.layers.link;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.LnkFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.common.utils.DnpCrc;
import br.org.scadabr.dnp34j.master.common.utils.Utils;
import br.org.scadabr.dnp34j.master.layers.DataMap;
import br.org.scadabr.dnp34j.master.layers.ThreadStopException;
import br.org.scadabr.dnp34j.master.layers.physical.PhyLayer;
import br.org.scadabr.dnp34j.master.layers.transport.TransportLayer;
import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;

/**
 * 
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class LnkRcv extends Thread implements LnkFeatures, InitFeatures {
    private static final int DATA_PAUSE_TIME = 20; // milliseconds

    private static final Logger LOG = LoggerFactory.getLogger(LnkRcv.class);

    private volatile boolean STOP = false;

    // =============================================================================
    // Attributes
    // =============================================================================
    // parameters from User
    private DNPConfig config;
    private boolean DIR; // true if i'm the master
    private byte ADDRESS_1; // source
    private byte ADDRESS_0;
    private boolean lnkConfirm;

    // if true, responding station sends confirmation messages
    private long lnkTimeout = 1000;
    private int lnkMaxRetries = 0;

    // variables used to ckeck the header of link frames
    private byte control;
    private boolean error;

    // buffers and pre-builded frames
    private byte[] BASIS;

    // intra-connection between layers
    private DNPUser user;
    private TransportLayer transportLayer;
    private PhyLayer phyLayer;
    private LnkSnd lnkSnd;

    // from secondary
    private boolean dfc;

    // Data Flow Control bit ->> true : when buffer is full, to avoid overflow
    // RS485 hack (pb with Luciol)
    private int ignoreNextBytes;
    private Buffer frameRcv;

    // following parameters are tied to a remote station
    private int[] DNPAddressList;

    // destinations : BROADCAST, remoteStation 1, remoteStation 2, ... ,
    // remoteStation n
    private int currentRemoteStation; // refer to a remote station

    // from primary
    private boolean[] receiveFcb;

    // state of previous Frame Control Bit (Alternate Bit) received
    private boolean[] sendFcb;

    // state of previous Frame Control Bit (Alternate Bit) sent
    // buffers
    private Buffer[] previousFrameRcv;

    // =============================================================================
    // Constructor
    // =============================================================================
    public LnkRcv(DNPUser user) throws Exception {
        setUser(user);
        setTransportLayer(user.getTranspLayer());

        setConfig(user.getConfig());

        setDIR(true);
        setDfc(false);

        try {
            setADDRESS_1((byte) ((config.getMasterAddress() >> 8) & 0xFF));
            setADDRESS_0((byte) (config.getMasterAddress() & 0xFF));

            // setLnkConfirm(config.isREQ_LNK_CONFIRM());
            // setLnkTimeout((long) config.getLNK_CONFIRM_TIMEOUT());
            // setLnkMaxRetries(config.getMAX_LNK_RETRIES());
        }
        catch (Exception e) {
            System.out.println("[LnkRcv] - Retrieval of DNPConfig attributes failed");
            System.out.println("[LnkRcv] - Exception throwed");
            throw new Exception(e);
        }

        BASIS = new byte[8];
        BASIS[0] = START_0;
        BASIS[1] = START_1;
        BASIS[2] = EMPTY_LENGTH;
        BASIS[3] = (byte) ((DIR) ? 0x80 : 0x00);
        BASIS[4] = (byte) 0; // undefined yet
        BASIS[5] = (byte) 0; // undefined yet
        BASIS[6] = ADDRESS_0;
        BASIS[7] = ADDRESS_1;

        setIgnoreNextBytes(0);

        setFrameRcv(new Buffer(M));

        currentRemoteStation = 1;

        setDNPAddressList(config.getDNPAddressList());
        receiveFcb = new boolean[DNPAddressList.length];

        sendFcb = new boolean[DNPAddressList.length];

        previousFrameRcv = new Buffer[DNPAddressList.length];

        for (int i = 0; i < DNPAddressList.length; i++) {
            previousFrameRcv[i] = new Buffer(M);
        }

    }

    // =============================================================================
    // Methods
    // =============================================================================
    public void init() throws Exception {
        sendPrimaryMsg(RESET_LINK);

        if (!lnkSnd.getConLnkSndLock().waiting(lnkTimeout)) {
            System.out.println("timeout exceed");
            init();
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("[LinkLayer] initialized");
        }
    }

    public boolean initLink(long timeout) throws Exception {
        lnkSnd.getConLnkSndLock().lock();
        sendPrimaryMsg(RESET_LINK);
        return lnkSnd.getConLnkSndLock().waiting(timeout);
    }

    /**
     * DOCUMENT ME!
     */
    public void run() {
        try {
            while (!STOP) {
                // ignore next bytes if RS485 is used
                // ignoreNextBytes();
                // byte#0 & byte#1
                startHandler();

                frameRcv.reset();
                frameRcv.writeByte(START_0);
                frameRcv.writeByte(START_1);

                byte length = 0;
                int remaining;

                try {
                    // byte#2
                    waitForAvailable();
                    length = (byte) phyLayer.getInputStream().read();
                    frameRcv.writeByte(length);

                    // byte#3 ..#n
                    remaining = (Utils.byte2int(length) + 2 + (2 * ((Utils.byte2int(length) + 10) / 16)));

                    // System.out.println("BYTES REMAINING: " + remaining);

                    while (remaining > 0) {
                        int available = phyLayer.getInputStream().available();
                        int size = Math.min(remaining, available);

                        remaining -= size;

                        byte[] remainingBytes = new byte[size];
                        phyLayer.getInputStream().read(remainingBytes);
                        frameRcv.writeBytes(remainingBytes);
                    }
                }
                catch (IOException e) {
                    handleConnectionError();

                    break;
                }

                if(LOG.isDebugEnabled()) {
                    LOG.debug("[LinkLayer] received " + Utils.Display(frameRcv.value()));
                }

                // header CRC check
                error = !DnpCrc.checkCRC(frameRcv.value(0, 9));

                if (LOG.isDebugEnabled() && error) {
                    LOG.debug("[LinkLayer] error header CRC check");
                }

                if (rightAddress()) {
                    control = frameRcv.value(3);
                    byte function = (byte) (control & 0x0F);

                    if (((control & 0x40) == 0x40) || (function == CON_DATA) || (function == UNCON_DATA)) {
                        primaryHandler();
                    }
                    else {
                        secondaryHandler();
                    }
                }
            }
        }
        catch (ThreadStopException e) {
            // Ignore
        }
        catch (Exception t) {
            System.out.print("[MasterLnkRcv] ");
            t.printStackTrace();
            System.out.println(t);
        }
    }

    //    /**
    //     * DOCUMENT ME!
    //     */
    //    private void ignoreNextBytes() throws Exception {
    //        while (ignoreNextBytes > 0) {
    //            try {
    //                int size = Math.min(ignoreNextBytes, phyLayer.getInputStream().available());
    //                ignoreNextBytes -= size;
    //                phyLayer.getInputStream().skip(size);
    //            }
    //            catch (IOException e) {
    //            }
    //        }
    //    }

    /**
     * DOCUMENT ME!
     * 
     * @throws Exception
     */
    private void startHandler() throws Exception {
        boolean valid = false;
        byte next;

        try {
            while (!valid) {
                waitForAvailable();

                next = (byte) phyLayer.getInputStream().read();
                // aguarda primeiro byte que deve ser 0x05
                if (next == START_0) {
                    waitForAvailable();

                    // aguarda segundo byte que deve ser 0x64
                    next = (byte) phyLayer.getInputStream().read();

                    if (next == START_1) {
                        valid = true;
                    }
                    else {
                        if (true) {
                            System.out.println("[LinkLayer] " + Utils.DisplayByte(next)
                                    + " is not a DNP3 header. Byte ignored");
                        }

                        while (next == START_0) {
                            waitForAvailable();
                            next = (byte) phyLayer.getInputStream().read();
                        }

                        if (next == START_1) {
                            valid = true;
                        }
                    }
                }
                else {
                    if (next == -1) {
                        // System.out.println("IHHHHHHHHHH");
                        // throw new IOException();
                    }
                    else {
                        if (true) {
                            System.out.println("[LinkLayer] " + Utils.DisplayByte(next)
                                    + " is not a DNP3 header. Byte ignored");
                        }
                    }
                }
            }
        }
        catch (ThreadStopException e) {
            // Ignore
        }
        catch (IOException e) {
            e.printStackTrace();
            handleConnectionError();
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private boolean rightAddress() throws Exception {
        boolean valid = false;

        if ((frameRcv.value(4) != ADDRESS_0) || (frameRcv.value(5) != ADDRESS_1)) // wrong address
        {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[LinkLayer] dest address doesn't match");
            }

            return false;
        }

        int remoteAddress = frameRcv.value(6) + (frameRcv.value(7) << 8);

        for (int i = 1; i < DNPAddressList.length; i++) {
            if (remoteAddress == DNPAddressList[i]) {
                currentRemoteStation = i;
                valid = true;

                break;
            }
        }

        return valid;
    }

    /**
     * DOCUMENT ME!
     */
    private void primaryHandler() throws Exception {
        // i'm secondary
        // i receive a message FROM PRIMARY
        // check if its a duplicate frame
        if (((control & 0x10) == 0x10) && (((control & 0x20) == 0x20) == receiveFcb[currentRemoteStation])) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[LinkLayer] duplicate frame");
            }
        }

        // handle this frame
        switch ((byte) (control & 0x0F)) {
        case RESET_LINK: {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[LinkLayer] i send a confirm LPDU : " + !error);
            }

            sendSecondaryMsg((error) ? NACK : ACK);
        }

            break;

        case REQUEST: {
            // if OK
            if (!error) {
                // cache this frame in case of a reply
                previousFrameRcv[currentRemoteStation].reset();
                previousFrameRcv[currentRemoteStation].writeBytes(frameRcv.value());

                // send this frame
                sendSecondaryMsg(RESPOND);
            }
        }

            break;

        case CON_DATA:
        case UNCON_DATA: {
            // handle
            frameRcv.incrOffset(10);

            Buffer trsFrame = new Buffer(S);
            int size = 0;

            while ((frameRcv.length() > 0) && (!error)) {
                size = Math.min(18, frameRcv.length());
                error |= !DnpCrc.checkCRC(frameRcv.value(0, size - 1));
                trsFrame.writeBytes(frameRcv.readBytes(size - 2));
                frameRcv.incrOffset(2);
            }

            // check if a confirmation is requiered
            if ((control & 0x0F) < 4) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[LinkLayer] i send a confirm LPDU : " + !error);
                }

                sendSecondaryMsg((error) ? NACK : ACK);
            }

            if (error) {
                System.out.println("[LinkLayer] error data CRC check!!!!!!!!!");
            }

            // if OK
            if (!error) {
                // cache this frame in case of a reply
                previousFrameRcv[currentRemoteStation].reset();
                previousFrameRcv[currentRemoteStation].writeBytes(frameRcv.value());

                // update context
                receiveFcb[currentRemoteStation] = !receiveFcb[currentRemoteStation];

                // Transport Layer handle
                transportLayer.handleTransportMsg(trsFrame, currentRemoteStation);
            }
        }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void secondaryHandler() throws Exception {
        // i'm primary
        // i receive a message FROM SECONDARY
        if (LOG.isDebugEnabled()) {
            LOG.debug("[LinkLayer] Receive confirm message");
        }

        if (error || ((control & 0x10) == 0x10)) // Data Flow Control bit
        {
            // Buffer of the device is full, or there's an error in this frame
            dfc = true;

            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                // ignore
            }

            // To verify state of remote device, or to avoid overflow, request
            // state of the device
            sendSecondaryMsg(REQUEST);
        }
        else {
            switch ((byte) (control & 0x0F)) {
            case NACK:

                // Device is not ready -> Retry
                retry();

                break;

            case RESPOND:

                // Response to the request message -> Device is OK.
                dfc = false;

            case ACK:
                // Notify LnkSnd to send the next frame
                lnkSnd.getConLnkSndLock().unlock();

            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void retry() throws Exception {
        try {
            Thread.currentThread().wait(100);
        }
        catch (InterruptedException e) {
            // ignore
        }

        lnkSnd.sendPreviousFrame();
    }

    /**
     * DOCUMENT ME!
     * 
     * @param FC
     *            DOCUMENT ME!
     */
    public void sendPrimaryMsg(byte FC) throws Exception {
        lnkSnd.send(true, FC);
    }

    /**
     * DOCUMENT ME!
     * 
     * @param FC
     *            DOCUMENT ME!
     */
    public void sendSecondaryMsg(byte FC) throws Exception {
        lnkSnd.send(false, FC);
    }

    /**
     * DOCUMENT ME!
     */
    public void handleConnectionError() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("[LinkLayer] run() : Remote Connection closed.");
        }

        // if (!STOP) {
        // phyLayer.reconnect();
        // } else {
        // System.out.println("[LinkLayer] run() : Close Remote Connection.");
        // phyLayer.close();
        // }
    }

    /**
     * @return the sTOP
     */
    public boolean isSTOP() {
        return STOP;
    }

    /**
     * @param sTOP
     *            the sTOP to set
     */
    public void setSTOP(boolean sTOP) {
        STOP = sTOP;
    }

    /**
     * @return the config
     */
    public DNPConfig getConfig() {
        return config;
    }

    /**
     * @param config
     *            the config to set
     */
    public void setConfig(DNPConfig config) {
        this.config = config;
    }

    /**
     * @return the dIR
     */
    public boolean isDIR() {
        return DIR;
    }

    /**
     * @param dIR
     *            the dIR to set
     */
    public void setDIR(boolean dIR) {
        DIR = dIR;
    }

    /**
     * @return the aDDRESS_1
     */
    public byte getADDRESS_1() {
        return ADDRESS_1;
    }

    /**
     * @param aDDRESS_1
     *            the aDDRESS_1 to set
     */
    public void setADDRESS_1(byte aDDRESS_1) {
        ADDRESS_1 = aDDRESS_1;
    }

    /**
     * @return the aDDRESS_0
     */
    public byte getADDRESS_0() {
        return ADDRESS_0;
    }

    /**
     * @param aDDRESS_0
     *            the aDDRESS_0 to set
     */
    public void setADDRESS_0(byte aDDRESS_0) {
        ADDRESS_0 = aDDRESS_0;
    }

    /**
     * @return the lnkConfirm
     */
    public boolean isLnkConfirm() {
        return lnkConfirm;
    }

    /**
     * @param lnkConfirm
     *            the lnkConfirm to set
     */
    public void setLnkConfirm(boolean lnkConfirm) {
        this.lnkConfirm = lnkConfirm;
    }

    /**
     * @return the lnkTimeout
     */
    public long getLnkTimeout() {
        return lnkTimeout;
    }

    //    /**
    //     * @param lnkTimeout
    //     *            the lnkTimeout to set
    //     */
    //    public void setLnkTimeout(long lnkTimeout) {
    //        this.lnkTimeout = lnkTimeout;
    //    }

    /**
     * @return the lnkMaxRetries
     */
    public int getLnkMaxRetries() {
        return lnkMaxRetries;
    }

    //    /**
    //     * @param lnkMaxRetries
    //     *            the lnkMaxRetries to set
    //     */
    //    public void setLnkMaxRetries(int lnkMaxRetries) {
    //        this.lnkMaxRetries = lnkMaxRetries;
    //    }

    /**
     * @return the control
     */
    public byte getControl() {
        return control;
    }

    /**
     * @param control
     *            the control to set
     */
    public void setControl(byte control) {
        this.control = control;
    }

    /**
     * @return the error
     */
    public boolean isError() {
        return error;
    }

    /**
     * @param error
     *            the error to set
     */
    public void setError(boolean error) {
        this.error = error;
    }

    /**
     * @return the bASIS
     */
    public byte[] getBASIS() {
        return BASIS;
    }

    /**
     * @param bASIS
     *            the bASIS to set
     */
    public void setBASIS(byte[] bASIS) {
        BASIS = bASIS;
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
     * @return the lnkSnd
     */
    public LnkSnd getLnkSnd() {
        return lnkSnd;
    }

    /**
     * @param lnkSnd
     *            the lnkSnd to set
     */
    public void setLnkSnd(LnkSnd lnkSnd) {
        this.lnkSnd = lnkSnd;
    }

    /**
     * @return the dfc
     */
    public boolean isDfc() {
        return dfc;
    }

    /**
     * @param dfc
     *            the dfc to set
     */
    public void setDfc(boolean dfc) {
        this.dfc = dfc;
    }

    /**
     * @return the ignoreNextBytes
     */
    public int getIgnoreNextBytes() {
        return ignoreNextBytes;
    }

    /**
     * @param ignoreNextBytes
     *            the ignoreNextBytes to set
     */
    public void setIgnoreNextBytes(int ignoreNextBytes) {
        this.ignoreNextBytes = ignoreNextBytes;
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
     * @return the dNPAddressList
     */
    public int[] getDNPAddressList() {
        return DNPAddressList;
    }

    /**
     * @param dNPAddressList
     *            the dNPAddressList to set
     */
    public void setDNPAddressList(int[] dNPAddressList) {
        DNPAddressList = dNPAddressList;
    }

    /**
     * @return the currentRemoteStation
     */
    public int getCurrentRemoteStation() {
        return currentRemoteStation;
    }

    /**
     * @param currentRemoteStation
     *            the currentRemoteStation to set
     */
    public void setCurrentRemoteStation(int currentRemoteStation) {
        this.currentRemoteStation = currentRemoteStation;
    }

    /**
     * @return the receiveFcb
     */
    public boolean[] getReceiveFcb() {
        return receiveFcb;
    }

    /**
     * @param receiveFcb
     *            the receiveFcb to set
     */
    public void setReceiveFcb(boolean[] receiveFcb) {
        this.receiveFcb = receiveFcb;
    }

    /**
     * @return the sendFcb
     */
    public boolean[] getSendFcb() {
        return sendFcb;
    }

    /**
     * @param sendFcb
     *            the sendFcb to set
     */
    public void setSendFcb(boolean[] sendFcb) {
        this.sendFcb = sendFcb;
    }

    /**
     * @return the previousFrameRcv
     */
    public Buffer[] getPreviousFrameRcv() {
        return previousFrameRcv;
    }

    /**
     * @param previousFrameRcv
     *            the previousFrameRcv to set
     */
    public void setPreviousFrameRcv(Buffer[] previousFrameRcv) {
        this.previousFrameRcv = previousFrameRcv;
    }

    public void setUser(DNPUser user) {
        this.user = user;
    }

    public DNPUser getUser() {
        return user;
    }

    private void waitForAvailable() throws ThreadStopException, IOException {
        while (!STOP && phyLayer.getInputStream().available() < 1) {
            try {
                Thread.sleep(DATA_PAUSE_TIME);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (STOP)
            throw new ThreadStopException();
    }
}
