/*
 * User class
 *
 * Class    com.itlity.protocol.master.layers.User
 * File     User.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.layers;

import br.org.scadabr.dnp34j.master.common.AppFeatures;
import br.org.scadabr.dnp34j.master.common.DataMapFeatures;
import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.common.utils.Lock;
import br.org.scadabr.dnp34j.master.common.utils.Queue;
import br.org.scadabr.dnp34j.master.common.utils.Utils;
import br.org.scadabr.dnp34j.master.layers.application.AppRcv;
import br.org.scadabr.dnp34j.master.layers.application.AppSnd;
import br.org.scadabr.dnp34j.master.layers.link.LnkSnd;

/**
 * <p>
 * This class is an interface between DNP protocol and DNP external application It ties up the application layer of the
 * protocol with a database and a control/command application
 * 
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class User extends Thread implements InitFeatures, DataMapFeatures, AppFeatures {
    private static final boolean DEBUG = !APP_QUIET;
    private boolean STOP = false;
    /**
     * Current Configuration
     */
    /**
     * Current DataMap
     */
    // private DataMap dataMap;

    /**
     * Application Layer Parsing Process
     */
    private AppRcv appRcv;

    /**
     * Application Layer Sending Process
     */
    private AppSnd appSnd;

    /**
     * Link Layer Sending Process
     */
    private LnkSnd lnkSnd;

    /**
     * Frames data transmitted by the lower layer
     */
    private Buffer userRcvBuffer;

    /**
     * Queue of frames transmitted by the lower layer
     */
    private Queue userRcvQueue;

    /**
     * Lock. Unlocked when data from the lower layer is ready to be parsed
     */
    private Lock userRcvLock;

    /**
     * Lock. Unlocking it allows next user frame to be sent
     */
    private Lock userSndLock;
    private Lock databaseLock;
    /**
     * <p>
     * Initialize status Become <tt>true</tt> when init() has be done
     */
    private boolean initialized = false;

    // =============================================================================
    // Constructor
    // =============================================================================

    /**
     * Empty constructor for User class Use init() to initialize an instance of
     * this class
     */
    public User() {
    }

    public void resetLink(int retries) throws Exception {
        boolean linkStatus = false;

        do {
            System.out.println("[User] - Reset Link " + retries);
            linkStatus = lnkSnd.getLnkRcv().initLink(3000);

            if (!linkStatus)
                retries--;

        }
        while (!linkStatus && retries > 0);

        if (!linkStatus)
            throw new Exception("Reset Link Failed!");
    }

    // =============================================================================
    // Methods
    // =============================================================================

    /**
     * Initialize user parameters with Configuration parameters
     * 
     * @param DNPConfig
     *            Configuration
     * @throws Exception
     */
    public void init() throws Exception {
        // setConfig(conf);

        if (initialized) {
            return;
        }
        initialized = true;

        // setDataMap(new DataMap(config));

        userSndLock = new Lock(UNLOCKED);
        userRcvLock = new Lock();
        userRcvBuffer = new Buffer(M);
        userRcvQueue = new Queue();

        // appRcv = new AppRcv();

        setAppSnd(appRcv.getAppSnd());

        setLnkSnd(appRcv.getTransportLayer().getLnkRcv().getLnkSnd());

        appRcv.start();

        if (DEBUG) {
            System.out.println("[UserLayer] initialized");
        }
    }

    /**
     * Stop all protocol processes
     * 
     * @throws Exception
     */
    public void stopUser() throws Exception {
        try {
            // elements are set with invalid flag
            // dataMap.setElementsToInvalid();
            // polling threads are stopped
            setSTOP(true);
            // stop app layer
            appRcv.setSTOP(true);
            // stop link layer
            appRcv.getTransportLayer().getLnkRcv().setSTOP(true);
            // close physical connection
            appRcv.getTransportLayer().getLnkRcv().getPhyLayer().close();
        }
        catch (Exception e) {
            System.out.println("[User] - stopUser() failed");
            System.out.println("[User] - Exception throwed");
            throw new Exception(e);
        }
    }

    public Buffer buildAnalogControlCommand(byte operateMode, int index, int value) throws Exception {
        Buffer commandFrame = appSnd.buildRequestMsg(operateMode, ANALOG_OUTPUT_COMMAND, (byte) 2, new int[] { index },
                WITH_DATA);

        int previous_marker = commandFrame.length();
        byte[] valueOnBytes = toBytes(value, 2);

        commandFrame.setMarker(7);
        commandFrame.writeByte(valueOnBytes[0]);
        commandFrame.setMarker(8);
        commandFrame.writeByte(valueOnBytes[1]);

        commandFrame.setMarker(previous_marker);

        return commandFrame;
    }

    public Buffer buildBinaryControlCommand(byte operateMode, int index, byte controlCode, int timeOn, int timeOff)
            throws Exception {
        Buffer commandFrame = appSnd.buildRequestMsg(operateMode, BINARY_OUTPUT_COMMAND, (byte) 1, new int[] { index },
                WITH_DATA);

        int previous_marker = commandFrame.length();

        commandFrame.setMarker(7);
        commandFrame.writeByte(controlCode);

        commandFrame.setMarker(previous_marker);

        byte[] timeOnBytes = toBytes(timeOn, 4);
        commandFrame.setMarker(9);
        commandFrame.writeByte(timeOnBytes[0]);
        commandFrame.setMarker(10);
        commandFrame.writeByte(timeOnBytes[1]);
        commandFrame.setMarker(11);
        commandFrame.writeByte(timeOnBytes[2]);
        commandFrame.setMarker(12);
        commandFrame.writeByte(timeOnBytes[3]);

        byte[] timeOffBytes = toBytes(timeOff, 4);

        commandFrame.setMarker(13);
        commandFrame.writeByte(timeOffBytes[0]);
        commandFrame.setMarker(14);
        commandFrame.writeByte(timeOffBytes[1]);
        commandFrame.setMarker(15);
        commandFrame.writeByte(timeOffBytes[2]);
        commandFrame.setMarker(16);
        commandFrame.writeByte(timeOffBytes[3]);

        commandFrame.setMarker(previous_marker);
        return commandFrame;
    }

    private byte[] toBytes(int value, int size) {
        byte[] result = new byte[size];

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
        return result;
    }

    public Buffer buildReadStaticDataMsg() throws Exception {
        Buffer request = new Buffer(S);

        // if (config.getDNPAddressList().length > 2) {
        // lnkSnd.setAddressToReportTo(0); // BROADCAST
        // }

        request = appSnd.buildRequestMsg(READ, CLASS_STATIC, CLASS_0_VAR);

        return request;
    }

    public Buffer buildReadEventDataMsg() throws Exception {
        Buffer request = new Buffer(S);

        request = appSnd.addObjectToRequest(request, READ, CLASS_STATIC, CLASS_1_VAR);
        request = appSnd.addObjectToRequest(request, READ, CLASS_STATIC, CLASS_2_VAR);
        request = appSnd.addObjectToRequest(request, READ, CLASS_STATIC, CLASS_3_VAR);

        return request;
    }

    // ////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////

    /**
     * Build a time request
     * 
     * @return a user request frame
     */
    public Buffer buildSetTimeAndDateMsg() throws Exception {
        return appSnd.buildRequestMsg(WRITE, TIME_COMMAND, (byte) 1, (byte) 1, WITH_DATA);
    }

    /**
     * This function is called to send a user frame to the protocol sending
     * process
     * 
     * @param a
     *            user request frame
     * @throws Exception
     */
    public synchronized void send(Buffer aFrame) throws Exception {
        if (DEBUG) {
            System.out.println("[UserLayer] push APDU " + Utils.Display(aFrame.value()));
        }

        userRcvLock.lock();

        appRcv.push(aFrame, false);
    }

    public boolean sendBuffer(Buffer aFrame) {
        boolean sent = false;
        try {
            // userRcvLock.lock();
            // sent = appRcv.pushed(aFrame, false);
            send(aFrame);
            userRcvLock.unlock();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return sent;
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

    /**
     * @return the appRcv
     */
    public AppRcv getAppRcv() {
        return appRcv;
    }

    /**
     * @param appRcv
     *            the appRcv to set
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
     * @param appSnd
     *            the appSnd to set
     */
    public void setAppSnd(AppSnd appSnd) {
        this.appSnd = appSnd;
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
     * @return the userRcvBuffer
     */
    public Buffer getUserRcvBuffer() {
        return userRcvBuffer;
    }

    /**
     * @param userRcvBuffer
     *            the userRcvBuffer to set
     */
    public void setUserRcvBuffer(Buffer userRcvBuffer) {
        this.userRcvBuffer = userRcvBuffer;
    }

    /**
     * @return the userRcvQueue
     */
    public Queue getUserRcvQueue() {
        return userRcvQueue;
    }

    /**
     * @param userRcvQueue
     *            the userRcvQueue to set
     */
    public void setUserRcvQueue(Queue userRcvQueue) {
        this.userRcvQueue = userRcvQueue;
    }

    /**
     * @return the userRcvLock
     */
    public Lock getUserRcvLock() {
        return userRcvLock;
    }

    /**
     * @param userRcvLock
     *            the userRcvLock to set
     */
    public void setUserRcvLock(Lock userRcvLock) {
        this.userRcvLock = userRcvLock;
    }

    /**
     * @return the userSndLock
     */
    public Lock getUserSndLock() {
        return userSndLock;
    }

    /**
     * @param userSndLock
     *            the userSndLock to set
     */
    public void setUserSndLock(Lock userSndLock) {
        this.userSndLock = userSndLock;
    }

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @param initialized
     *            the initialized to set
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setDatabaseLock(Lock databaseLock) {
        this.databaseLock = databaseLock;
    }

    public Lock getDatabaseLock() {
        return databaseLock;
    }

}
