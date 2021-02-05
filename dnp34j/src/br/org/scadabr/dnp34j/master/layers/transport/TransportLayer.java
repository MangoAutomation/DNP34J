package br.org.scadabr.dnp34j.master.layers.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.org.scadabr.dnp34j.master.common.AppFeatures;
import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.layers.DataMap;
import br.org.scadabr.dnp34j.master.layers.application.AppRcv;
import br.org.scadabr.dnp34j.master.layers.application.AppSnd;
import br.org.scadabr.dnp34j.master.layers.link.LnkRcv;
import br.org.scadabr.dnp34j.master.layers.link.LnkSnd;
import br.org.scadabr.dnp34j.master.session.DNPUser;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.1.1.1 $
 */
public class TransportLayer implements AppFeatures, InitFeatures {

    // =============================================================================
    // Attributes
    // =============================================================================
    // variables used to check the header of transport frames
    private Buffer[] trsFrame; // used to collect link frames from any remote
    // device
    private boolean[] trsFirstFrame; // next frame must be a first transport
    // frame
    private byte[] trsLastSeq; // sequence number of previous frame

    // intra-connection between layers
    private AppRcv appRcv;
    private AppSnd appSnd;
    private LnkRcv lnkRcv;
    private LnkSnd lnkSnd;

    private DNPUser user;

    // =============================================================================
    // Constructor
    // =============================================================================
    public TransportLayer(DNPUser user) throws Exception {
        this.setUser(user);
        setAppRcv(user.getAppRcv());
        setAppSnd(user.getAppSnd());
        // setLnkRcv(user.getLnkRcv());
        // setLnkSnd(user.getLnkSnd());
        // lnkRcv.start();

        trsFrame = new Buffer[user.getConfig().getDNPAddressList().length];

        for (int i = 0; i < user.getConfig().getDNPAddressList().length; i++) {
            trsFrame[i] = new Buffer(M);
        }

        trsFirstFrame = new boolean[user.getConfig().getDNPAddressList().length];

        for (int i = 0; i < user.getConfig().getDNPAddressList().length; i++) {
            trsFirstFrame[i] = true;
        }

        trsLastSeq = new byte[user.getConfig().getDNPAddressList().length];
        // lnkRcv.init();
    }

    // =============================================================================
    // Methods
    // =============================================================================
    // build some transport frames from an application frame
    public void buildTransportMsg(Buffer anAppFrame) throws Exception {
        byte sequence = (byte) 0x00;
        byte header;

        while (anAppFrame.length() != 0) {
            int length = Math.min(TRANSPORT_FRAME_SIZE_MAX - 1, anAppFrame.length());
            header = (byte) (trsLastSeq[lnkSnd.getAddressToReportTo()] + ((sequence == 0) ? 0x40 : 0x00)
                    + ((anAppFrame.length() == length) ? 0x80 : 0x00));
            pushLower(header, anAppFrame, length);
            sequence += 1;
            trsLastSeq[lnkSnd.getAddressToReportTo()] = (byte)(trsLastSeq[lnkSnd.getAddressToReportTo()] + 1);
            if(trsLastSeq[lnkSnd.getAddressToReportTo()] == 64) {
                trsLastSeq[lnkSnd.getAddressToReportTo()] = 0;
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////
    // handle a transport frame and redirect it to application layer
    public void handleTransportMsg(Buffer trsFrame, int currentRemoteStation) throws Exception {
        byte TH = trsFrame.readByte();
        pushUpper(TH, trsFrame, currentRemoteStation);
    }

    // ///////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////
    private void pushUpper(byte TH, Buffer aFrame, int currentRemoteStation) throws Exception {
        trsFrame[currentRemoteStation].writeBytes(aFrame.readBytes());
        if ((TH & 0x80) == 0x80) {
            trsFirstFrame[currentRemoteStation] = true;
            appRcv.getAppRcvBuffer().writeBytes(trsFrame[currentRemoteStation].readBytes());
            appRcv.getAppRcvQueue().push(appRcv.getAppRcvBuffer().length());
            appRcv.getAppRcvLock().unlock();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param header DOCUMENT ME!
     * @param anAppFrame DOCUMENT ME!
     * @param length DOCUMENT ME!
     */
    private void pushLower(byte header, Buffer anAppFrame, int length) throws Exception {
        lnkSnd.getLnkSndBuffer().writeByte(header);
        lnkSnd.getLnkSndBuffer().writeBytes(anAppFrame.readBytes(length));
        lnkSnd.getLnkSndQueue().push(length + 1);
        lnkSnd.getLnkSndLock().unlock();
    }

    /**
     * @return the trsFrame
     */
    public Buffer[] getTrsFrame() {
        return trsFrame;
    }

    /**
     * @param trsFrame the trsFrame to set
     */
    public void setTrsFrame(Buffer[] trsFrame) {
        this.trsFrame = trsFrame;
    }

    /**
     * @return the trsFirstFrame
     */
    public boolean[] getTrsFirstFrame() {
        return trsFirstFrame;
    }

    /**
     * @param trsFirstFrame the trsFirstFrame to set
     */
    public void setTrsFirstFrame(boolean[] trsFirstFrame) {
        this.trsFirstFrame = trsFirstFrame;
    }

    /**
     * @return the trsLastSeq
     */
    public byte[] getTrsLastSeq() {
        return trsLastSeq;
    }

    /**
     * @param trsLastSeq the trsLastSeq to set
     */
    public void setTrsLastSeq(byte[] trsLastSeq) {
        this.trsLastSeq = trsLastSeq;
    }

    /**
     * @return the appRcv
     */
    public AppRcv getAppRcv() {
        return appRcv;
    }

    /**
     * @param appRcv the appRcv to set
     */
    public void setAppRcv(AppRcv appRcv) {
        this.appRcv = appRcv;
    }

    /**
     * @return the appSnd
     */
    public AppSnd getAppSnd() {
        return appSnd;
    }

    /**
     * @param appSnd the appSnd to set
     */
    public void setAppSnd(AppSnd appSnd) {
        this.appSnd = appSnd;
    }

    /**
     * @return the lnkRcv
     */
    public LnkRcv getLnkRcv() {
        return lnkRcv;
    }

    /**
     * @param lnkRcv the lnkRcv to set
     */
    public void setLnkRcv(LnkRcv lnkRcv) {
        this.lnkRcv = lnkRcv;
    }

    /**
     * @return the lnkSnd
     */
    public LnkSnd getLnkSnd() {
        return lnkSnd;
    }

    /**
     * @param lnkSnd the lnkSnd to set
     */
    public void setLnkSnd(LnkSnd lnkSnd) {
        this.lnkSnd = lnkSnd;
    }

    public void setUser(DNPUser user) {
        this.user = user;
    }

    public DNPUser getUser() {
        return user;
    }
}
