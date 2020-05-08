package br.org.scadabr.dnp34j.master.layers.physical;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.LnkFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.layers.link.LnkRcv;
import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig.COMM;
import br.org.scadabr.dnp34j.master.session.config.EthernetParameters;
import br.org.scadabr.dnp34j.serial.SerialPortWrapper;

/**
 * <p>
 * This class manages the physical connection with the remote DNP device. It allows link layer to
 * send frames & unlock it when data is available
 *
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class PhyLayer implements LnkFeatures, InitFeatures {
    static final boolean DEBUG = !LNK_QUIET;

    // =============================================================================
    // Attributes
    // =============================================================================
    private InputStream inputStream;
    private OutputStream outputStream;
    private Buffer sendingBytes;
    private String uri;
    private int commType;
    private String commAddress;
    private int port;
    private SerialPortWrapper serialPort;
    private DNPConfig config;
    private LnkRcv lnkRcv;
    private PhySERIAL phySERIAL;
    private PhyETHERNET phyETHERNET;
    private Socket socket;
    private PhyLayer phyLayer;
    private DNPUser user;

    /**
     * Constructor. Initalize physical port, with InitFeatures & MainClass parameters
     */
    // =============================================================================
    // Constructor
    // =============================================================================

    public PhyLayer(DNPUser user) throws Exception {
        this.config = user.getConfig();
        process(config);
        this.user = user;
    }

    // =============================================================================
    // Methods
    // =============================================================================
    private PhyLayer process(DNPConfig config) throws Exception {
        try {
            this.config = config;

            if (config.getCommType() == COMM.ETHERNET) {
                EthernetParameters parameters = (EthernetParameters) config.getCommConfig();
                setCommAddress(parameters.getHost());
                setPort(parameters.getPort());
                setPhyETHERNET(new PhyETHERNET(this));

                setInputStream(phyETHERNET.getInputStream());
                setOutputStream(phyETHERNET.getOutputStream());
            } else if (config.getCommType() == COMM.SERIAL) {
                this.serialPort = (SerialPortWrapper) config.getCommConfig();
                setPhySERIAL(new PhySERIAL(this));
                setInputStream(phySERIAL.getInputStream());
                setOutputStream(phySERIAL.getOutputStream());
            }
            sendingBytes = new Buffer(S);
        } catch (Exception e) {
            this.close();
            throw new Exception("Connection fault", e);
        }
        return phyLayer;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws Exception
     */
    public void reconnect() throws Exception {
        try {
            switch (commType) {
                case SERIAL:
                    if (phySERIAL != null) {
                        phySERIAL.close();
                        phySERIAL.init();
                    }
                    break;
                case ETHERNET:
                    if (phyETHERNET != null) {
                        phyETHERNET.close();
                        phyETHERNET.init();
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("[PhyLayer] - reconnect() failed");
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws Exception
     */
    public synchronized void close() throws Exception {

        if (inputStream != null)
            inputStream.close();
        if (outputStream != null)
            outputStream.close();

        if (phySERIAL != null)
            phySERIAL.close();

        if (phyETHERNET != null)
            phyETHERNET.close();
    }

    /**
     * DOCUMENT ME!
     *
     * @param someByte DOCUMENT ME!
     */
    public synchronized void write(byte[] someByte) throws Exception {
        sendingBytes.reset();
        sendingBytes.writeBytes(someByte);

        try {
            while (sendingBytes.length() > 0) {
                int size = sendingBytes.length();
                outputStream.write(sendingBytes.readBytes(size));
                outputStream.flush();
            }
        } catch (Exception e) {
            if (DEBUG) {
                System.out.println("[PhyLayer] Writing Exception");
                System.out.println("[PhyLayer] Remote Connection closed.");
            }
            user.reportException(e);
        }
    }

    /**
     * @return the inputStream
     */
    public synchronized InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @param inputStream the inputStream to set
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return the outputStream
     */
    public synchronized OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @param outputStream the outputStream to set
     */
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * @return the sendingBytes
     */
    public Buffer getSendingBytes() {
        return sendingBytes;
    }

    /**
     * @param sendingBytes the sendinif (phyETHERNET != null) { phyETHERNET.close();
     *        phyETHERNET.init(); gBytes to set
     */
    public void setSendingBytes(Buffer sendingBytes) {
        this.sendingBytes = sendingBytes;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the comm
     */
    public int getCommType() {
        return commType;
    }

    /**
     * @param comm the comm to set
     */
    public void setCommType(int commType) {
        this.commType = commType;
    }

    /**
     * @return the commAddress
     */
    public String getCommAddress() {
        return commAddress;
    }

    /**
     * @param commAddress the commAddress to set
     */
    public void setCommAddress(String commAddress) {
        this.commAddress = commAddress;
    }

    public SerialPortWrapper getSerialPort() {
        return this.serialPort;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(DNPConfig config) {
        this.config = config;
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
     * @return the phySERIAL
     */
    public PhySERIAL getPhySERIAL() {
        return phySERIAL;
    }

    /**
     * @param phySERIAL the phySERIAL to set
     */
    public void setPhySERIAL(PhySERIAL phySERIAL) {
        this.phySERIAL = phySERIAL;
    }

    /**
     * @return the phyETHERNET
     */
    public PhyETHERNET getPhyETHERNET() {
        return phyETHERNET;
    }

    /**
     * @param phyETHERNET the phyETHERNET to set
     */
    public void setPhyETHERNET(PhyETHERNET phyETHERNET) {
        this.phyETHERNET = phyETHERNET;
    }

    /**
     * @return the socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @param socket the socket to set
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * @return the phyLayer
     */
    public PhyLayer getPhyLayer() {
        return phyLayer;
    }

    /**
     * @param phyLayer the phyLayer to set
     */
    public void setPhyLayer(PhyLayer phyLayer) {
        this.phyLayer = phyLayer;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }
}
