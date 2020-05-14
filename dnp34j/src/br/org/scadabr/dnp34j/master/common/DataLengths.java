/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package br.org.scadabr.dnp34j.master.common;

/**
 * A more verbose class that supplies data lengths and as a bonus is understandable.
 *
 * @author Terry Packer
 */
public class DataLengths implements DataMapFeatures {

    /**
     * Get the expected data length in bits for a given group and variation of data types
     *
     * @param group
     * @param variation
     * @return length in bits or -1 if not known/supported
     */
    public static int getDataLength(byte group, byte variation) {
        switch(group) {
            case BINARY_INPUT_STATIC:
                switch(variation) {
                    case 1:
                        return 1;
                    case 2:
                        //Bit with Flag
                        return 8;
                }
                break;
            case BINARY_INPUT_EVENT:
                switch(variation) {
                    case 1:
                        //Bit with flag
                        return 8;
                    case 2:
                        //Bit with flag and time
                        return 56;
                    case 3:
                        //Bit with flag and relative time
                        return 24;
                    default:
                        break;
                }
                break;
            case BINARY_OUTPUT_STATIC:
                switch(variation) {
                    case 1:
                        return 1;
                    case 2:
                        //Bit with Flag
                        return 8;
                    default:
                        break;
                }
                break;
            case BINARY_OUTPUT_EVENT:
                switch(variation) {
                    case 1:
                        //Bit with flag
                        return 8;
                    case 2:
                        //Bit with flag and time
                        return 56;
                    default:
                        break;
                }
                break;
            case BINARY_OUTPUT_COMMAND:
                switch(variation) {
                    case 1:
                        return 88;
                    case 2:
                        return 88;
                    default:
                        break;
                }
                break;
            case COUNTER_STATIC:
                //Counters
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag
                        return 40;
                    case 4:
                        //16 bits w/ flag
                        return 24;
                    case 5:
                        //32 bits
                        return 32;
                    case 6:
                        //16 bits
                        return 16;
                    case 7:
                        //32 bits
                        return 32;
                    case 8:
                        //16 bits
                        return 16;
                    default:
                        break;
                }
                break;
            case FROZEN_COUNTER:
                //Frozen Counters
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag
                        return 40;
                    case 4:
                        //16 bits w/ flag
                        return 24;
                    case 5:
                        //32 bits w/ flag and time
                        return 88;
                    case 6:
                        //16 bits w/ flag and time
                        return 72;
                    case 7:
                        //32 bits w/ flag and time
                        return 88;
                    case 8:
                        //16 bits w/ flag and time
                        return 72;
                    case 9:
                        //32 bits
                        return 32;
                    case 10:
                        //16 bits
                        return 16;
                    case 11:
                        //32 bits
                        return 32;
                    case 12:
                        //16 bits
                        return 16;
                    default:
                        break;
                }
                break;
            case COUNTER_EVENT:
                //Counter events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag
                        return 40;
                    case 4:
                        //16 bits w/ flag
                        return 24;
                    case 5:
                        //32 bits w/ flag and time
                        return 88;
                    case 6:
                        //16 bits w/ flag and time
                        return 72;
                    case 7:
                        //32 bits w/ flag and time
                        return 88;
                    case 8:
                        //16 bits w/ flag and time
                        return 72;
                    default:
                        break;
                }
                break;
            case FROZEN_COUNTER_EVENT:
                //Frozen Counter events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag
                        return 32;
                    case 4:
                        //16 bits w/ flag
                        return 24;
                    case 5:
                        //32 bits w/ flag and time
                        return 88;
                    case 6:
                        //16 bits w/ flag and time
                        return 72;
                    case 7:
                        //32 bits w/ flag and time
                        return 88;
                    case 8:
                        //16 bits w/ flag and time
                        return 72;
                    default:
                        break;
                }
                break;
            case ANALOG_INPUT_STATIC:
                //Analog Inputs
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits
                        return 32;
                    case 4:
                        //16 bits
                        return 16;
                    case 5:
                        //32 bit floating point w/ flag
                        return 40;
                    case 6:
                        //64 bit floating point w/ flag
                        return 72;
                    default:
                        break;
                }
                break;
            case FROZEN_ANALOG_INPUT:
                //Frozen analog Inputs
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag and time
                        return 88;
                    case 4:
                        //16 bits w/ flag and time
                        return 72;
                    case 5:
                        //32 bits
                        return 32;
                    case 6:
                        //16 bits
                        return 16;
                    case 7:
                        //32 bit floating point w/ flag
                        return 40;
                    case 8:
                        //64 bit floating point w/ flag
                        return 72;
                    default:
                        break;
                }
                break;
            case ANALOG_INPUT_EVENT:
                //Analog Input Events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag and time
                        return 88;
                    case 4:
                        //16 bits w/ flag and time
                        return 72;
                    case 5:
                        //32 bit floating point w/ flag
                        return 40;
                    case 6:
                        //64 bit floating point w/ flag
                        return 72;
                    case 7:
                        //32 bit floating point w/ flag and time
                        return 88;
                    case 8:
                        //64 bit floating point w/ flag and time
                        return 112;
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
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag and time
                        return 88;
                    case 4:
                        //16 bits w/ flag and time
                        return 72;
                    case 5:
                        //32 bit floating point w/ flag
                        return 40;
                    case 6:
                        //64 bit floating point w/ flag
                        return 72;
                    case 7:
                        //32 bit floating point w/ flag and time
                        return 88;
                    case 8:
                        //64 bit floating point w/ flag and time
                        return 112;
                    default:
                        break;
                }
                break;
            case ANALOG_INPUT_REPORTING_DEADBAND:
                //Analog Input Reporting Deadband
                switch(variation) {
                    case 1:
                        //16 bits
                        return 16;
                    case 2:
                        //32 bits
                        return 32;
                    case 3:
                        //32 bit floating point
                        return 32;
                    default:
                        break;
                }
                break;
            case ANALOG_OUTPUT_STATIC:
                //Analog output status
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bit floating point w/ flag
                        return 40;
                    case 4:
                        //64 bit floating point w/ flag
                        return 72;
                    default:
                        break;
                }
                break;
            case ANALOG_OUTPUT_EVENTS:
                //Analog output events
                switch(variation) {
                    case 1:
                        //32 bits w/ flag
                        return 40;
                    case 2:
                        //16 bits w/ flag
                        return 24;
                    case 3:
                        //32 bits w/ flag and time
                        return 88;
                    case 4:
                        //16 bits w/ flag and time
                        return 72;
                    case 5:
                        //32 bit floating point w/ flag
                        return 40;
                    case 6:
                        //64 bit floating point w/ flag
                        return 72;
                    case 7:
                        //32 bit floating point w/ flag and time
                        return 88;
                    case 8:
                        //64 bit floating point w/ flag and time
                        return 112;
                    default:
                        break;
                }
                break;
            case ANALOG_OUTPUT_COMMAND:
                //Analog output events
                switch(variation) {
                    case 1:
                        //32 bit int with control status
                        return 40;
                    case 2:
                        //16 bit int with control status
                        return 24;
                    case 3:
                        //32 bit float with control status
                        return 40;
                    case 4:
                        //64 bit float with control status
                        return 72;
                }
                break;
            case TIME_STATIC:
                switch(variation) {
                    case 1:
                        return 48;
                    case 2:
                        return 80;
                    case 3:
                        return 48;
                    case 4:
                        return 88;
                    default:
                        break;
                }
                break;
            case TIME_OCCURANCE:
                switch(variation) {
                    case 1:
                        return 48;
                    case 2:
                        return 48;
                    default:
                        break;
                }
                break;
            case TIME_DELAY:
                switch(variation) {
                    case 1:
                        return 16;
                    case 2:
                        return 16;
                    default:
                        break;
                }
                break;
            case CLASS_STATIC:
                return 0;
            case IIN_STATIC:
                return 16;
            default:break;
        }
        return -1;
    }

    /**
     * Is this group/variation a packed bit string where each bit represents the state of an index
     * @param group
     * @param variation
     * @return
     */
    public static boolean isBitString(byte group, byte variation) {
        switch(group) {
            case BINARY_INPUT_STATIC:
                switch(variation) {
                    case 1:
                        return true;
                    default:
                        return false;
                }
            case BINARY_OUTPUT_STATIC:
                switch(variation) {
                    case 1:
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

}
