package br.org.scadabr.dnp34j.master.session.config;

public class SerialParameters {
	private String commAddress;
	private int baudrate = 9600;
	private int databits = 8;
	private int stopbits = 1;
	private int parity = 0;

	public SerialParameters(String commAddress, int baudrate) {
		this.commAddress = commAddress;
		this.baudrate = baudrate;
	}

	public SerialParameters(String commAddress, int baudrate, int databits,
			int stopbits, int parity) {
		this.commAddress = commAddress;
		this.baudrate = baudrate;
		this.databits = databits;
		this.stopbits = stopbits;
		this.parity = parity;
	}

	public String getCommAddress() {
		return commAddress;
	}

	public void setCommAddress(String commAddress) {
		this.commAddress = commAddress;
	}

	public int getBaudrate() {
		return baudrate;
	}

	public void setBaudrate(int baudrate) {
		this.baudrate = baudrate;
	}

	public int getDatabits() {
		return databits;
	}

	public void setDatabits(int databits) {
		this.databits = databits;
	}

	public int getStopbits() {
		return stopbits;
	}

	public void setStopbits(int stopbits) {
		this.stopbits = stopbits;
	}

	public int getParity() {
		return parity;
	}

	public void setParity(int parity) {
		this.parity = parity;
	}

}
