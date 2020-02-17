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

    // =============================================================================
    // Attributes
    // =============================================================================

    /**
     * Size (bits) of each data object supported Variation V is in V-1 case of the array Intended to
     * be used with length() method
     */

    // static byte[][][] size =
    // {
    // { null, { 1, 8 }, { 8, 56, -5 } },
    //
    // { { 1, 8 }, null, { 88 } },
    //
    // { { 40, 24,0, 0, 32, 16 }, null, { 40, 24, 0, 0, 88, 72 } },
    //
    // { { 40, 24, 32, 16 }, null, { 40, 24, 88, 72 } },
    //
    // { { 40, 24 }, { 40, 24 } },
    //
    // { { 48 }, {-7}, { 16, 16 } },
    //
    // { { 0 } }, null, { { 1 } }
    //
    // };

    static byte[][][] size = {{null, {1, // v01 : Bin Input
            8}, // v02 : Bin Input with status
            {8, // v01 : Bin Input Change
                    56 // v02 : Bin Input Change with time
                    , 24}}, // v03 : Bin Input Change with relative time

            {{1, // v01 : Bin Output
                    8}, // v02 : Bin Output with status
                    null,

                    {88 // v01 : Control Relay Output Block
                    }},

            {{40, // v01 : 32-Bit Bin Counter
                    24, // v02 : 16-Bit Bin Counter
                    0, 0, 32, // v05 : 32-Bit Counter without Flag
                    16}, // v06 : 16-Bit Counter without Flag
                    {-40, -24, -40, -24, -88, -72, -88, -72, -32},

                    {40, // v01 : 32-Bit Counter Change without time
                            24, // v02 : 16-Bit Counter Change without time
                            0, 0, 88, // v05 : 32-Bit Counter Change with time
                            72}},

            {{40, // v01 : 32-Bit Ana Input
                    24, // v02 : 16-Bit Ana Input
                    32, // v03 : 32-Bit Ana Input without Flag
                    16}, // v04 : 16-Bit Ana Input without Flag
                    null,

                    {40, // v01 : 32-Bit Ana Change Event without Time
                            24, // v02 : 16-Bit Ana Change Event without Time
                            88, // v03 : 32-Bit Ana Change Event with Time
                            72, // v04 ??
                            -1, // v05 ??
                            -1, // v06 ??
                            88 // v07 : 32-Bit Floating Point Change Event with Time
                    }},

            {{40, // v01 : 32-Bit Ana Output Status
                    24}, // v02 : 16-Bit Ana Output Status
                    {40, // v01 : 32-Bit Ana Output Block
                            24 // v02 : 16-Bit Ana Output Block
                    }},

            {{48}, // v01 : Time and Date
                    {-48},

                    {16, // v01 : Time Delay Coarse
                            16 // v02 : Time Delay Fine
                    }},

            {{0 // v 01 : doesn't carry any information in itself
            }},

            null,

            {{1 // v 01 : Internal Indications
            }}};

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

        if (((length(g, v) + 7) / 8) != someBytes.length) {
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
     * DataObject length. This method is called when group and length are coded in decimal format
     * 
     * @param g Object group
     * @param v Object variation
     * 
     * @return object size, <tt>0</tt> if there's no object coding, <tt>-1</tt> if this object is
     *         not supported
     */
    public static byte length(byte g, byte v) {
        return fixedLength(Utils.decimal2Hexa(g), v);
    }

    /**
     * DataObject length. This method is called when group and length are coded in hexa format
     * 
     * @param g Object group
     * @param v Object variation
     * 
     * @return object size, 0 if there's no object coding, -1 if this object is not supported
     */
    public static byte fixedLength(byte g, byte v) {
        int first4bits = (int) ((g >> 4) & 0x0F);
        int last4bits = (int) (g & 0x0F);
        byte res;

        // System.out.println("fixedLength() Group: " + g + " Variation: " + v);
        // System.out.println("first4Bits: " + first4bits);
        // System.out.println("last4Bits: " + last4bits);

        // System.out.println("size[" + first4bits + "][" + last4bits + "]["
        // + (v - 1) + "]");
        try {
            res = size[first4bits][last4bits][v - 1];
        } catch (Exception e) {
            res = -1;
        }

        return res;
    }

    /**
     * Verify if this object is include in DNP Object Table
     * 
     * @param g Object group
     * @param v Object variation
     * 
     * @return object is include in DNP Object Table
     */
    public static boolean isValid(byte g, byte v) {
        return (length(g, v) != -1);
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

            // Special case for Float type
            if (variation == 7) {
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

        long i = setTime(result);

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
    // /**
    // * <p>
    // * Properties of DNP3 library objects
    // *
    // * @author Alexis CLERC
    // */
    // public class DataObject implements InitFeatures, DataMapFeatures {
    // static final boolean DEBUG = !DATABASE_QUIET;
    //
    // //=============================================================================
    // // Attributes
    // //=============================================================================
    //
    // /**
    // * Size (bits) of each data object supported
    // * Variation V is in V-1 case of the array
    // * Intended to be used with length() method
    // */
    // static byte[][][] size =
    // {
    // {
    // null,
    // { 1, // v01 : Bin Input
    // 8 }, // v02 : Bin Input with status
    // { 8, // v01 : Bin Input Change
    // 56 // v02 : Bin Input Change with time
    // }
    // },
    //
    // {
    // { 1, // v01 : Bin Output
    // 8 }, // v02 : Bin Output with status
    // null,
    //
    // { 88 // v01 : Control Relay Output Block
    // }
    // },
    //
    // {
    // {
    // 40, //v01 : 32-Bit Bin Counter
    // 24, //v02 : 16-Bit Bin Counter
    // 0, 0, 32, // v05 : 32-Bit Counter without Flag
    // 16
    // }, // v06 : 16-Bit Counter without Flag
    // null,
    //
    // {
    // 40, //v01 : 32-Bit Counter Change without time
    // 24, //v02 : 16-Bit Counter Change without time
    // 0, 0, 88, // v05 : 32-Bit Counter Change with time
    // 72
    // }
    // },
    //
    // {
    // {
    // 40, // v01 : 32-Bit Ana Input
    // 24, // v02 : 16-Bit Ana Input
    // 32, // v03 : 32-Bit Ana Input without Flag
    // 16
    // }, // v04 : 16-Bit Ana Input without Flag
    // null,
    //
    // {
    // 40, // v01 : 32-Bit Ana Change Event without Time
    // 24, // v02 : 16-Bit Ana Change Event without Time
    // 88, // v03 : 32-Bit Ana Change Event with Time
    // 72
    // }
    // },
    //
    // {
    // { 40, // v01 : 32-Bit Ana Output Status
    // 24 }, // v02 : 16-Bit Ana Output Status
    // { 40, // v01 : 32-Bit Ana Output Block
    // 24 // v02 : 16-Bit Ana Output Block
    // }
    // },
    //
    // {
    // { 48 }, // v01 : Time and Date
    // null,
    //
    // { 16, // v01 : Time Delay Coarse
    // 16 // v02 : Time Delay Fine
    // }
    // },
    //
    // {
    // { 0 // v 01 : doesn't carry any information in itself
    // }
    // },
    //
    // null,
    //
    // {
    // { 1 // v 01 : Internal Indications
    // }
    // }
    // };
    //
    // /**
    // * Object group
    // */
    // public byte group;
    //
    // /**
    // * Object variation
    // */
    // public byte variation;
    //
    // /**
    // * Object data
    // */
    // public byte[] data;
    //
    // //=============================================================================
    // // Constructor
    // //=============================================================================
    //
    // /**
    // * Build a dataObject
    // *
    // * @param type DataType of this object
    // * @param g Object group
    // * @param v Object variation
    // *
    // * @throws IllegalArgumentException
    // * <ul>
    // * <li> objectType doesn't match with group attribute
    // * <li> object with this group and variation attributes is not known
    // * </ul>
    // */
    // public DataObject(byte type, byte g, byte v) {
    // if (!isValid(type, g, v)) {
    // throw new IllegalArgumentException("Unknown Data Object");
    // }
    //
    // group = g;
    // variation = v;
    // data = null;
    // }
    //
    // /**
    // * Build a dataObject
    // *
    // * @param g Object group
    // * @param v Object variation
    // *
    // * @throws IllegalArgumentException
    // * <ul>
    // * <li> object with this group and variation attributes is not known
    // * </ul>
    // */
    // public DataObject(byte g, byte v) {
    // if (!isValid(g, v)) {
    // throw new IllegalArgumentException("Unknown Data Object");
    // }
    //
    // group = g;
    // variation = v;
    // data = null;
    // }
    //
    // /**
    // * Build a dataObject filled with data
    // *
    // * @param g Object group
    // * @param v Object variation
    // * @param someBytes Object data
    // *
    // * @throws IllegalArgumentException
    // * <ul>
    // * <li> object with this group and variation attributes is not known
    // * </ul>
    // */
    // public DataObject(byte g, byte v, byte[] someBytes) {
    // if (!isValid(g, v)) {
    // throw new IllegalArgumentException("Unknown Data Object");
    // }
    //
    // if (((length(g, v) + 7) / 8) != someBytes.length) {
    // throw new IllegalArgumentException(
    // "Frame length (" + someBytes.length
    // + ") doesn't match with this DataObject (" + g + ", " + v + ")");
    // }
    //
    // group = g;
    // variation = v;
    // data = someBytes;
    // }
    //
    // //=============================================================================
    // // Methods
    // //=============================================================================
    //
    // /**
    // * DataObject length.
    // * This method is called when group and length are coded in decimal format
    // *
    // * @param g Object group
    // * @param v Object variation
    // *
    // * @return object size, <tt>0</tt> if there's no object coding,
    // * <tt>-1</tt> if this object is not supported
    // */
    // public static byte length(byte g, byte v) {
    // return fixedLength(Utils.decimal2Hexa(g), v);
    // }
    //
    // /**
    // * DataObject length.
    // * This method is called when group and length are coded in hexa format
    // *
    // * @param g Object group
    // * @param v Object variation
    // *
    // * @return object size, 0 if there's no object coding,
    // * -1 if this object is not supported
    // */
    // public static byte fixedLength(byte g, byte v) {
    // int first4bits = (int) ((g >> 4) & 0x0F);
    // int last4bits = (int) (g & 0x0F);
    // byte res;
    //
    // try {
    // res = size[first4bits][last4bits][v - 1];
    // } catch (Exception e) {
    // res = -1;
    // }
    //
    // return res;
    // }
    //
    // /**
    // * Verify if this object is include in DNP Object Table
    // *
    // * @param g Object group
    // * @param v Object variation
    // *
    // * @return object is include in DNP Object Table
    // */
    // public static boolean isValid(byte g, byte v) {
    // return (length(g, v) != -1);
    // }
    //
    // /**
    // * Verify if this object is include in DNP Object Table
    // * and if group matches with the type of the data
    // *
    // * @param g Object group
    // * @param v Object variation
    // *
    // * @return object is include in DNP Object Table
    // * and group matches with the type of the data
    // */
    // public static boolean isValid(byte type, byte g, byte v) {
    // return ((type == (Utils.decimal2Hexa(g) & 0xF0)) && (isValid(g, v)));
    // }
    //
    // /**
    // * Display this Data Object
    // *
    // * @return String representation of Data Object
    // */
    // public String output() {
    // return ("G : " + group + ", V : " + variation
    // + ((data != null) ? (", data : " + Utils.Display(data)) : ""));
    // }
    //
    // //////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////
    //
    // /**
    // * Get an ObjectType (BIN_IN, BIN_OUT, COUNTER, ANA_IN, ..)
    // * from a group attribute
    // *
    // * @param group group attribute
    // *
    // * @return an object type
    // */
    // public static byte getObjectType(byte group) {
    // return (byte) (Utils.decimal2Hexa(group) & 0xF0);
    // }
    //
    // /**
    // * Display Data Objects, useful for trace Get ot Set functions
    // *
    // * @param someDataObjects data objects to display
    // *
    // * @return String representation of these objects
    // */
    // public static void displayDataObjects(DataObject[] someDataObjects) {
    // String s = new String();
    //
    // for (int i = 0; i < someDataObjects.length; i++) {
    // if (someDataObjects[i].data != null) {
    // s += Utils.Display(someDataObjects[i].data);
    // } else {
    // s += "null\n";
    // }
    // }
    //
    // if (DEBUG) {
    // System.out.println(s);
    // }
    // }
    //
    // /**
    // * <p>
    // * Format a Binary input record in a DNP3 Data Object formatted value
    // *
    // * Group & variation supported :
    // * <ul>
    // * <li> group 1, var 1 -> 1 byte with state value (0x80)
    // * <li> group 1, var 2 -> 1 byte with state value (0x80) & status (0x3F,
    // 0x01 is on-line)
    // * <li> group 2, var 1 -> 1 byte with state value (0x80) & status (0x3F,
    // 0x01 is on-line)
    // * <li> group 2, var 2 -> 1 byte with state value (0x80) & status (0x3F,
    // 0x01 is on-line)
    // * <li> 6 bytes (time of occurence)
    // * </ul>
    // *
    // * @param group Data Object group
    // * @param variation Data Object variation
    // * @param rec a Record
    // * @param inverted logic to apply before formatting
    // *
    // * @return DNP3 Data Object value
    // */
    // public static byte[] binaryInputFormat(
    // byte group, byte variation, Record rec, boolean inverted) {
    // boolean state = rec.booleanValue();
    // state ^= inverted; // modify state value with inverted logic
    //
    // byte[] result = new byte[(DataObject.length(group, variation) + 7) / 8];
    //
    // // value
    // result[0] = (byte) ((state) ? (byte) 0x80 : (byte) 0x00);
    //
    // // flag
    // if ((group != 1) || (variation != 1)) {
    // result[0] |= getFlag(rec);
    // }
    //
    // // time of occurence
    // if ((group == 2) && (variation == 2)) {
    // System.arraycopy(getTime(rec), 0, result, 1, 6);
    // }
    //
    // return result;
    // }
    //
    // /**
    // * <p>
    // * Format a Binary Output record in a DNP3 Data Object formatted value
    // *
    // * Group & variation supported :
    // * <ul>
    // * <li> group 10, var 1 -> 1 byte with state value (0x80)
    // * <li> group 10, var 2 -> 1 byte with state value (0x80) & status (0x3F,
    // 0x01 is on-line)
    // * <li> group 12, var 1 -> 11 bytes with
    // * <ul>
    // * <li> control code : 1 byte (LATCH_ON, LATCH_OFF, PULSE_ON_CLOSE,
    // PULSE_ON_TRIP)
    // * <li> count : 1 byte (0x01)
    // * <li> on time : 4 bytes (rec.timeStamp)
    // * <li> off time : 4 bytes (0)
    // * <li> status : 1 byte (0)
    // * </ul>
    // * </ul>
    // *
    // * @param group Data Object group
    // * @param variation Data Object variation
    // * @param rec a Record
    // * @param inverted logic to apply before formatting
    // * @param onTime time to include in the Data Object
    // *
    // * @return DNP3 Data Object value
    // */
    // public static byte[] binaryOutputFormat(
    // byte group, byte variation, Record rec, boolean inverted, long onTime) {
    // boolean state = rec.booleanValue(); // value
    // state ^= inverted; // modify state value with inverted logic
    //
    // byte[] result = new byte[(DataObject.length(group, variation) + 7) / 8];
    //
    // if (group == 10) {
    // // value & flag
    // result[0] =
    // (byte) (((state) ? (byte) 0x80 : (byte) 0x00)
    // | ((variation == 2) ? getFlag(rec) : (byte) 0x00));
    // } else if (group == 12) {
    // // count
    // result[1] = 1;
    //
    // if (onTime > 0) {
    // // control code
    // result[0] = ((state) ? PULSE_ON_CLOSE : PULSE_ON_TRIP);
    //
    // // on time
    // result[2] = (byte) (onTime & 0xFF);
    // result[3] = (byte) ((onTime >> 8) & 0xFF);
    // result[4] = (byte) ((onTime >> 16) & 0xFF);
    // result[5] = (byte) ((onTime >> 24) & 0xFF);
    // } else {
    // // control code
    // result[0] = ((state) ? LATCH_ON : LATCH_OFF);
    // }
    // }
    //
    // return result;
    // }
    //
    // /**
    // * <p>
    // * Format a Counter or an Analog Input record in a DNP3 Data Object
    // formatted value
    // *
    // * Group & variation supported :
    // * <ul>
    // * <li> group 20-30, var 1 -> 5 bytes (8 bits flag + 32 bits value)
    // * <li> group 20-30, var 2 -> 3 bytes (8 bits flag + 16 bits value)
    // * <li> group 20-30, var 3 -> 4 bytes (32 bits value)
    // * <li> group 20-30, var 4 -> 2 bytes (16 bits value)
    // * <li> group 22-32, var 1 -> 5 bytes (8 bits flag + 32 bits value)
    // * <li> group 22-32, var 2 -> 3 bytes (8 bits flag + 16 bits value)
    // * <li> group 22-32, var 3 -> 11 bytes (8 bits flag + 32 bits value + 6
    // bytes (time of occurence))
    // * <li> group 22-32, var 4 -> 9 bytes (8 bits flag + 16 bits value + 6
    // bytes (time of occurence))
    // * </ul>
    // *
    // * @param group Data Object group
    // * @param variation Data Object variation
    // * @param rec a Record
    // * @param scale scale to apply before formatting
    // * @param offset offset to apply before formatting
    // *
    // * @return DNP3 Data Object value
    // */
    // public static byte[] analogInputFormat(
    // byte group, byte variation, Record rec, int scale, int offset) {
    // int state = (int) (rec.floatValue() * scale) + offset;
    // byte[] result = new byte[(DataObject.length(group, variation) + 7) / 8];
    // int i = 0;
    //
    // // flag
    // if ((variation < 3) || ((group % 10) == 2)) {
    // result[i++] = getFlag(rec);
    // }
    //
    // // value
    // result[i++] = (byte) (state & 0xFF);
    // result[i++] = (byte) ((state >> 8) & 0xFF);
    //
    // // value 32 bits
    // if ((variation % 2) == 1) {
    // result[i++] = (byte) ((state >> 16) & 0xFF);
    // result[i++] = (byte) ((state >> 24) & 0xFF);
    // }
    //
    // // time of occurence
    // if ((variation > 2) && ((group % 10) == 2)) {
    // System.arraycopy(getTime(rec), 0, result, i, 6);
    // }
    //
    // return result;
    // }
    //
    // /**
    // * <p>
    // * Format an Analog Output record in a DNP3 Data Object formatted value
    // *
    // * group & variation supported :
    // * <ul>
    // * <li> group 40, var 1 -> 5 bytes (8 bits flag + 32 bits value)
    // * <li> group 40, var 2 -> 3 bytes (8 bits flag + 16 bits value)
    // * <li> group 41, var 1 -> 5 bytes (32 bits value + 8 bits status)
    // * <li> group 41, var 2 -> 3 bytes (16 bits value + 8 bits status)
    // * </ul>
    // *
    // * @param group Data Object group
    // * @param variation Data Object variation
    // * @param rec a Record
    // * @param scale scale to apply before formatting
    // * @param offset offset to apply before formatting
    // *
    // * @return DNP3 Data Object value
    // */
    // public static byte[] analogOutputFormat(
    // byte group, byte variation, Record rec, int scale, int offset) {
    // int state = (int) (rec.floatValue() * scale) + offset;
    // byte[] result = new byte[(DataObject.length(group, variation) + 7) / 8];
    // int i = 0;
    //
    // // flag
    // if (group == 40) {
    // result[i++] = getFlag(rec);
    // }
    //
    // // value
    // result[i++] = (byte) (state & 0xFF);
    // result[i++] = (byte) ((state >> 8) & 0xFF);
    //
    // // value 32 bits
    // if (variation == 1) {
    // result[i++] = (byte) ((state >> 16) & 0xFF);
    // result[i++] = (byte) ((state >> 24) & 0xFF);
    // }
    //
    // return result;
    // }
    //
    // /**
    // * <p>
    // * Format an Time record in a DNP3 Data Object formatted value
    // *
    // * Group & variation supported :
    // * <ul>
    // * <li> group 50, var 1 -> 6 bytes (time stamp)
    // * </ul>
    // * @param group Data Object group
    // * @param variation Data Object variation
    // * @param scale scale to apply before formatting
    // * @param offset offset to apply before formatting
    // *
    // * @return DNP3 Data Object value
    // */
    // public static byte[] timeFormat(
    // byte group, byte variation, Record rec, int scale, int offset) {
    // byte[] result = new byte[6];
    // long time = (long) (rec.floatValue() * scale) + offset;
    //
    // for (int i = 0; i < result.length; i++) {
    // result[i] = (byte) ((time >> (8 * i)) & 0xFF);
    // }
    //
    // return result;
    // }
    //
    // //////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////
    //
    // /**
    // * Extract an Integer value from an array of bytes,
    // * formatted with group and variation attributes.
    // * Suits for Counters and Analogs
    // *
    // * @param group Data Object group
    // * @param variation Data Object variation
    // * @param data data to extract
    // * @param scale scale to apply after unformatting
    // * @param offset offset to apply after unformatting
    // *
    // * @return extracted value
    // */
    // public static Float unformatFloat(
    // byte group, byte variation, byte[] data, int scale, int offset) {
    // // value
    // long state;
    //
    // if (group == TIME) {
    // state = setTime(data);
    // } else {
    // int i = 0;
    //
    // if (variation < 3) {
    // i++;
    // }
    //
    // state = ((data[i++] & 0xFF) | ((data[i++] << 8) & 0xFF00));
    //
    // if ((variation % 2) == 1) {
    // state |= (((data[i++] << 16) & 0xFF0000)
    // | ((data[i++] << 24) & 0xFF000000));
    // }
    // }
    //
    // // apply scale & offset to result
    // float result = (state - offset) / scale;
    //
    // return new Float(result);
    // }
    //
    // /**
    // * Extract a Boolean value from an array of bytes,
    // * formatted with group and variation attributes
    // * Suits for Binary Input & Output
    // *
    // * @param group Data Object group
    // * @param variation Data Object variation
    // * @param data data to extract
    // * @param inverted logic to apply after unformatting
    // *
    // * @return extracted value
    // */
    // public static Boolean unformatBool(
    // byte group, byte variation, byte[] data, boolean inverted) {
    // boolean result;
    //
    // if (group == 12) {
    // result = ((data[0] == LATCH_ON) || (data[0] == PULSE_ON_CLOSE));
    // } else {
    // result = ((data[0] & 0x80) != 0);
    // }
    //
    // // apply scale & offset to result
    // result ^= inverted;
    //
    // return new Boolean(result);
    // }
    //
    // //////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////
    //
    // /**
    // * Get Flag attributes from a record
    // * This flag is joined to dataObject following variation attribute
    // * ex : Binary Input with status.
    // *
    // * @param rec a record
    // *
    // * @return DNP formatted flag attribute
    // */
    // public static byte getFlag(Record rec) {
    // byte flag =
    // ((rec.quality & Record.Q_INVALID) == 0) ? (byte) 0x01 : (byte) 0x00;
    // flag |= (((rec.quality & Record.Q_COMM_FAIL) != 0) ? (byte) 0x04
    // : (byte) 0x00);
    // flag |= (((rec.quality & Record.Q_FORCED) != 0) ? (byte) 0x10 : (byte)
    // 0x00);
    // flag |= (((rec.quality & Record.Q_OVER_RANGE) != 0) ? (byte) 0x20
    // : (byte) 0x00);
    //
    // return flag;
    // }
    //
    // /**
    // * Set quality attribute of a record following flag bits indications
    // *
    // * @param rec a record
    // * @param flag DNP formatted flag attribute
    // *
    // * @return record quality updated
    // */
    // public static short setFlag(Record rec, byte flag) {
    // short quality = rec.quality;
    //
    // // on-Line bit
    // quality |= Record.Q_INVALID;
    //
    // if ((flag & 0x01) != 0) {
    // quality -= Record.Q_INVALID;
    // }
    //
    // // Communication Lost bit
    // quality |= Record.Q_COMM_FAIL;
    //
    // if ((flag & 0x04) == 0) {
    // quality -= Record.Q_COMM_FAIL;
    // }
    //
    // // Forced Data bit
    // quality |= Record.Q_FORCED;
    //
    // if ((flag & 0x10) == 0) {
    // quality -= Record.Q_FORCED;
    // }
    //
    // // Over Range bit
    // quality |= Record.Q_OVER_RANGE;
    //
    // if ((flag & 0x20) == 0) {
    // quality -= Record.Q_OVER_RANGE;
    // }
    //
    // return quality;
    // }
    //
    // /**
    // * Set quality attribute of a record with onLine attribute
    // *
    // * @param rec a record
    // *
    // * @return record quality updated with onLine attribute
    // */
    // public static short setFlag(Record rec) {
    // return (short) ((rec.quality | Record.Q_INVALID) - Record.Q_INVALID);
    // }
    //
    // //////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////
    //
    // /**
    // * Update systemTime with a 6 dnp3-formatted bytes
    // *
    // * @param someBytes DNP formatted time value
    // */
    // public static void setSystemTime(byte[] someBytes) {
    // long time = setTime(someBytes);
    //
    // // AJile feature !!!
    // Configuration.setBaseTime(time);
    // }
    //
    // /**
    // * Get time from a 6 dnp3-formatted bytes
    // *
    // * @param someBytes DNP formatted time value
    // *
    // * @return time extracted
    // */
    // public static long setTime(byte[] someBytes) {
    // long time = 0;
    //
    // for (int i = 0; i < someBytes.length; i++) {
    // time |= (someBytes[i] << (8 * i));
    // }
    //
    // return time;
    // }
    //
    // /**
    // * Format a TimeStamp to a 6 dnp3-formatted bytes
    // *
    // * @param rec a Record with TimeStamp
    // *
    // * @return DNP formatted time value
    // */
    // public static byte[] getTime(Record rec) {
    // byte[] result = new byte[6];
    // long time = rec.timeStamp;
    //
    // for (int i = 0; i < result.length; i++) {
    // result[i] = (byte) ((time >> (8 * i)) & 0xFF);
    // }
    //
    // return result;
    // }
    //
    // /**
    // * Format CurrentTime to a 6 dnp3-formatted bytes
    // *
    // * @return DNP formatted time value
    // */
    // public static byte[] getTime() {
    // byte[] result = new byte[6];
    // long time = System.currentTimeMillis();
    //
    // for (int i = 0; i < result.length; i++) {
    // result[i] = (byte) ((time >> (8 * i)) & 0xFF);
    // }
    //
    // return result;
    // }
    //
    // /**
    // * get time Delay from static parameters
    // */
    // public static byte[] getTimeDelay(long delay, byte variation) {
    // byte[] result = new byte[2];
    //
    // if (variation == COARSE) {
    // delay = (delay / 1000);
    // }
    //
    // for (int i = 0; i < result.length; i++) {
    // result[i] = (byte) ((delay >> (8 * i)) & 0xFF);
    // }
    //
    // return result;
    // }
}
