/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package br.org.scadabr.dnp34j.master.layers;

import br.org.scadabr.dnp34j.master.common.DataObject;
import br.org.scadabr.dnp34j.master.session.database.DataElement;

/**
 *
 * @author Terry Packer
 */
public class TestBitString {

    public static void main(String[] args) throws Exception {

        //Test Group 1 Var 1 with a single byte
        System.out.println("Group 1 Var 1: 1 Byte");
        byte[] newDataObjects = new byte[1];
        newDataObjects[0] = (byte)0b01010101;
        setBits((byte)1, (byte)1, 0, 7, newDataObjects);


        System.out.println("Group 1 Var 1: 2 Bytes");
        //Test Group 1 Var 1 with a 2 bytes
        newDataObjects = new byte[2];
        newDataObjects[0] = (byte)0b11111111;
        newDataObjects[1] = (byte)0b10101010;
        setBits((byte)1, (byte)1, 0, 15, newDataObjects);


    }


    private static void setBits(byte group, byte variation, int start, int stop, byte[] newDataObjects) {
        byte[] newDO = new byte[1];

        if(stop - start > newDataObjects.length * 8) {
            throw new RuntimeException("Invalid data format for bit string");
        }

        //TODO I think this code is working in reverse...
        //See Section A.2.1 or Section A6.1 for packed bit string formats
        //Scale to be bit in highest byte
        int index = start;
        int bitLocation = stop - newDataObjects.length * 8;
        int byteNumber = 0;
        while(index <= stop) {
            newDO[0] = (byte) ((newDataObjects[byteNumber] >> bitLocation) & 0x01);
            setDB(index, newDO, group, variation);
            index++;
            bitLocation++;
            if(bitLocation > 7) {
                byteNumber++;
                bitLocation = 0;
            }
        }
    }

    private static void setDB(int index, byte[] data, byte group, byte variation) {
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
            case DataMap.BINARY_INPUT_STATIC:
                switch(variation) {
                    case 1:
                        rec.setValue(new Boolean(((data[0] & 0b00000001) != 0)));
                        break;
                    case 2:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        break;
                }
                break;
            case DataMap.BINARY_OUTPUT_STATIC:
                switch(variation) {
                    case 1:
                        rec.setValue(new Boolean(((data[0] & 0b00000001) != 0)));
                        break;
                    case 2:
                        rec.setValue(new Boolean(((data[0] & 0b10000000) != 0)));
                        break;
                }
                break;

        }

        System.out.println(rec);
    }


}
