/*
 * Circular memory buffer
 *
 * Class    com.itlity.protocol.common.utils.Buffer
 * File     Buffer.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common.utils;

import br.org.scadabr.dnp34j.master.common.InitFeatures;

/**
 * <p>
 * Circular memory buffer for a link, transport, application or user layer frame
 * management, with 2 pointers
 * <ul>
 * <li>offset : read pointer
 * <li>marker : write pointer
 * </ul>
 * 
 * Data window is between these 2 values It contains remaining bytes to read
 * 
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class Buffer implements InitFeatures {
	static final boolean DEBUG = !QUIET;

	// =============================================================================
	// Attributes
	// =============================================================================

	/**
	 * Buffer capacity
	 */
	private int size;

	/**
	 * Data area
	 */
	private byte[] buffer;

	/**
	 * Read access pointer
	 */
	private int offset;

	/**
	 * Write access pointer
	 */
	private int marker;

	// =============================================================================
	// Constructor
	// =============================================================================

	/**
	 * Build an empty circular memory buffer with a medium capacity
	 */
	public Buffer() {
		size = M;
		buffer = new byte[size];
		offset = 0;
		marker = 0;
	}

	/**
	 * Build a circular memory buffer with a medium capacity initialized with
	 * some data
	 * 
	 * @param someBytes
	 *            data with which the buffer is initialized
	 */
	public Buffer(byte[] someBytes) {
		size = M;
		buffer = new byte[size];
		System.arraycopy(someBytes, 0, buffer, 0, someBytes.length);
		offset = 0;
		marker = someBytes.length;
	}

	/**
	 * Build an empty circular memory buffer with specified capacity
	 * 
	 * @param s
	 *            buffer capacity
	 */
	public Buffer(int s) {
		size = s;
		buffer = new byte[size];
		offset = 0;
		marker = 0;
	}

	/**
	 * Build a circular memory buffer initialized with some data with specified
	 * capacity
	 * 
	 * @param s
	 *            buffer capacity
	 * @param someBytes
	 *            data with which the buffer is initialized
	 */
	public Buffer(int s, byte[] someBytes) {
		size = s;
		buffer = new byte[size];
		System.arraycopy(someBytes, 0, buffer, 0, someBytes.length);
		offset = 0;
		marker = someBytes.length;
	}

	// =============================================================================
	// Methods
	// =============================================================================

	/**
	 * Empty the window
	 */
	public void reset() {
		offset = marker;
	}

	/**
	 * Increment the offset pointer to skip the reading of some bytes. Useful to
	 * read some bytes further
	 * 
	 * @param length
	 *            offset pointer incrementation
	 */
	public void incrOffset(int length) {
		if (length > size) {
			if (DEBUG) {
				System.out.println("[Buffer] WARNING : incrOffset too large");
			}
		}

		if (length > length()) {
			if (DEBUG) {
				System.out
						.println("[Buffer] ERROR : incrOffset -> offset overflow");
			}
		}

		offset = (offset + length) % size;
	}

	/**
	 * Decrement the offset pointer to recover previous bytes.
	 * 
	 * @param length
	 *            offset pointer decrementation
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             decrOffset too large, or offset overflow
	 */
	public void decrOffset(int length) {
		if (length > size) {
			throw new ArrayIndexOutOfBoundsException(
					"[Buffer] ERROR : decrOffset too large");
		}

		if (length > (size - length())) {
			throw new ArrayIndexOutOfBoundsException(
					"[Buffer] ERROR : decrOffset -> offset overflow");
		}

		offset = (offset - length) % size;
		offset += ((offset < 0) ? size : 0);
	}

	/**
	 * Increment the marker pointer to skip the writing of some bytes.
	 * 
	 * @param length
	 *            marker pointer incrementation
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             incrMarker too large, or marker overflow
	 */
	public void incrMarker(int length) {
		if (length > size) {
			throw new ArrayIndexOutOfBoundsException(
					"[Buffer] ERROR : incrMarker too large");
		}

		if (length > (size - length())) {
			throw new ArrayIndexOutOfBoundsException(
					"[Buffer] ERROR : incrMarker -> marker overflow");
		}

		marker = (marker + length) % size;
	}

	/**
	 * Decrement the marker pointer to recover previous bytes.
	 * 
	 * @param length
	 *            marker pointer decrementation
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             decrMarker too large, or marker overflow
	 */
	public void decrMarker(int length) {
		if (length > size) {
			throw new ArrayIndexOutOfBoundsException(
					"[Buffer] ERROR : decrMarker too large");
		}

		if (length > length()) {
			throw new ArrayIndexOutOfBoundsException(
					"[Buffer] ERROR : decrMarker -> marker overflow");
		}

		marker = (marker - length) % size;
		marker += ((marker < 0) ? size : 0);
	}

	/**
	 * Length of current window
	 * 
	 * @return the length of current window
	 */
	public int length() {
		int length = (marker - offset) % size;
		length += ((length < 0) ? size : 0);

		return length;
	}

	/**
	 * Pop the first byte of current window.
	 * 
	 * @return this byte
	 */
	public byte readByte() {
		if (length() == 0) {
			if (DEBUG) {
				System.out
						.println("[Buffer] ERROR : readByte -> nothing to read");
			}
		}

		byte result = buffer[offset];
		offset = (offset + 1) % size;

		return result;
	}

	/**
	 * Pop some bytes of current window.
	 * 
	 * @return these bytes
	 */
	public byte[] readBytes(int length) throws IndexOutOfBoundsException {
		// if (length > length())
		// throw new
		// RuntimeException("[Buffer] ERROR : readBytes -> not enough bytes to read");
		byte[] result = new byte[length];
		int s = Math.min(length, size - offset);
		System.arraycopy(buffer, offset, result, 0, s);
		offset = (offset + s) % size;

		if (s < length) {
			System.arraycopy(buffer, 0, result, s, length - s);
			offset = length - s;
		}

		return result;
	}

	/**
	 * Pop all bytes of current window.
	 * 
	 * @return these bytes
	 */
	public byte[] readBytes() {
		return readBytes(length());
	}

	/**
	 * Push a byte on current window.
	 * 
	 * @param aByte
	 *            byte added to current window
	 */
	public void writeByte(byte aByte) {
		buffer[marker] = aByte;
		marker = (marker + 1) % size;
	}

	/**
	 * Push 2 bytes on current window.
	 * 
	 * @param anInt
	 *            value added to current window as 2 LSB bytes
	 */
	public void writeBytes(int anInt) {
		writeByte((byte) (anInt & 0xFF));
		writeByte((byte) ((anInt >> 8) & 0xFF));
	}

	/**
	 * Push some bytes on current window.
	 * 
	 * @param someBytes
	 *            bytes added to current window
	 */
	public void writeBytes(byte[] someBytes) throws IndexOutOfBoundsException {
		if (someBytes == null) {
			return;
		}

		int s = Math.min(someBytes.length, size - marker);
		System.arraycopy(someBytes, 0, buffer, marker, s);
		marker = (marker + s) % size;

		if (s < someBytes.length) {
			System.arraycopy(someBytes, s, buffer, 0, someBytes.length - s);
			marker = someBytes.length - s;
		}
	}

	/**
	 * Push some bytes on current window.
	 * 
	 * @param someInts
	 *            values added to current window
	 */
	public void writeBytes(int[] someInts) {
		if (someInts == null) {
			return;
		}

		for (int i = 0; i < someInts.length; i++) {
			writeBytes(someInts[i]);
		}
	}

	/**
	 * Segment of current window.
	 * 
	 * @param start
	 *            begenning of this segment
	 * @param stop
	 *            end of this segment (included)
	 * 
	 * @return this segment
	 */
	public byte[] value(int start, int stop) throws IndexOutOfBoundsException {
		start = (offset + start) % size;
		stop = (offset + stop) % size;

		int length = (stop - start + 1) % size;
		length += ((length < 0) ? size : 0);

		byte[] result = new byte[length];
		int s = Math.min(result.length, size - start);
		System.arraycopy(buffer, start, result, 0, s);

		if (s < result.length) {
			System.arraycopy(buffer, 0, result, s, result.length - s);
		}

		return result;
	}

	/**
	 * Current window.
	 * 
	 * @return display current window
	 */
	public byte[] value() {
		return value(0, length() - 1);
	}

	/**
	 * A byte of current window.
	 * 
	 * @param number
	 *            number of selected byte
	 * 
	 * @return value of selected byte
	 */
	public byte value(int number) throws ArrayIndexOutOfBoundsException {
		number = (offset + number) % size;

		return buffer[number];
	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return the buffer
	 */
	public byte[] getBuffer() {
		return buffer;
	}

	/**
	 * @param buffer
	 *            the buffer to set
	 */
	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param offset
	 *            the offset to set
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * @return the marker
	 */
	public int getMarker() {
		return marker;
	}

	/**
	 * @param marker
	 *            the marker to set
	 */
	public void setMarker(int marker) {
		this.marker = marker;
	}
}