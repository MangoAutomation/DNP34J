/*
 * Miscellaneous utilities
 *
 * Class    com.itlity.protocol.common.utils.Utils
 * File     Utils.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common.utils;

/**
 * DOCUMENT ME!
 * 
 * @author $author$
 * @version $Revision: 1.1.1.1 $
 */
public class Utils {
	// =============================================================================
	// Methods
	// =============================================================================

	/**
	 * Convert the decimal format of a byte to hexa if a byte (ex : 0x23)
	 * represents a decimal value (0x23 represents the decimal value 35), it is
	 * converted in order to represent a hexa value (0x23 is converted to 0x35,
	 * which represents the hexa value 0x35)
	 * 
	 * @param aByte
	 *            byte to convert
	 * 
	 * @return byte converted
	 */
	public static byte decimal2Hexa(byte aByte) {
		return (byte) (((aByte / 10) * 16) + (aByte % 10));
	}

	/**
	 * Convert the hexa format of a byte to decimal if a byte (ex : 0x23)
	 * represents an hexa value (0x23 represents the hexa value 0x23), it is
	 * converted in order to represent a decimal value (0x23 is converted to
	 * 0x17, which represents the decimal value 23)
	 * 
	 * @param aByte
	 *            byte to convert
	 * 
	 * @return byte converted
	 */
	public static byte hexa2Decimal(byte aByte) {
		return (byte) (((aByte / 16) * 10) + (aByte % 16));
	}

	/**
	 * Convert a byte value into a positive int value
	 * 
	 * @param aByte
	 *            byte to convert
	 * 
	 * @return value converted
	 */
	public static int byte2int(byte aByte) {
		return (aByte < 0) ? (aByte + 256) : aByte;
	}

	/**
	 * Display a char
	 * 
	 * @param b
	 *            char to display
	 * 
	 * @return String representation of this char
	 */
	public static String DisplayChar(char b) {
		String s = Integer.toHexString(b);
		return "0x" + s;
	}

	/**
	 * Display a byte
	 * 
	 * @param b
	 *            byte to display
	 * 
	 * @return String representation of this byte
	 */
	public static String DisplayByte(byte b) {
		String s = Integer.toHexString(b);

		if (s.length() > 2) {
			s = s.substring(6);
		}

		return "0x" + s;
	}

	/**
	 * Display an array of bytes
	 * 
	 * @param b
	 *            an array of bytes to display
	 * 
	 * @return String representation of this array of bytes
	 */
	public static String Display(byte[] b) {
		return Display(b, b.length);
	}

	/**
	 * Display some bytes of an array of bytes
	 * 
	 * @param b
	 *            an array of bytes to display
	 * @param length
	 *            number of bytes to display
	 * 
	 * @return String representation of these bytes
	 */
	public static String Display(byte[] b, int length) {
		String s = new String();

		for (int i = 0; i < length; i++) {
			if ((i == 0) || (i == 10) || (((i - 10) % 18) == 0)) {
				s += "\n";
			}

			s += ("[" + i + "]:" + DisplayByte(b[i]) + " ");
		}

		return s + "\n";
	}

	/**
	 * Compare two byte arrays.
	 * 
	 * @param array1
	 *            a byte array
	 * @param array2
	 *            a byte array
	 * 
	 * @return <tt>true</tt> if they're equal, <tt>false</tt> if they're
	 *         different or if one of them at least equals <tt>null</tt>
	 */
	public static boolean areEqual(byte[] array1, byte[] array2) {
		if ((array1 == null) | (array2 == null)) {
			return false;
		}

		if (array1.length != array2.length) {
			return false;
		}

		for (int i = array1.length - 1; i > -1; i--) {
			if (array1[i] != array2[i]) {
				return false;
			}
		}

		return true;
	}
}