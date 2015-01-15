package br.org.scadabr.dnp34j.master.layers.application;

import br.org.scadabr.dnp34j.master.common.AppFeatures;
import br.org.scadabr.dnp34j.master.common.DataMapFeatures;
import br.org.scadabr.dnp34j.master.common.DataObject;
import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.common.utils.Lock;
import br.org.scadabr.dnp34j.master.common.utils.Queue;
import br.org.scadabr.dnp34j.master.common.utils.Utils;
import br.org.scadabr.dnp34j.master.layers.DataMap;
import br.org.scadabr.dnp34j.master.layers.transport.TransportLayer;
import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;

/**
 * <p>
 * This is the receive part of the application layer This is a major part of DNP implementation :
 * <ul>
 * <li>from an external application,
 * <ul>
 * <li>it builds application requests and transmits them to the send part of application layer
 * </ul>
 * <li>from the underlayer,
 * <ul>
 * <li>it catches complete transport frames and build application frames
 * <li>a built application frame may be
 * <ul>
 * <li>a confirmation frame and transmit it to the send part of application layer
 * <li>a request frame and transmit it to the send part of application layer
 * <li>fragment(s) of a response frame and transmit their data objects to an API
 * </ul>
 * </ul>
 * </ul>
 * 
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC</a>
 */
public class AppRcv extends Thread implements AppFeatures, InitFeatures, DataMapFeatures {
    static final boolean DEBUG = !APP_QUIET;
    private boolean STOP = false;

    // =============================================================================
    // Attributes
    // =============================================================================
    private DNPConfig config;
    private Buffer appRcvBuffer;
    private Queue appRcvQueue;
    private Lock appRcvLock;
    private Buffer frameRcv;
    private boolean appConfirm; // if true, remote station sends confirmation
    // messages
    private long appTimeout;
    private int appMaxRetries;
    // private User user;
    private DNPUser user;
    private AppSnd appSnd;
    private TransportLayer transportLayer;
    private byte appLastSeq;
    private boolean appFirstFrame;
    private byte[] iin;
    private byte AC;
    private byte FC;
    private byte UNS;

    private DataMap dataMap;

    // =============================================================================
    // Constructor
    // =============================================================================

    /**
     * Constructor. Initialize the receive part of the application layer
     * 
     * @throws Exception
     */

    public AppRcv(DNPUser user) throws Exception {
        setUser(user);
        setConfig(user.getConfig());

        appRcvBuffer = new Buffer(M);
        appRcvQueue = new Queue();
        appRcvLock = new Lock();

        appFirstFrame = true;
        appLastSeq = 0;
        iin = new byte[2];
        frameRcv = new Buffer(M);

        transportLayer = user.getTranspLayer();
        dataMap = new DataMap(user);
        if (DEBUG) {
            System.out.println("[ApplicationLayer] initialized");
        }
    }

    // =============================================================================
    // Methods
    // =============================================================================
    public void run() {
        try {
            while (!STOP) {
                while (!STOP && appRcvQueue.empty())
                    appRcvLock.waiting(20);
                if (appRcvQueue.empty())
                    continue;

                appRcvLock.lock();

                if (DEBUG) {
                    System.out.println("[ApplicationLayer] frame from TransportLayer !");
                }
                handle(appRcvBuffer.readBytes(appRcvQueue.pop()));
            }
        }
        catch (Throwable t) {
            System.out.print("[MasterAppRcv] ");
            t.printStackTrace();
        }
    }

    // ///////////////////////////////////////////////////////////////////////

