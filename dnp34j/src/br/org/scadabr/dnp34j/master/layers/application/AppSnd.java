package br.org.scadabr.dnp34j.master.layers.application;

import br.org.scadabr.dnp34j.master.common.AppFeatures;
import br.org.scadabr.dnp34j.master.common.DataMapFeatures;
import br.org.scadabr.dnp34j.master.common.DataObject;
import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.common.utils.Lock;
import br.org.scadabr.dnp34j.master.common.utils.Queue;
import br.org.scadabr.dnp34j.master.common.utils.Utils;
import br.org.scadabr.dnp34j.master.layers.transport.TransportLayer;
import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;

/**
 *
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class AppSnd extends Thread implements AppFeatures, InitFeatures,
DataMapFeatures {
    static final boolean DEBUG = !APP_QUIET;

    // =============================================================================
    // Attributes
    // =============================================================================
    private Buffer appSndBuffer; // request or response buffer
    private Queue appSndQueue; // request or response queue
    private Buffer conAppSndBuffer; // confirm buffer
    private Queue conAppSndQueue; // confirm queue
    private Lock appSndLock; // lock when current send process is running
    private Lock conAppSndLock; // lock until a confirm message is received
    private Buffer frameRcv;
    private Buffer previousFrameSnd;
    private AppRcv appRcv;
    private TransportLayer transportLayer;
    private DNPConfig config;

    // =============================================================================
    // Constructor
    // =============================================================================
    public AppSnd(DNPUser user) throws Exception {
        appSndBuffer = new Buffer(M);
        appSndQueue = new Queue();
        conAppSndBuffer = new Buffer(S);
        conAppSndQueue = new Queue();
        appSndLock = new Lock();
        conAppSndLock = new Lock(UNLOCKED);
        frameRcv = new Buffer(M);
        previousFrameSnd = new Buffer(M);

        this.config = user.getConfig();
    }

    // =============================================================================
    // Methods
    // =============================================================================
    @Override
    public void run() {
        try {
            while (!appRcv.isSTOP()) {
                scheduler(); // scheduler for requests
                send(false); // OK, go !
            }
        } catch (Throwable t) {
            System.out.print("[MasterAppSnd] ");
            t.printStackTrace();
            System.out.println(t);
        }
    }

    /**
     * Temporize between each request/response
     */
    public void scheduler() throws Exception {
        // schedule requests & responses
        // if a request/response exist, no need to wait
        while (!appRcv.isSTOP() && appSndQueue.empty())
            // wait until a request/response is ready to send
            // appSndLock.waiting(20);
            Thread.sleep(20);
        if (!appRcv.isSTOP())
            return;

        // locked when current process is running
        // System.out.println("appSndLock.lock");
        appSndLock.lock();

        // a confirmation may be expected
        int remainingRetries = appRcv.getAppMaxRetries();
        while (!appRcv.isSTOP()) {
            if (conAppSndLock.waiting(appRcv.getAppTimeout())) {
                // System.out.println("appSndLock OK");
                /*
                 * enters here if - no confirmation is expected - or a
                 * confirmation is expected and - is already received - timeout
                 * has not expired
                 *
                 * -> confirmation received
                 */
                break;
            }

            /*
             * enters here if - confirmation is expected and - timeout has
             * expired
             *
             * -> confirmation not received
             */
            if (remainingRetries > 0) {
                sendPreviousFrame();
                remainingRetries--;
            } else {
                // notifies this error to database
                // dataMap.setElementsToInvalid();

                break;
            }
        }

        while (!conAppSndQueue.empty()) // priority is to send confirm messages
        {
            try {
                Thread.currentThread().wait(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Build a request header This one may be used to build a Class 0 request
     */
    private byte[] buildHeader(byte FC, byte group, byte variation) {
        byte[] header = new byte[4];
        header[0] = (byte) (0xc0 + ((appRcv.getConfig().isREQ_APP_CONFIRM()) ? 0x20
                : 0)); // AC

        header[0] = (byte) (header[0] + appRcv.getAppLastSeq());
        header[1] = FC;
        header[2] = group;
        header[3] = variation;

        return header;
    }

    /**
     * Build a request from a user command This one is mostly used with a READ
     * function
     */
    public Buffer buildRequestMsg(byte FC, byte group, byte variation) {
        Buffer requestFrame = new Buffer(S);
        requestFrame.writeBytes(buildHeader(FC, group, variation));
        requestFrame.writeByte(ALL_POINTS);

        // if (withData) {
        // requestFrame.writeBytes(dataMap.get(group, variation, dataMap
        // .getIndexMin(group), dataMap.getIndexMax(group)));
        // }

        return requestFrame;
    }

    /**
     * Build a request from a user command This one is mostly used with a READ
     * function This is also used with IIN Object to clear some indications of a
     * slave device (WRITE function) with time or IIN requests, there's no
     * corresponding data in database
     */
    public Buffer buildRequestMsg(byte FC, byte group, byte variation,
            int start, int stop) throws Exception {
        Buffer requestFrame = new Buffer(S);
        requestFrame.writeBytes(buildHeader(FC, group, variation));

        if (stop < 256) {
            requestFrame.writeByte(START_STOP_8);
            requestFrame.writeByte((byte) start);
            requestFrame.writeByte((byte) stop);
        } else {
            requestFrame.writeByte(START_STOP_16);
            requestFrame.writeBytes(start);
            requestFrame.writeBytes(stop);
        }

        // if (withData) {
        // if (DataObject.getObjectType(group) == IIN) {
        // requestFrame.writeBytes(getIIN(start, stop));
        // } else {
        // requestFrame.writeBytes(dataMap.get(group, variation, start,
        // stop));
        // }
        // }

        return requestFrame;
    }

    /**
     * Build a request from a user command This one is mostly used with a READ
     * function This is also used with Time Object to set time of a slave device
     * (WRITE function) with time or IIN requests, there's no corresponding data
     * in database
     */
    public Buffer buildRequestMsg(byte FC, byte group, byte variation,
            int quantity, boolean withData) {
        Buffer requestFrame = new Buffer(S);
        requestFrame.writeBytes(buildHeader(FC, group, variation));

        if (quantity < 256) {
            requestFrame.writeByte(QUANTITY_8);
            requestFrame.writeByte((byte) quantity);
        } else {
            requestFrame.writeByte(QUANTITY_16);
            requestFrame.writeBytes(quantity);
        }

        if (withData) {
            if (group == 50) {
                requestFrame.writeBytes(DataObject.getTime(System
                        .currentTimeMillis()));
            }
            // else {
            // requestFrame.writeBytes(dataMap.get(group, variation, 0,
            // quantity - 1));
            // }
        }

        return requestFrame;
    }

    /**
     * Build a request from a user command This one is mostly used with a
     * CONTROL function
     */
    public Buffer buildRequestMsg(byte FC, byte group, byte variation,
            int[] values, boolean withData) {
        Buffer requestFrame = new Buffer(S);
        requestFrame.writeBytes(buildHeader(FC, group, variation));

        if (values[values.length - 1] < 256) {
            requestFrame.writeByte(INDEXES_8);
            requestFrame.writeByte((byte) values.length);
            if (withData) {
                byte[] byteValues = new byte[values.length];
                for(int i=0; i<values.length; i++) {
                    byteValues[i] = (byte)values[i];
                }
                requestFrame.writeBytes(byteValues);
            }
        } else {
            if (withData) {
                requestFrame.writeByte(INDEXES_16);
                requestFrame.writeBytes(values.length);
                requestFrame.writeBytes(values);
            }
        }

        return requestFrame;
    }

    /**
     * add an object to a request it is used with polling to build a request
     * with multiple objects
     */
    public Buffer addObjectToRequest(Buffer requestFrame, byte FC, byte group,
            byte variation, int start, int stop)
                    throws Exception {
        if (requestFrame.length() == 0) {
            requestFrame.writeBytes(buildHeader(FC, group, variation));
        } else {
            requestFrame.writeByte(group);
            requestFrame.writeByte(variation);
        }

        if (stop < 256) {
            requestFrame.writeByte(START_STOP_8);
            requestFrame.writeByte((byte) start);
            requestFrame.writeByte((byte) stop);
        } else {
            requestFrame.writeByte(START_STOP_16);
            requestFrame.writeBytes(start);
            requestFrame.writeBytes(stop);
        }
        return requestFrame;
    }

    /**
     * add an object to a request ( it is used with polling to build a request
     * with multiple objects
     */
    public Buffer addObjectToRequest(Buffer requestFrame, byte FC, byte group,
            byte variation) {
        if (requestFrame.length() == 0) {
            requestFrame.writeBytes(buildHeader(FC, group, variation));
        } else {
            requestFrame.writeByte(group);
            requestFrame.writeByte(variation);
        }

        requestFrame.writeByte(ALL_POINTS);

        return requestFrame;
    }

    // ///////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////

    /**
     * When a response is parsed by AppRcvMaster, a confirmation message and/or
     * a request may be generated. When a message is generated in this way, it's
     * pushed in a Buffer - conAppSndBuffer for a confirm message, -
     * appSndBuffer for a request This method is called - immediatly after a
     * confirm message is pushed - later when AppSnd scheduler allows next
     * request to be sent.
     *
     * This method pops this message from its buffer and launch
     * buildApplicationMsg()
     */
    public void send(boolean type) throws Exception {
        if (type) // confirm message
        {
            buildApplicationMsg(conAppSndBuffer.readBytes(conAppSndQueue.pop()));
        } else {
            buildApplicationMsg(appSndBuffer.readBytes(appSndQueue.pop()));
        }
    }

    /**
     * Update context just before sending the request then, the message is
     * transmitted to transportLayer.
     */
    private void buildApplicationMsg(byte[] someBytes) throws Exception {
        frameRcv.reset();
        frameRcv.writeBytes(someBytes);

        byte AC = frameRcv.value(0);

        // a confirm message may be expected, and no message will be sent until
        // it comes
        if ((AC & 0x20) == 0x20) {
            conAppSndLock.lock();
        }

        // update application sequence number
        appRcv.setAppLastSeq((byte) (AC & 0x0F));
        // store current frame for possible retries
        previousFrameSnd.reset();
        previousFrameSnd.writeBytes(frameRcv.value());

        // transmit this application frame to transport layer
        transportLayer.buildTransportMsg(frameRcv);

        if (DEBUG) {
            System.out.println("[ApplicationLayer] Sending frame"
                    + Utils.Display(frameRcv.value()));
        }

    }

    // ///////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////
    // For the purpose of doing retries, send previous frame
    private void sendPreviousFrame() throws Exception {
        transportLayer.buildTransportMsg(previousFrameSnd);
    }

    // /**
    // * Add IIN objects to a write request using object 80 such requests are
    // * build to clear indications of a slave device so all bits are set to 0
    // */
    // private byte[] getIIN(int start, int stop) {
    // return new byte[(((stop - start) > 7) ? 2 : 1)];
    // }
    //
    /**
     * Add a time object to a write request using object 50 such requests are
     * build to set time of a slave device so this object is filled with
     * currentTime
     */

    /**
     * @return the appSndBuffer
     */
    public Buffer getAppSndBuffer() {
        return appSndBuffer;
    }

    /**
     * @param appSndBuffer
     *            the appSndBuffer to set
     */
    public void setAppSndBuffer(Buffer appSndBuffer) {
        this.appSndBuffer = appSndBuffer;
    }

    /**
     * @return the appSndQueue
     */
    public Queue getAppSndQueue() {
        return appSndQueue;
    }

    /**
     * @param appSndQueue
     *            the appSndQueue to set
     */
    public void setAppSndQueue(Queue appSndQueue) {
        this.appSndQueue = appSndQueue;
    }

    /**
     * @return the conAppSndBuffer
     */
    public Buffer getConAppSndBuffer() {
        return conAppSndBuffer;
    }

    /**
     * @param conAppSndBuffer
     *            the conAppSndBuffer to set
     */
    public void setConAppSndBuffer(Buffer conAppSndBuffer) {
        this.conAppSndBuffer = conAppSndBuffer;
    }

    /**
     * @return the conAppSndQueue
     */
    public Queue getConAppSndQueue() {
        return conAppSndQueue;
    }

    /**
     * @param conAppSndQueue
     *            the conAppSndQueue to set
     */
    public void setConAppSndQueue(Queue conAppSndQueue) {
        this.conAppSndQueue = conAppSndQueue;
    }

    /**
     * @return the appSndLock
     */
    public Lock getAppSndLock() {
        return appSndLock;
    }

    /**
     * @return the conAppSndLock
     */
    public Lock getConAppSndLock() {
        return conAppSndLock;
    }

    /**
     * @param conAppSndLock
     *            the conAppSndLock to set
     */
    public void setConAppSndLock(Lock conAppSndLock) {
        this.conAppSndLock = conAppSndLock;
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
}