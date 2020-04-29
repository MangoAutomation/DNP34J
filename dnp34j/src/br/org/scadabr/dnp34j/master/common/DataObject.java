/*
 * Data Object class
 *
 * Class com.itlity.protocol.common.DataObject File DataObject.java Author Alexis CLERC
 * <alexis.clerc@sysaware.com> (c) SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import br.org.scadabr.dnp34j.master.common.utils.Utils;

/**
 * <p>
 * Properties of DNP3 library objects
 *
 * @author Alexis CLERC
 */
public class DataObject implements InitFeatures, DataMapFeatures {
    static final boolean DEBUG = !DATABASE_QUIET;

    /**
     * Object group
     */
    public byte group;

    /**
     * Object variation
     */
    public byte variation;

    /**
     * Object data
     */
    public byte[] data;

    // =============================================================================
    // Constructor
    // =============================================================================

    /**
     * Build a dataObject
     *
     * @param type DataType of this object
     * @param g Object group
     * @param v Object variation
     *
     * @throws IllegalArgumentException
     *         <ul>
     *         <li>objectType doesn't match with group attribute
     *         <li>object with this group and variation attributes is not known
     *         </ul>
     */
    public DataObject(byte type, byte g, byte v) {
        if (!isValid(type, g, v)) {
            throw new IllegalArgumentException("Unknown Data Object");
        }

        group = g;
        variation = v;
        data = null;
    }

    /**
     * Build a dataObject
     *
     * @param g Object group
     * @param v Object variation
     *
     * @throws IllegalArgumentException
     *         <ul>
     *         <li>object with this group and variation attributes is not known
     *         </ul>
     */
    public DataObject(byte g, byte v) {
        if (!isValid(g, v)) {
            throw new IllegalArgumentException("Unknown Data Object");
        }

        group = g;
        variation = v;
        data = null;
    }

    /**
     * Build a dataObject filled with data
     *
     * @param g Object group
     * @param v Object variation
     * @param someBytes Object data
     *
     * @throws IllegalArgumentException
     *         <ul>
     *         <li>object with this group and variation attributes is not known
     *         </ul>
     */
    public DataObject(byte g, byte v, byte[] someBytes) {
        if (!isValid(g, v)) {
            throw new IllegalArgumentException("Unknown Data Object");
        }

        if (((DataLengths.getDataLength(g, v) + 7) / 8) != someBytes.length) {
            throw new IllegalArgumentException("Frame length (" + someBytes.length
                    + ") doesn't match with this DataObject (" + g + ", " + v + ")");
        }

        group = g;
        variation = v;
        data = someBytes;
    }

    // =============================================================================
    // Methods
    // =============================================================================

    /**
     * Verify if this object is include in DNP Object Table
     *
     * @param g Object group
     * @param v Object variation
     *
     * @return object is include in DNP Object Table
     */
    public static boolean isValid(byte g, byte v) {
        return (DataLengths.getDataLength(g, v) != -1);
    }

    /**
     * Verify if this object is include in DNP Object Table and if group matches with the type of
     * the data
     *
     * @param g Object group
     * @param v Object variation
     *
     * @return object is include in DNP Object Table and group matches with the type of the data
     */
    public static boolean isValid(byte type, byte g, byte v) {
        return ((type == (Utils.decimal2Hexa(g) & 0xF0)) && (isValid(g, v)));
    }

    /**
     * Display this Data Object
     *
     * @return String representation of Data Object
     */
    public String output() {
        return ("G : " + group + ", V : " + variation
                + ((data != null) ? (", data : " + Utils.Display(data)) : ""));
    }

    // ////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Get an ObjectType (BIN_IN, BIN_OUT, COUNTER, ANA_IN, ..) from a group attribute
     *
     * @param group group attribute
     *
     * @return an object type
     */
    public static byte getObjectType(byte group) {
        return (byte) (Utils.decimal2Hexa(group) & 0xF0);
    }

    /**
     * Convert to long from bytes
     * @param byteArray
     * @param offset
     * @param len
     * @return
     */
    public static final long toLong(byte[] byteArray, int offset, int len) {
        long val = 0;
        len = Math.min(len, 8);
        for (int i = (len - 1); i >= 0; i--) {
            val <<= 8;
            val |= (byteArray[offset + i] & 0x00FF);
        }
        return val;
    }

    /**
     * Convert to IEEE Float from bytes this requires 4 bytes in the array after offset
     * @param byteArray
     * @param offset
     * @return
     */
    public static final float toFloat(byte[] byteArray, int offset) {
        if(byteArray.length < offset + 4) {
            throw new IllegalArgumentException("Too few bytes to extract float.");
        }
        // Convert to IEEE Float
        ByteBuffer b = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, offset, offset + 4))
                .order(ByteOrder.LITTLE_ENDIAN);
        return b.getFloat();
    }

    /**
     * Convert to IEEE Float from bytes this requires 8 bytes in the array after offset
     * @param byteArray
     * @param offset
     * @return
     */
    public static final double toDouble(byte[] byteArray, int offset) {
        if(byteArray.length < offset + 8) {
            throw new IllegalArgumentException("Too few bytes to extract double.");
        }

        // Convert to IEEE Float
        ByteBuffer b = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, offset, offset + 8))
                .order(ByteOrder.LITTLE_ENDIAN);
        return b.getDouble();
    }

    /**
     * Convert from float to IEEE float bytes of specified number of bytes
     * @param value
     * @return
     */
    public static final byte[] getFloat(float value) {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }

    /**
     * Convert from double 8 bytes
     * @param value
     * @return
     */
    public static final byte[] getDouble(double value) {
        return ByteBuffer.allocate(8).putDouble(value).array();
    }


    /**
     * Format CurrentTime to a 6 dnp3-formatted bytes
     *
     * @return DNP formatted time value
     */
    public static byte[] getTime(long time) {

        byte[] result = new byte[6];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) ((time >> (8 * i)) & 0xFF);
        }

        return result;
    }


    /**
     * get time Delay from static parameters
     */
    public static byte[] getTimeDelay(long delay, byte variation) {
        byte[] result = new byte[2];

        if (variation == COARSE) {
            delay = (delay / 1000);
        }

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) ((delay >> (8 * i)) & 0xFF);
        }

        return result;
    }

    /**
     * Convert int to bytes
     * @param value
     * @param size
     * @return
     */
    public static byte[] toBytes(int value, int size) {
        byte[] result = new byte[size];

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
        return result;
    }

}
