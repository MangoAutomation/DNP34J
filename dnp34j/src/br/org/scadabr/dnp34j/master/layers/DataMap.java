/*
 * Data Map class
 *
 * Class com.itlity.protocol.master.DataMap File DataMap.java Author Alexis CLERC
 * <alexis.clerc@sysaware.com> (c) SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.layers;

import br.org.scadabr.dnp34j.master.common.DataLengths;
import br.org.scadabr.dnp34j.master.common.DataMapFeatures;
import br.org.scadabr.dnp34j.master.common.DataObject;
import br.org.scadabr.dnp34j.master.common.InitFeatures;
import br.org.scadabr.dnp34j.master.common.utils.Buffer;
import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.database.DataElement;

/**
 * <p>
 * This class is an interface between database and DNP Master application
 *
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class DataMap implements DataMapFeatures, InitFeatures {
    static final boolean DEBUG = !DATABASE_QUIET;

    private DNPUser user;

    /**
     * Constructor. Build & Initialize a DataMap
     */
    public DataMap(DNPUser user) {
        this.user = user;
    }

    /**
     * Set objects of a group/variation. Return a copy of data joined in the request Range :
     * [index(values[0]), index(values[1]), ... ,index(values[values.length-1])]
     *
     * @param group Object group
     * @param variation Object variation
     * @param values indexes of updated objects
     * @param newDataObjects updated objects to submit
     * @param qualifier either INDEXES_8 or INDEXES_16 quelifier
     *
     * @return a range of Data Objects
     */
    public byte[] set(byte group, byte variation, int[] values, DataObject[] newDataObjects,
            byte qualifier) {
        Buffer getData = new Buffer(S);

        for (int i = 0; i < values.length; i++) {
            if ((qualifier & 0xF0) == 0x10) {
                getData.writeByte((byte) values[i]);
            } else {
                getData.writeBytes(values[i]);
            }

            getData.writeBytes(newDataObjects[i].data);
            setDB(values[i], newDataObjects[i].data, group, variation);
        }

        return getData.readBytes();
    }

    /**
     * Set objects of a group/variation. Return a copy of data joined in the request Range :
     * [index[start], index[start+1], ... ,index[stop]]
     *
     * @param group Object group
     * @param variation Object variation
     * @param start range start
     * @param stop range stop
     * @param newDataObjects updated objects to submit
     *
     * @return a range of Data Objects
     */
    public void set(byte group, byte variation, int start, int stop, byte[] newDataObjects) {
        if (DataLengths.getDataLength(group, variation) < 0) {
            return;
        }

        //This is
        if (DataLengths.isBitString(group, variation)) {
            setBits(group, variation, start, stop, newDataObjects);
        } else {
            setBytes(group, variation, start, stop, newDataObjects);
        }
    }


    /**
     *
     * For example usage see IEEE Std 1815-2012 Object group1: binary inputs Variation 1
     *
     * Set bits objects of a group/variation. Return a copy of data joined in the request Range :
     * [index[start], index[start+1], ... ,index[stop]]
     *
     * @param group Object group
     * @param variation Object variation
     * @param start range start
     * @param stop range stop
     * @param newDataObjects updated objects to submit
     *
     * @return a range of Data Objects
     */
    private void setBits(byte group, byte variation, int start, int stop, byte[] newDataObjects) {

        //Find start bit (possibly larger than 1 byte)
        int bitLocation = stop - start;
        if(bitLocation > 7) {
            bitLocation = 7;
        }

        // for each byte
        for (int i = 0; i < newDataObjects.length; i++) {
            // for each used bit in this byte
            while(bitLocation >= 0) {
                // index
                int index = start + (i * 8) + bitLocation;

                if (stop < index) {
                    break;
                }
                byte[] newDO = new byte[1];
                newDO[0] = (byte) ((newDataObjects[i] >> bitLocation) & 0x01);
                setDB(index, newDO, group, variation);
                bitLocation--;
            }
        }
    }

    /**
     * Set bytes objects of a group/variation. Return a copy of data joined in the request Range :
     * [index[start], index[start+1], ... ,index[stop]]
     *
     * @param group Object group
     * @param variation Object variation
     * @param start range start
     * @param stop range stop
     * @param newDataObjects updated objects to submit
     *
     * @return a range of Data Objects
     */
    private void setBytes(byte group, byte variation, int start, int stop, byte[] newDataObjects) {
        int length = (DataLengths.getDataLength(group, variation) + 7) / 8;

        byte[] newDO = new byte[length];

        for (int i = start; i < (stop + 1); i++) {
            try {
                System.arraycopy(newDataObjects, (i - start) * length, newDO, 0, length);
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                continue;
            }
            setDB(i, newDO, group, variation);
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Update database
     * <ul>
     * <li>set new value (Boolean for Binary Input & Output, Integer for Counters, Analog Input &
     * Output)
     * <li>set quality
     * </ul>
     *
     * @param index index point
     * @param data updated value
     * @param group object group
     * @param variation object variation
     */
    private void setDB(int index, byte[] data, byte group, byte variation) {
        DataElement rec = new DataElement();
        rec.setIndex(index);
        rec.setGroup(DataObject.getObjectType(group));
        rec.setTimestamp(System.currentTimeMillis());
        rec.setSpecificGroup(group);
        rec.setVariation(variation);

        //Set Quality if supported
        if (rec.supportsQuality()) {
            rec.setQuality(data[0]);
        }

        //Extract the value
        switch(group) {
            case BINARY_INPUT_STATIC:
                switch(variation) {
                    case 1:
                        rec.setValue(new Boolean(((data[0] & 0b00000001) != 0)));
                        break;
                    case 2:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        break;
                }
                break;
            case BINARY_INPUT_EVENT:
                switch(variation) {
                    case 1:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        break;
                    case 2:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        //Has time
                        rec.setTimestamp(DataObject.toTime(data, 1));
                        break;
                    case 3:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        //Common time of occurrence not supported yet so not extracting relative 2 bytes of ms
                        break;
                    default:
                        break;
                }
                break;
            case BINARY_OUTPUT_STATIC:
                switch(variation) {
                    case 1:
                        rec.setValue(new Boolean(((data[0] & 0b00000001) != 0)));
                        break;
                    case 2:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        break;
                }
                break;
            case BINARY_OUTPUT_EVENT:
                switch(variation) {
                    case 1:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        break;
                    case 2:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        //Has time
                        rec.setTimestamp(DataObject.toTime(data, 1));
                        break;
                    default:
                        break;
                }
                break;
            case BINARY_OUTPUT_COMMAND:
                switch(variation) {
                    case 1:
                    case 2:
                        rec.setControlStatus(data[10]);
                        break;
                }
                break;
            case COUNTER_STATIC:
                //Counters
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 4:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 5:
                        //32 bits
                        rec.setValue(new Integer(DataObject.toInt(data, 0)));
                        break;
                    case 6:
                        //16 bits
                        rec.setValue(new Short(DataObject.toShort(data, 0)));
                        break;
                    case 7:
                        //32 bits
                        rec.setValue(new Integer(DataObject.toInt(data, 0)));
                        break;
                    case 8:
                        //16 bits
                        rec.setValue(new Short(DataObject.toShort(data, 0)));
                        break;
                    default:
                        break;
                }
                break;
            case FROZEN_COUNTER:
                //Frozen Counters
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 4:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 5:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 6:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 7:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 8:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 9:
                        //32 bits
                        rec.setValue(new Integer(DataObject.toInt(data, 0)));
                        break;
                    case 10:
                        //16 bits
                        rec.setValue(new Short(DataObject.toShort(data, 0)));
                        break;
                    case 11:
                        //32 bits
                        rec.setValue(new Integer(DataObject.toInt(data, 0)));
                        break;
                    case 12:
                        //16 bits
                        rec.setValue(new Short(DataObject.toShort(data, 0)));
                        break;
                    default:
                        break;
                }
                break;
            case COUNTER_EVENT:
                //Counter events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 4:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 5:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 6:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 7:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 8:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    default:
                        break;
                }
                break;
            case FROZEN_COUNTER_EVENT:
                //Frozen Counter events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 4:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 5:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 6:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 7:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 8:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    default:
                        break;
                }
                break;
            case ANALOG_INPUT_STATIC:
                //Analog Inputs
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits
                        rec.setValue(new Integer(DataObject.toInt(data, 0)));
                        break;
                    case 4:
                        //16 bits
                        rec.setValue(new Short(DataObject.toShort(data, 0)));
                        break;
                    case 5:
                        //32 bit floating point w/ flag
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        break;
                    case 6:
                        //64 bit floating point w/ flag
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        break;
                    default:
                        break;
                }
                break;
            case FROZEN_ANALOG_INPUT:
                //Frozen analog Inputs
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 4:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 5:
                        //32 bits
                        rec.setValue(new Integer(DataObject.toInt(data, 0)));
                        break;
                    case 6:
                        //16 bits
                        rec.setValue(new Short(DataObject.toShort(data, 0)));
                        break;
                    case 7:
                        //32 bit floating point w/ flag
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        break;
                    case 8:
                        //64 bit floating point w/ flag
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        break;
                    default:
                        break;
                }
                break;
            case ANALOG_INPUT_EVENT:
                //Analog Input Events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 4:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 5:
                        //32 bit floating point w/ flag
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        break;
                    case 6:
                        //64 bit floating point w/ flag
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        break;
                    case 7:
                        //32 bit floating point w/ flag and time
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 8:
                        //64 bit floating point w/ flag and time
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 9));
                        break;
                    default:
                        break;
                }
                break;
            case FROZEN_ANALOG_INPUT_EVENT:
                //Frozen Analog Input Events
                //Analog Input Events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 4:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 5:
                        //32 bit floating point w/ flag
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        break;
                    case 6:
                        //64 bit floating point w/ flag
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        break;
                    case 7:
                        //32 bit floating point w/ flag and time
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 8:
                        //64 bit floating point w/ flag and time
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 9));
                        break;
                    default:
                        break;
                }
                break;
            case ANALOG_INPUT_REPORTING_DEADBAND:
                //Analog Input Reporting Deadband
                switch(variation) {
                    case 1:
                        //16 bits
                        rec.setValue(new Short(DataObject.toShort(data, 0)));
                        break;
                    case 2:
                        //32 bits
                        rec.setValue(new Integer(DataObject.toInt(data, 0)));
                        break;
                    case 3:
                        //32 bit floating point
                        rec.setValue(new Float(DataObject.toFloat(data, 0)));
                        break;
                    default:
                        break;
                }
                break;
            case ANALOG_OUTPUT_STATIC:
                //Analog output status
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bit floating point w/ flag
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        break;
                    case 4:
                        //64 bit floating point w/ flag
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        break;
                    default:
                        break;
                }
                break;
            case ANALOG_OUTPUT_EVENTS:
                //Analog output events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        break;
                    case 2:
                        //16 bits w/ flag
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        break;
                    case 3:
                        //32 bits w/ flag and time
                        rec.setValue(new Integer(DataObject.toInt(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 4:
                        //16 bits w/ flag and time
                        rec.setValue(new Short(DataObject.toShort(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 3));
                        break;
                    case 5:
                        //32 bit floating point w/ flag
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        break;
                    case 6:
                        //64 bit floating point w/ flag
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        break;
                    case 7:
                        //32 bit floating point w/ flag and time
                        rec.setValue(new Float(DataObject.toFloat(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 5));
                        break;
                    case 8:
                        //64 bit floating point w/ flag
                        rec.setValue(new Double(DataObject.toDouble(data, 1)));
                        rec.setTimestamp(DataObject.toTime(data, 9));
                        break;
                    default:
                        break;
                }
            case ANALOG_OUTPUT_COMMAND:
                switch(variation) {
                    case 1:
                        rec.setControlStatus(data[4]);
                        break;
                    case 2:
                        rec.setControlStatus(data[2]);
                        break;
                    case 3:
                        rec.setControlStatus(data[4]);
                        break;
                    case 4:
                        rec.setControlStatus(data[8]);
                        break;
                }
                break;
            default:break;
        }

        if (user.getDatabase() != null)
            user.getDatabase().writeRecord(rec);

        if (DEBUG) {
            System.out.println("[DataMap " + this + "] Set : (G,V,I, value) " + group
                    + " variation:" + variation + " index: " + index);
        }
    }

    public void setUser(DNPUser user) {
        this.user = user;
    }

    public DNPUser getUser() {
        return user;
    }
}
