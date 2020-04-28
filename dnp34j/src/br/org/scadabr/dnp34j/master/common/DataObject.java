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
     * Display Data Objects, useful for trace Get ot Set functions
     *
     * @param someDataObjects data objects to display
     *
     * @return String representation of these objects
     */
    public static void displayDataObjects(DataObject[] someDataObjects) {
        String s = new String();

        for (int i = 0; i < someDataObjects.length; i++) {
            if (someDataObjects[i].data != null) {
                s += Utils.Display(someDataObjects[i].data);
            } else {
                s += "null\n";
            }
        }

        if (DEBUG) {
            System.out.println(s);
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Extract an Integer value from an array of bytes, formatted with group and variation
     * attributes. Suits for Counters and Analogs
     *
     * @param group Data Object group
     * @param variation Data Object variation
     * @param data data to extract
     * @param scale scale to apply after unformatting
     * @param offset offset to apply after unformatting
     *
     * @return extracted value
     */
    public static float unformatFloat(byte group, byte variation, byte[] data, int scale,
            int offset) {
        // value
        long state = 0;

        if (group == TIME) {
            state = setTime(data);
        } else {
            int i = 0;

            // Special case for 32 bit Float type
            if ((group == 30 && (variation == 7 || variation == 5 )) || (group == 32 && (variation == 7 || variation == 5 )) || (group == 40 && variation == 3)){
                i++; // com flag
                // Convert to IEEE Float
                ByteBuffer b = ByteBuffer.wrap(Arrays.copyOfRange(data, i, i + 4))
                        .order(ByteOrder.LITTLE_ENDIAN);
                return b.getFloat();
            } else {
                // variation < 3 = com flag
                if (variation < 3) {
                    i++;
                }

                state = ((data[i++] & 0xFF) | ((data[i++] << 8) & 0xFF00));
                // 32 bits?
                if ((variation % 2) == 1) {
                    state |= (((data[i++] << 16) & 0xFF0000) | ((data[i++] << 24) & 0xFF000000));
                } else {
                    if ((state & 0x8000) == 0x8000) {
                        state |= 0xFFFF0000;
                    }
                }
            }
        }
        // apply scale & offset to result
        float result = (state - offset) / scale;

        // System.out.println(result);
        return result;
    }

    public static float unformatCounterFloat(byte group, byte variation, byte[] data, int scale,
            int offset) {
        // value
        long state;
        if (group == TIME) {
            state = setTime(data);
        } else {
            int i = 0;

            if ((variation < 3)) {
                i++;
            }

            state = ((data[i++] & 0xFF) | ((data[i++] << 8) & 0xFF00));
        }
        // apply scale & offset to result
        float result = (state - offset) / scale;
        return result;
    }

    /**
     * Extract a Boolean value from an array of bytes, formatted with group and variation attributes
     * Suits for Binary Input & Output
     *
     * @param group Data Object group
     * @param variation Data Object variation
     * @param data data to extract
     * @param inverted logic to apply after unformatting
     *
     * @return extracted value
     */
    public static Boolean unformatBool(byte group, byte variation, byte[] data, boolean inverted) {
        boolean result;

        if (group == 12) {
            result = ((data[0] == LATCH_ON) || (data[0] == PULSE_ON_CLOSE));
        } else {
            result = ((data[0] & 0x80) != 0);
        }

        // apply scale & offset to result
        result ^= inverted;

        return new Boolean(result);
    }

    // ////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    // ////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Get time from a 6 dnp3-formatted bytes
     *
     * @param someBytes DNP formatted time value
     *
     * @return time extracted
     */
    public static long setTime(byte[] someBytes) {
        long time = DataObject.toLong(someBytes, 0, 6);
        return time;
    }

    public static final long toLong(byte[] byteArray, int offset, int len) {
        long val = 0;
        len = Math.min(len, 8);
        for (int i = (len - 1); i >= 0; i--) {
            val <<= 8;
            val |= (byteArray[offset + i] & 0x00FF);
        }
        return val;
    }

    public static final float toFloat(byte[] byteArray, int offset, int length) {
        // Convert to IEEE Float
        ByteBuffer b = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, offset, offset + length))
                .order(ByteOrder.LITTLE_ENDIAN);
        return b.getFloat();
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

        setTime(result);

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

}
