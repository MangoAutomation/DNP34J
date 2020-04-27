package br.org.scadabr.dnp34j.samples;

import java.util.HashMap;
import java.util.List;

import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;
import br.org.scadabr.dnp34j.master.session.config.EthernetParameters;
import br.org.scadabr.dnp34j.master.session.database.DataBuffer;
import br.org.scadabr.dnp34j.master.session.database.DataElement;
import br.org.scadabr.dnp34j.serial.SerialPortWrapper;

public class ScadaBRInterface {
    private DNPUser user;
    private DNPConfig configuration;
    private int staticPollFrequence = 30;

    public void initEthernet(EthernetParameters parameters, int masterAddress, int slaveAddress,
            int staticPollFrequence) throws Exception {
        configuration = new DNPConfig(parameters, masterAddress, slaveAddress);
        this.staticPollFrequence = staticPollFrequence;
        init(configuration);
    }

    public void initSerial(SerialPortWrapper parameters, int masterAddress, int slaveAddress,
            int staticPollFrequence) throws Exception {

        configuration = new DNPConfig(parameters, masterAddress, slaveAddress);
        this.staticPollFrequence = staticPollFrequence;
        init(configuration);
    }

    private void init(DNPConfig configuration) throws Exception {
        user = new DNPUser(configuration);

        user.init();
    }

    private int pollCount = 0;

    public void doPoll() throws Exception {
        if (pollCount == 0) {
            doStaticPoll();
            pollCount++;
        } else {
            doRBEPoll();
            pollCount++;
            if (pollCount > staticPollFrequence)
                pollCount = 0;
        }
        Thread.sleep(100);
    }

    private void doStaticPoll() throws Exception {
        user.sendSynch(user.buildReadStaticDataMsg());
    }

    private void doRBEPoll() throws Exception {
        user.sendSynch(user.buildReadAllEventDataMsg());
    }

    public void terminate() throws Exception {
        user.stop();
    }

    public HashMap<Integer, DataBuffer> getBinaryInputPoints() {
        return user.getDatabase().getBinaryInputPoints();
    }

    public HashMap<Integer, DataBuffer> getBinaryOutputPoints() {
        return user.getDatabase().getBinaryOutputPoints();
    }

    public HashMap<Integer, DataBuffer> getCounterInputPoints() {
        return user.getDatabase().getCounterInputPoints();
    }

    public HashMap<Integer, DataBuffer> getAnalogInputPoints() {
        return user.getDatabase().getAnalogInputPoints();
    }

    public HashMap<Integer, DataBuffer> getAnalogOutputPoints() {
        return user.getDatabase().getAnalogOutputPoints();
    }

    public DataBuffer getBinaryInputPoints(int index) {
        return user.getDatabase().getBinaryInputPoints().get(index);
    }

    public DataBuffer getBinaryOutputPoints(int index) {
        return user.getDatabase().getBinaryOutputPoints().get(index);
    }

    public DataBuffer getCounterInputPoints(int index) {
        return user.getDatabase().getCounterInputPoints().get(index);
    }

    public DataBuffer getAnalogInputPoints(int index) {
        return user.getDatabase().getAnalogInputPoints().get(index);
    }

    public DataBuffer getAnalogOutputPoints(int index) {
        return user.getDatabase().getAnalogOutputPoints().get(index);
    }

    public DataElement getElement(byte group, int index) {
        // return null;
        List<DataElement> list = user.getDatabase().read(index, group);
        if (list.isEmpty())
            return null;
        if (list.size() > 1)
            throw new RuntimeException("DataBuffer contained " + list.size() + " records");
        return list.get(0);
    }
}
