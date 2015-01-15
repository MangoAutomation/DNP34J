package br.org.scadabr.dnp34j.master.layers.physical;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import br.org.scadabr.dnp34j.master.common.InitFeatures;

/**
 * DOCUMENT ME!
 * 
 * @author $author$
 * @version $Revision: 1.1.1.1 $
 */
public class PhyETHERNET implements InitFeatures {
	static final boolean DEBUG = !LNK_QUIET;

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

		if (DEBUG) {
			System.out.println("Trying to connect to:");
			System.out.println("http://" + host + ":" + port);
		}

		try {
			socket = new Socket(host, port);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (DEBUG) {
			System.out.println("Created socket:");
			System.out.println(socket);
		}
		while (!connectionAccepted) {
			connectionAccepted = true;

			if (DEBUG) {
				System.out.println("[PhyLayer] Attempting to connect "
						+ phyLayer.getUri());
			}
		}

		if (DEBUG) {
			System.out.println("[PhyLayer] Connected to " + phyLayer.getUri());
		}

		try {
			setOutputStream(socket.getOutputStream());
			setInputStream(socket.getInputStream());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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