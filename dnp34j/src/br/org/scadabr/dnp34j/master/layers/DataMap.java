/*
 * Data Map class
 *
 * Class com.itlity.protocol.master.DataMap File DataMap.java Author Alexis CLERC
 * <alexis.clerc@sysaware.com> (c) SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.layers;

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

    // public void set(byte group, byte variation, byte[] dataObjects) {
    // set(group, variation, getIndexMin(group), getIndexMax(group),
    // dataObjects);
    // }

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
        if (DataObject.length(group, variation) < 0) {
            return;
        }

        if (DataObject.length(group, variation) == 1) {
            setBits(group, variation, start, stop, newDataObjects);
        } else {
            setBytes(group, variation, start, stop, newDataObjects);
        }
    }

    /**
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
        byte[] newDO = new byte[1];

        // for each byte
        for (int i = 0; i < newDataObjects.length; i++) {
            // for each bit in this byte
            for (int j = 0; j < 8; j++) {
                // index
                int index = start + (i * 8) + j;

                if (stop < index) {
                    break;
                }

                newDO[0] = (byte) ((newDataObjects[i] << (7 - j)) & 0x80);
                setDB(index, newDO, group, variation);
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
        int length = (DataObject.length(group, variation) + 7) / 8;

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
        // set value & Q_INVALID quality
        switch (DataObject.getObjectType(group)) {
            case BIN_IN: {
                rec.setValue(DataObject.unformatBool(group, variation, data, (false)).toString());

                if (variation == 2) {
                    rec.setQuality(data[0]);
                } else {
                    // rec.quality = DataObject.setFlag(rec);
                }

                if ((group == 2) && (variation == 2)) // Binary Input Change with
                    // Time
                {
                    byte[] time = new byte[6];
                    System.arraycopy(data, 1, time, 0, 6);
                    rec.setTimestamp(DataObject.setTime(time));
                }
            }

            break;

            case BIN_OUT: {
                rec.setValue(DataObject.unformatBool(group, variation, data, (false)).toString());

                if (variation == 2) {
                    rec.setQuality(data[0]);
                } else {
                    // rec.quality = DataObject.setFlag(rec);
                }
            }

            break;

            case COUNTER: {
                // rec.setValue(DataObject.unformatFloat(group, variation, data,
                // element.getScale(), element.getOffset()));
                // rec.quality = DataObject.setFlag(rec);
            }

            break;

            case ANA_IN: {
                if (variation < 3) {
                    rec.setQuality(data[0]);
                    rec.setValue("" + DataObject.unformatFloat(group, variation, data, 1, 0));
                } else if (variation == 7) {
                    // Floating point change with timestamp
                    rec.setQuality(data[0]);
                    rec.setValue("" + DataObject.unformatFloat(group, variation, data, 1, 0));
                    rec.setTimestamp(DataObject.toLong(data, 5, 6));
                } else if (variation == 3) {
                    // Analog change with timestamp
                    rec.setQuality(data[0]);
                    rec.setValue("" + DataObject.unformatFloat(group, variation, data, 1, 0));
                    rec.setTimestamp(DataObject.toLong(data, 5, 6));
                } else if (variation == 5) {
                    // Analog input with flag
                    rec.setQuality(data[0]);
                    rec.setValue("" + DataObject.unformatFloat(group, variation, data, 1, 0));
                } else {
                    rec.setValue("" + DataObject.unformatFloat(group, variation, data, 1, 0));
                    // rec.quality = DataObject.setFlag(rec);
                }
            }

            break;

            case ANA_OUT: {
                rec.setValue("" + DataObject.unformatFloat(group, variation, data, 1, 0));

                if (group == 40) {
                    rec.setQuality(data[0]);
                } else {
                    // rec.quality = DataObject.setFlag(rec);
                }
            }

            break;

            case TIME: {
                // rec.setValue(DataObject.unformatFloat(group, variation, data,
                // element.getScale(), element.getOffset()));
                // rec.quality = DataObject.setFlag(rec);
            }
            default:
                // ignore
        }
        if (user.getDatabase() != null)
            user.getDatabase().writeRecord(rec);
        // elem.writeNewRecord(rec);
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
