package br.org.scadabr.dnp34j.master.session;

import br.org.scadabr.dnp34j.master.common.AppFeatures;
import br.org.scadabr.dnp34j.master.common.DataMapFeatures;
import br.org.scadabr.dnp34j.master.common.DataObject;
import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.common.utils.Lock;
import br.org.scadabr.dnp34j.master.layers.application.AppRcv;
import br.org.scadabr.dnp34j.master.layers.application.AppSnd;
import br.org.scadabr.dnp34j.master.layers.link.LnkRcv;
import br.org.scadabr.dnp34j.master.layers.link.LnkSnd;
import br.org.scadabr.dnp34j.master.layers.physical.PhyLayer;
import br.org.scadabr.dnp34j.master.layers.transport.TransportLayer;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;
import br.org.scadabr.dnp34j.master.session.database.Database;

public class DNPUser implements InitFeatures, DataMapFeatures, AppFeatures {

    private Lock userRcvLock;

    private AppRcv appRcv;
    private AppSnd appSnd;
    private TransportLayer transpLayer;
    private LnkRcv lnkRcv;
    private LnkSnd lnkSnd;
    private PhyLayer phyLayer;

    private Database database;
    private DNPConfig config;

    public DNPUser(DNPConfig config) {
        this.config = config;
    }

    public void init() throws Exception {
        phyLayer = new PhyLayer(this);

        database = new Database();
        appRcv = new AppRcv(this);
        appSnd = new AppSnd(this);
        transpLayer = new TransportLayer(this);
        lnkRcv = new LnkRcv(this);
        lnkSnd = new LnkSnd();

        appRcv.setAppSnd(appSnd);
        appRcv.setTransportLayer(transpLayer);
        appSnd.setAppRcv(appRcv);
        appSnd.setTransportLayer(transpLayer);

        lnkRcv.setLnkSnd(lnkSnd);
        lnkRcv.setPhyLayer(phyLayer);
        lnkSnd.setPhyLayer(phyLayer);
        lnkSnd.setLnkRcv(lnkRcv);
        lnkSnd.setTransportLayer(transpLayer);

        transpLayer.setAppRcv(appRcv);
        transpLayer.setAppSnd(appSnd);
        transpLayer.setLnkRcv(lnkRcv);
        transpLayer.setLnkSnd(lnkSnd);

        appRcv.start();
        appSnd.start();
        lnkRcv.start();
        lnkSnd.start();

        userRcvLock = new Lock();
        Thread.sleep(1000);
        boolean ok = resetLink(config.getRequestTimeout());
        if (!ok)
            throw new Exception("Reset Link Failed!");
    }

    public synchronized void sendSynch(Buffer aFrame) throws Exception {
        appRcv.push(aFrame, false);
        userRcvLock.lock();
        if (!userRcvLock.waiting(config.getRequestTimeout()))
            throw new Exception("REQUEST TIMEOUT EXCEPTION");
        //Thread.sleep(100);
    }

    public synchronized void send(Buffer aFrame) throws Exception {
        appRcv.push(aFrame, false);
    }

    public Buffer buildReadStaticDataMsg() {
        Buffer request = new Buffer(S);

        request = appSnd.buildRequestMsg(READ, CLASS_STATIC, CLASS_0_VAR);

        return request;
    }

    public Buffer buildReadDataMsg(byte group, byte classVar) {
        Buffer request = new Buffer(S);
        request = appSnd.addObjectToRequest(request, READ, group, classVar);
        return request;
    }

    public Buffer buildReadAllEventDataMsg() {
        Buffer request = new Buffer(S);

        request = appSnd.addObjectToRequest(request, READ, CLASS_STATIC, CLASS_1_VAR);
        request = appSnd.addObjectToRequest(request, READ, CLASS_STATIC, CLASS_2_VAR);
        request = appSnd.addObjectToRequest(request, READ, CLASS_STATIC, CLASS_3_VAR);

        return request;
    }

