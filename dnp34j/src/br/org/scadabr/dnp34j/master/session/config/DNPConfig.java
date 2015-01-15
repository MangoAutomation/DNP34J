package br.org.scadabr.dnp34j.master.session.config;

public class DNPConfig {

    private int requestRetries = 5;
    private int reconnectRetries = 5;

    private long requestTimeout = 800;
    private boolean REQ_LNK_CONFIRM;
    private boolean REQ_APP_CONFIRM;

    private int masterAddress;
    //    private int slaveAddress;
    private int[] DNPAddressList;
    private COMM commType;
    private Object commConfig;

    public enum COMM {
        ETHERNET, SERIAL
    }

    public DNPConfig(EthernetParameters parameters, int masterAddress, int slaveAddress) {
        this.commType = COMM.ETHERNET;
        this.commConfig = parameters;
        this.masterAddress = masterAddress;
        //        this.slaveAddress = slaveAddress;
        this.setDNPAddressList(new int[] { masterAddress, slaveAddress });

    }

    public DNPConfig(SerialParameters parameters, int masterAddress, int slaveAddress) {
        this.commType = COMM.SERIAL;
        this.commConfig = parameters;
        this.masterAddress = masterAddress;
        //        this.slaveAddress = slaveAddress;
        this.setDNPAddressList(new int[] { masterAddress, slaveAddress });
    }

    public boolean isREQ_LNK_CONFIRM() {
        return REQ_LNK_CONFIRM;
    }

    public void setREQ_LNK_CONFIRM(boolean rEQLNKCONFIRM) {
        REQ_LNK_CONFIRM = rEQLNKCONFIRM;
    }

    public boolean isREQ_APP_CONFIRM() {
        return REQ_APP_CONFIRM;
    }

    public void setREQ_APP_CONFIRM(boolean rEQAPPCONFIRM) {
        REQ_APP_CONFIRM = rEQAPPCONFIRM;
    }

    public COMM getCommType() {
        return commType;
    }

    public void setCommType(COMM commType) {
        this.commType = commType;
    }

    public Object getCommConfig() {
        return commConfig;
    }

    public void setCommConfig(Object commConfig) {
        this.commConfig = commConfig;
    }

    public int getMasterAddress() {
        return masterAddress;
    }

    public void setMasterAddress(int masterAddress) {
        this.masterAddress = masterAddress;
    }

    //	public int getSlaveAddress() {
    //		return slaveAddress;
    //	}
    //
    //	public void setSlaveAddress(int slaveAddress) {
    //		this.slaveAddress = slaveAddress;
    //	}
    //
    public void setDNPAddressList(int[] dNPAddressList) {
        DNPAddressList = dNPAddressList;
    }

    public int[] getDNPAddressList() {
        return DNPAddressList;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setReconnectRetries(int reconnectRetries) {
        this.reconnectRetries = reconnectRetries;
    }

    public int getReconnectRetries() {
        return reconnectRetries;
    }

    public void setRequestRetries(int requestRetries) {
        this.requestRetries = requestRetries;
    }

    public int getRequestRetries() {
        return requestRetries;
    }

}
