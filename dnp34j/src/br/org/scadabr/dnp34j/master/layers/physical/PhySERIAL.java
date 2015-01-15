package br.org.scadabr.dnp34j.master.layers.physical;

import java.io.InputStream;
import java.io.OutputStream;

import br.org.scadabr.dnp34j.master.common.InitFeatures;

import com.serotonin.io.serial.SerialParameters;
import com.serotonin.io.serial.SerialPortException;
import com.serotonin.io.serial.SerialPortProxy;
import com.serotonin.io.serial.SerialUtils;

/**
 * DOCUMENT ME!
 * 
 * @author $author$
 * @version $Revision: 1.1.1.1 $
 */
public class PhySERIAL implements InitFeatures {
    static final boolean DEBUG = !LNK_QUIET;

    // =============================================================================
    // Attributes
    // =============================================================================
    private SerialPortProxy serialPort;
    private PhyLayer phyLayer;
    private String commPortId;
    private int baudrate;
    private int dataBits;
    private int stopBits;
    private int parity;
    private SerialParameters serialParams;
    private InputStream inputStream;
    private OutputStream outputStream;

    // =============================================================================
    // Constructor
    // =============================================================================
    public PhySERIAL(PhyLayer parent) throws Exception {
        this.phyLayer = parent;
        initialize(phyLayer);
    }

    public void init() throws Exception {
        initialize(phyLayer);
    }

    private void initialize(PhyLayer phyLayer) throws Exception {
        if (DEBUG) {
            System.out.print("[PhyLayer] Located Serial Ports : ");
        }

        if (DEBUG) {
            System.out.println("\n Open port " + phyLayer.getUri());
        }

        try {
            setCommPortId(phyLayer.getCommAddress());
            setBaudrate(phyLayer.getBaudrate());
            setDataBits(phyLayer.getDataBits());
            setStopBits(phyLayer.getStopBits());
            setParity(phyLayer.getParity());

        }
        catch (Exception e) {
            // TODO: handle exception
        }

        SerialParameters serialParams = new SerialParameters();

        serialParams.setCommPortId(commPortId);
        serialParams.setBaudRate(baudrate);
        serialParams.setDataBits(dataBits);
        serialParams.setStopBits(stopBits);
        serialParams.setParity(parity);

        // System.out.println("serialParams [commPortId]:: " + commPortId);
        // System.out.println("serialParams [baudrate]:: " + baudrate);
        // System.out.println("serialParams [dataBits]:: " + dataBits);
        // System.out.println("serialParams [stopBits]:: " + stopBits);
        // System.out.println("serialParams [parity]:: " + parity);

        try {
            serialPort = SerialUtils.openSerialPort(serialParams);// (SerialPort)
        }
        catch (SerialPortException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setInputStream(serialPort.getInputStream());
        setOutputStream(serialPort.getOutputStream());

        // try {
        // serialPort.setSerialPortParams(phyLayer.getBaudrate(), phyLayer
        // .getDataBits(), phyLayer.getStopBits(), phyLayer
        // .getParity());
        // } catch (gnu.io.UnsupportedCommOperationException e) {
        // }
    }

    // =============================================================================
    // Methods
    // =============================================================================
    //    public static CommPortIdentifier serialPortIdentifier(String port) throws Exception {
    //        CommPortIdentifier result = null;
    //        Enumeration<?> commEnum = CommPortIdentifier.getPortIdentifiers();
    //        CommPortIdentifier cpi;
    //
    //        while (commEnum.hasMoreElements()) {
    //            cpi = (CommPortIdentifier) commEnum.nextElement();
    //
    //            if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL) {
    //                // if (cpi.getName().substring(3).equals(port.substring(3))) {
    //                if (cpi.getName().equals(port)) {
    //                    result = cpi;
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * DOCUMENT ME!
    //     * 
    //     * @param port
    //     *            DOCUMENT ME!
    //     * 
    //     * @return DOCUMENT ME!
    //     */
    //    public static boolean serialPortAvailable(String port) throws Exception {
    //        boolean available = serialPortIdentifier(port) != null ? true : false;
    //        return available;
    //    }

    public void close() {
        closeClient();
    }

    protected void closeClient() {
        if (serialPort != null) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SerialUtils.close(serialPort);
                        // serialPort.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();
            try {
                t.join(50000);
                if (t.isAlive())
                    t.interrupt();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    public String getCommPortId() {
        return commPortId;
    }

    public void setCommPortId(String commPortId) {
        this.commPortId = commPortId;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    public SerialParameters getSerialParams() {
        return serialParams;
    }

    public void setSerialParams(SerialParameters serialParams) {
        this.serialParams = serialParams;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    // boolean available = false;
    //		
    // if (serialPortIdentifier(port) != null) {
    // available = true;
    // }
}