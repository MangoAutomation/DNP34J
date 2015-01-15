package br.org.scadabr.dnp34j.master.session.config;

public class EthernetParameters {
	private String host;
	private int port;

	public EthernetParameters(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

}