    public Buffer buildAnalogControlCommand(byte operateMode, int index, int value) {
        Buffer commandFrame = appSnd.buildRequestMsg(operateMode, ANALOG_OUTPUT_COMMAND, (byte) 2,
                new int[] {index}, WITH_DATA);

        byte[] valueOnBytes = DataObject.toBytes(value, 2);
        commandFrame.setMarker(7);
        commandFrame.writeByte(valueOnBytes[0]);
        commandFrame.setMarker(8);
        commandFrame.writeByte(valueOnBytes[1]);
        commandFrame.writeByte((byte) 0x00);

        return commandFrame;
    }

    public Buffer buildBinaryControlCommand(byte operateMode, int index, byte controlCode,
            int timeOn, int timeOff) {
        Buffer commandFrame = appSnd.buildRequestMsg(operateMode, BINARY_OUTPUT_COMMAND, (byte) 1,
                new int[] {index}, WITH_DATA);

        commandFrame.writeByte(DataObject.toBytes(index, 1)[0]);
        commandFrame.setMarker(7);
        commandFrame.writeByte(controlCode);

        byte[] timeOnBytes = DataObject.toBytes(timeOn, 4);
        commandFrame.setMarker(9);
        commandFrame.writeByte(timeOnBytes[0]);
        commandFrame.setMarker(10);
        commandFrame.writeByte(timeOnBytes[1]);
        commandFrame.setMarker(11);
        commandFrame.writeByte(timeOnBytes[2]);
        commandFrame.setMarker(12);
        commandFrame.writeByte(timeOnBytes[3]);

        byte[] timeOffBytes = DataObject.toBytes(timeOff, 4);

        commandFrame.setMarker(13);
        commandFrame.writeByte(timeOffBytes[0]);
        commandFrame.setMarker(14);
        commandFrame.writeByte(timeOffBytes[1]);
        commandFrame.setMarker(15);
        commandFrame.writeByte(timeOffBytes[2]);
        commandFrame.setMarker(16);
        commandFrame.writeByte(timeOffBytes[3]);
        commandFrame.writeByte((byte) 0x00);

        return commandFrame;
    }

    public Buffer buildSetTimeAndDateMsg() {
        return appSnd.buildRequestMsg(WRITE, TIME_COMMAND, (byte) 1, (byte) 1, WITH_DATA);
    }

    public void stop() throws Exception {
        appRcv.setSTOP(true);
        lnkRcv.setSTOP(true);
        phyLayer.close();
    }

    private boolean resetLink(long timeout) throws Exception {
        boolean linkStatus = false;
        linkStatus = lnkSnd.getLnkRcv().initLink(timeout);
        return linkStatus;
    }

    public AppRcv getAppRcv() {
        return appRcv;
    }

    public void setAppRcv(AppRcv appRcv) {
        this.appRcv = appRcv;
    }

    public AppSnd getAppSnd() {
        return appSnd;
    }

    public void setAppSnd(AppSnd appSnd) {
        this.appSnd = appSnd;
    }

    public TransportLayer getTranspLayer() {
        return transpLayer;
    }

    public void setTranspLayer(TransportLayer transpLayer) {
        this.transpLayer = transpLayer;
    }

    public LnkRcv getLnkRcv() {
        return lnkRcv;
    }

    public void setLnkRcv(LnkRcv lnkRcv) {
        this.lnkRcv = lnkRcv;
    }

    public LnkSnd getLnkSnd() {
        return lnkSnd;
    }

    public void setLnkSnd(LnkSnd lnkSnd) {
        this.lnkSnd = lnkSnd;
    }

    public PhyLayer getPhyLayer() {
        return phyLayer;
    }

    public void setPhyLayer(PhyLayer phyLayer) {
        this.phyLayer = phyLayer;
    }

    public void setConfig(DNPConfig config) {
        this.config = config;
    }

    public DNPConfig getConfig() {
        return config;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Database getDatabase() {
        return database;
    }

    public Lock getUserRcvLock() {
        return userRcvLock;
    }
}