    /**
     * Handle a complete application frame from transport layer
     */
    private void handle(byte[] anAppFrame) throws Exception {
        frameRcv.reset();
        frameRcv.writeBytes(anAppFrame);
        AC = frameRcv.readByte();
        FC = frameRcv.readByte();
        iin[0] = frameRcv.readByte();
        iin[1] = frameRcv.readByte();
        frameRcv.decrOffset(4);

        // compliance
        if ((AC & 0x0F) != appLastSeq) {
            if (DEBUG) {
                System.out.println("[ApplicationLayer] ERROR : doesn't match with the message expected");
                System.out.println("[ApplicationLayer] ERROR : number expected : " + appLastSeq);
            }
        }

        if (FC == UNSOLICITED_RESPONSE)
            System.out.println("Unsolicited Message!");

        // handle a confirm or a response msg
        if (FC == CONFIRM) {
            handleConfirmMsg();
        }
        else {
            handleResponseMsg();
        }
    }

    /**
     * Handle a confirmation transmitted by the transport layer
     */
    private void handleConfirmMsg() throws Exception {
        // compliance
        if (!appSnd.getConAppSndLock().isLocked()) {
            if (DEBUG) {
                System.out.println("[ApplicationLayer] ERROR : i was not wating for a confirm message !");
            }
        }

        if ((AC & 0xc0) != 0xc0) {
            if (DEBUG) {
                System.out.println("[ApplicationLayer] ERROR : error found by application control");
            }
        }

        // unlock next request
        if (DEBUG) {
            System.out.println("[ApplicationLayer] received a confirm message. unlock send message");
        }

        appSnd.getConAppSndLock().unlock();
    }

