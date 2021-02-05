package br.org.scadabr.dnp34j.master.layers.physical;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.layers.DataMap;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.1.1.1 $
 */
public class PhyETHERNET implements InitFeatures {
    private static final Logger LOG = LoggerFactory.getLogger(PhyETHERNET.class);

    // =============================================================================
    // Attributes
    // =============================================================================
    private InputStream inputStream;
    private OutputStream outputStream;
    // private String commAddress;
    private String host;
    private int port;
    private PhyLayer phyLayer;
    private Socket socket;
    private ServerSocket serverSocket;

    // =============================================================================
    // Constructor
    // =============================================================================
    public PhyETHERNET() {
    }

    public PhyETHERNET(PhyLayer parent) throws Exception {
        this.phyLayer = parent;
        initialize(phyLayer);
    }

    // =============================================================================
    // Methods
    // =============================================================================

    public void init() throws Exception {
        initialize(phyLayer);
    }

    /**
     * DOCUMENT ME!
     *
     * @throws Exception
     */
    private void initialize(PhyLayer phyLayer) throws Exception {

        host = phyLayer.getCommAddress();
        port = phyLayer.getPort();

        boolean connectionAccepted = false;

        if(LOG.isDebugEnabled()) {
            LOG.debug("Trying to connect to:");
            LOG.debug("http://" + host + ":" + port);
        }

        socket = new Socket(host, port);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Created socket:");
            LOG.debug(socket.toString());
        }
        while (!connectionAccepted) {
            connectionAccepted = true;

            if(LOG.isDebugEnabled()) {
                LOG.debug("[PhyLayer] Attempting to connect "
                        + phyLayer.getUri());
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("[PhyLayer] Connected to " + phyLayer.getUri());
        }

        setOutputStream(socket.getOutputStream());
        setInputStream(socket.getInputStream());
    }

    public void close() throws Exception {
        closeClient();
    }

    /**
     * DOCUMENT ME!
     *
     * @throws Exception
     */
    protected void closeClient() throws Exception {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            String msg = "[PhyETHERNET] - socket.close() failed. Exception throwed!";
            e.printStackTrace();
            throw new Exception(msg, e);
        }
    }

    /**
     * @return the inputStream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @param inputStream
     *            the inputStream to set
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return the outputStream
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @param outputStream
     *            the outputStream to set
     */
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(int port) {
        this.port = port;
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
     * @return the socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @param socket
     *            the socket to set
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * @return the serverSocket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * @param serverSocket
     *            the serverSocket to set
     */
    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
}