    /**
     * Handle a complete application response transmitted by the transport layer
     */
    private void handleResponseMsg() throws Exception {
        if (appFirstFrame && ((AC & 0x80) != 0x80)) // i'm waiting for a first
        {
            if (DEBUG) {
                System.out.println("[ApplicationLayer] ERROR : it's not the first frame");
            }
        }

        // send a confirm msg
        if ((AC & 0x20) == 0x20) {
            buildConfirmMsg();
        }

        // update the context
        appFirstFrame = false;
        appLastSeq = (byte) ((appLastSeq + 1) % 16);

        int length = frameRcv.length();

        if (length > 4) {
            try {
                updateDatamap(new Buffer(M, frameRcv.value()));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        appSnd.getConAppSndLock().unlock();
        user.getUserRcvLock().unlock();

        if ((AC & 0x40) == 0x40) // case where this fragment is the last one
        {
            appFirstFrame = true; // next fragement must be a first one
            // user.getUserSndLock().unlock(); // next request can be send now
        }

    }

    // ///////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////

    /**
     * If received response is waiting for a confirmation, this method builds a
     * confim message
     */
    private void buildConfirmMsg() throws Exception {
        byte[] aFrame = new byte[2];
        aFrame[0] = (byte) (0xC0 + (AC & 0x1F));
        aFrame[1] = CONFIRM;
        push(new Buffer(XS, aFrame), true);
    }

    /**
     * When a response containing data is handled, This function is called to
     * update database
     */
    private void updateDatamap(Buffer aFrame) throws Exception {
        aFrame.incrOffset(4);

        while (aFrame.length() > 0) {
            boolean discard = false;
            byte group = aFrame.readByte();
            byte variation = aFrame.readByte();
            byte qualField = aFrame.readByte();
            int dataLength = DataObject.length(group, variation);

            if (dataLength < 0) {
                discard = true;
                System.out.println("Group: " + group + " Variation: " + variation + " dataLength: " + dataLength);
            }

            // nao suportada e nao descartavel!
            if (dataLength == -1) {
                throw new Exception("Invalid Application frame received!");
            }
            switch (qualField) {

            case START_STOP_8: {
                int start = Utils.byte2int(aFrame.readByte());
                int stop = Utils.byte2int(aFrame.readByte());
                int length = (((stop - start + 1) * Math.abs(dataLength)) + 7) / 8;

                if (!discard) {
                    dataMap.set(group, variation, start, stop, aFrame.readBytes(length));
                }
                else {
                    aFrame.readBytes(length);
                }

            }

                break;

            case START_STOP_16: {
                int start = (Utils.byte2int(aFrame.readByte()) + ((aFrame.readByte() << 8) & 0xFF00));
                int stop = (Utils.byte2int(aFrame.readByte()) + ((aFrame.readByte() << 8) & 0xFF00));
                int length = (((stop - start + 1) * Math.abs(dataLength)) + 7) / 8;
                if (!discard) {
                    dataMap.set(group, variation, start, stop, aFrame.readBytes(length));
                }
                else {
                    aFrame.readBytes(length);
                }

            }

                break;

            case ALL_POINTS: {
                // int length = ((dataMap.getIndexMax(group) * Math
                // .abs(dataLength)) + 7) / 8;
                // if (!discard) {
                // // dataMap.set(group, variation, aFrame.readBytes(length));
                // } else {
                // aFrame.readBytes(length);
                // }

            }

                break;

            case QUANTITY_8: {
                int quantity = Utils.byte2int(aFrame.readByte());
                int length = ((quantity * Math.abs(dataLength)) + 7) / 8;

                if (!discard) {
                    dataMap.set(group, variation, 0, quantity - 1, aFrame.readBytes(length));
                }
                else {
                    aFrame.readBytes(length);
                }
            }

                break;

            case QUANTITY_16: {
                int quantity = (Utils.byte2int(aFrame.readByte()) + ((aFrame.readByte() << 8) & 0xFF00));
                int length = ((quantity * Math.abs(dataLength)) + 7) / 8;

                if (!discard) {
                    dataMap.set(group, variation, 0, quantity - 1, aFrame.readBytes(length));
                }
                else {
                    aFrame.readBytes(length);
                }

            }

                break;

            case INDEXES_8: {
                int[] values = new int[aFrame.readByte()];
                DataObject[] dataObjects = new DataObject[values.length];

                if (!discard) {
                    for (int i = 0; i < values.length; i++) {
                        values[i] = Utils.byte2int(aFrame.readByte());

                        int length = (dataLength + 7) / 8;
                        dataObjects[i] = new DataObject(group, variation, aFrame.readBytes(length));
                    }

                    dataMap.set(group, variation, values, dataObjects, qualField);
                }
                else {
                    dataLength *= -1;
                    int numBytes = (dataLength / 8) * values.length + values.length;
                    aFrame.readBytes(numBytes);
                }
            }

                break;

            case INDEXES_16: {
                int[] values = new int[(Utils.byte2int(aFrame.readByte()) + ((aFrame.readByte() << 8) & 0xFF00))];
                DataObject[] dataObjects = new DataObject[values.length];

                if (!discard) {
                    for (int i = 0; i < values.length; i++) {
                        values[i] = (Utils.byte2int(aFrame.readByte()) + ((aFrame.readByte() << 8) & 0xFF00));

                        int length = (dataLength + 7) / 8;
                        dataObjects[i] = new DataObject(group, variation, aFrame.readBytes(length));
                    }

                    dataMap.set(group, variation, values, dataObjects, qualField);
                }
                else {
                    dataLength *= -1;
                    int numBytes = (dataLength / 8) * values.length + values.length * 2;
                    aFrame.readBytes(numBytes);
                }

            }
            }

        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////

    /**
     * When a new request is created by user layer the frame is queued, and sent
     * by AppSnd scheduler
     */
    public void push(Buffer aFrame, boolean type) throws Exception {
        int length = aFrame.length();

        if (type) {
            appSnd.getConAppSndBuffer().writeBytes(aFrame.readBytes());
            appSnd.getConAppSndQueue().push(length);
            appSnd.send(true);
        }
        else {
            // appSnd.getAppSndLock().setState(user.getUserRcvLock().isState());
            appSnd.getAppSndBuffer().writeBytes(aFrame.readBytes());
            appSnd.getAppSndQueue().push(length);
            appSnd.getAppSndLock().unlock();
        }
    }

    public boolean pushed(Buffer aFrame, boolean type) throws Exception {
        boolean hasBeenPushed = false;
        try {
            push(aFrame, type);
            hasBeenPushed = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return hasBeenPushed;
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
     * @return the appRcvBuffer
     */
    public Buffer getAppRcvBuffer() {
        return appRcvBuffer;
    }

    /**
     * @param appRcvBuffer
     *            the appRcvBuffer to set
     */
    public void setAppRcvBuffer(Buffer appRcvBuffer) {
        this.appRcvBuffer = appRcvBuffer;
    }

    /**
     * @return the appRcvQueue
     */
    public Queue getAppRcvQueue() {
        return appRcvQueue;
    }

    /**
     * @param appRcvQueue
     *            the appRcvQueue to set
     */
    public void setAppRcvQueue(Queue appRcvQueue) {
        this.appRcvQueue = appRcvQueue;
    }

    /**
     * @return the appRcvLock
     */
    public Lock getAppRcvLock() {
        return appRcvLock;
    }

    /**
     * @param appRcvLock
     *            the appRcvLock to set
     */
    public void setAppRcvLock(Lock appRcvLock) {
        this.appRcvLock = appRcvLock;
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
     * @return the appConfirm
     */
    public boolean isAppConfirm() {
        return appConfirm;
    }

    /**
     * @param appConfirm
     *            the appConfirm to set
     */
    public void setAppConfirm(boolean appConfirm) {
        this.appConfirm = appConfirm;
    }

    /**
     * @return the appTimeout
     */
    public long getAppTimeout() {
        return appTimeout;
    }

    /**
     * @param appTimeout
     *            the appTimeout to set
     */
    public void setAppTimeout(long appTimeout) {
        this.appTimeout = appTimeout;
    }

    /**
     * @return the appMaxRetries
     */
    public int getAppMaxRetries() {
        return appMaxRetries;
    }

    /**
     * @param appMaxRetries
     *            the appMaxRetries to set
     */
    public void setAppMaxRetries(int appMaxRetries) {
        this.appMaxRetries = appMaxRetries;
    }

    /**
     * @return the appSnd
     */
    public AppSnd getAppSnd() {
        return appSnd;
    }

    /**
     * @param appSnd
     *            the appSnd to set
     */
    public void setAppSnd(AppSnd appSnd) {
        this.appSnd = appSnd;
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
     * @return the appLastSeq
     */
    public byte getAppLastSeq() {
        return appLastSeq;
    }

    /**
     * @param appLastSeq
     *            the appLastSeq to set
     */
    public void setAppLastSeq(byte appLastSeq) {
        this.appLastSeq = appLastSeq;
    }

    /**
     * @return the appFirstFrame
     */
    public boolean isAppFirstFrame() {
        return appFirstFrame;
    }

    /**
     * @param appFirstFrame
     *            the appFirstFrame to set
     */
    public void setAppFirstFrame(boolean appFirstFrame) {
        this.appFirstFrame = appFirstFrame;
    }

    /**
     * @return the iin
     */
    public byte[] getIin() {
        return iin;
    }

    /**
     * @param iin
     *            the iin to set
     */
    public void setIin(byte[] iin) {
        this.iin = iin;
    }

    /**
     * @return the aC
     */
    public byte getAC() {
        return AC;
    }

    /**
     * @param aC
     *            the aC to set
     */
    public void setAC(byte aC) {
        AC = aC;
    }

    /**
     * @return the fC
     */
    public byte getFC() {
        return FC;
    }

    /**
     * @param fC
     *            the fC to set
     */
    public void setFC(byte fC) {
        FC = fC;
    }

    /**
     * @return the uNS
     */
    public byte getUNS() {
        return UNS;
    }

    /**
     * @param uNS
     *            the uNS to set
     */
    public void setUNS(byte uNS) {
        UNS = uNS;
    }

    public void setUser(DNPUser user) {
        this.user = user;
    }

    public DNPUser getUser() {
        return user;
    }

    public void setConfig(DNPConfig config) {
        this.config = config;
    }

    public DNPConfig getConfig() {
        return config;
    }
}