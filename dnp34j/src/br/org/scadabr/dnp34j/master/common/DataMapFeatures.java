/*
 * This interface contains DNP data-relative values
 *
 * Class    com.itlity.protocol.common.DataMapFeatures
 * File     DataMapFeatures.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common;


/**
 * <p>
 * DataMapFeatures defines
 * <ul>
 * <li> object types supported by this implementation
 * <li> qualifiers which may be parsed and / or generated
 * <li> group - varaitions attributes for each static, control and event objects
 * <li> control block attributes
 * <li> specific variations used by delays or classes objects
 * </ul>
 *
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public interface DataMapFeatures {
  /**
   * Binary Input Objects, group 0x
   */
  public byte BIN_IN = (byte) 0x00;

  /**
   * Binary Output Objects, group 1x
   */
  public byte BIN_OUT = (byte) 0x10;

  /**
   * Counter Objects, group 2x
   */
  public byte COUNTER = (byte) 0x20;

  /**
   * Analog Input Objects, group 3x
   */
  public byte ANA_IN = (byte) 0x30;

  /**
   * Analog Output Objects, group 4x
   */
  public byte ANA_OUT = (byte) 0x40; // Analog Output Objects, group 4x

  /**
   * Time Object Definitions, group 5x
   */
  public byte TIME = (byte) 0x50; // Time Object Definitions, group 5x

  /**
   * Classes of data, group 6x
   */
  public byte CLASS = (byte) 0x60;

  /**
   * Internal Indications, group 8x
   */
  public byte IIN = (byte) 0x80;

  /**
   * Means variation requested is not specified
   */
  public byte ALL_VARIATIONS = (byte) 0;

  /**
   * <p>
   * Qualifier 0
   * Following bytes define a range of objects
   * <ul>
   * <li> byte+1 : start
   * <li> byte+2 : stop
   * </ul>
   * Range : [index(start) - index(stop)]
   */
  public byte START_STOP_8 = (byte) 0x00;

  /**
   * <p>
   * Qualifier 1
   * Following bytes define a range of objects
   * <ul>
   * <li> byte+1 & byte+2 : start (LSB)
   * <li> byte+3 & byte+4 : stop (LSB)
   * </ul>
   * Range : [index(start) - index(stop)]
   */
  public byte START_STOP_16 = (byte) 0x01;

  /**
   * <p>
   * Qualifier 6
   * There's no range specified following this qualifier
   * Range : [first known index - last known index]
   */
  public byte ALL_POINTS = (byte) 0x06;

  /**
   * <p>
   * Qualifier 7
   * Following bytes define a quantity of objects
   * <ul>
   * <li> byte+1 : quantity
   * </ul>
   * Range : [0 - index(quantity-1)]
   */
  public byte QUANTITY_8 = (byte) 0x07;

  /**
   * <p>
   * Qualifier 8
   * Following bytes define a quantity of objects
   * <ul>
   * <li> byte+1 & byte+2 : quantity (LSB)
   * </ul>
   * Range : [0 - index(quantity-1)]
   */
  public byte QUANTITY_16 = (byte) 0x08;

  /**
   * <p>
   * Qualifier 17
   * Following bytes define the number of objects requested
   * and their indexes
   * <ul>
   * <li> byte+1 : number of objects requested
   * <li> byte+2 + ... + byte+n : index of each point requested
   * </ul>
   * Range : [index 0, index 1, ... ,last index]
   */
  public byte INDEXES_8 = (byte) 0x17;

  /**
   * <p>
   * Qualifier 28
   * Following bytes define the number of objects requested
   * and their indexes
   * <ul>
   * <li> byte+1 & byte+2 : number of objects requested (LSB)
   * <li> (byte+3 & byte+4) + ... + (byte+2n-1 & byte+2n) : index
   *  of each point requested (LSB)
   * </ul>
   * Range : [index 0, index 1, ... ,last index]
   */
  public byte INDEXES_16 = (byte) 0x28;

  /**
   * Group used by Binary Input static Objects
   */
  public byte BINARY_INPUT_STATIC = (byte) 1;

  /**
   * Group used by Binary Output static Objects
   */
  public byte BINARY_OUTPUT_STATIC = (byte) 10;

  /**
   * Group used by Counter static Objects
   */
  public byte COUNTER_STATIC = (byte) 20;

  /**
   * Group used by Analog Input static Objects
   */
  public byte ANALOG_INPUT_STATIC = (byte) 30;

  /**
   * Group used by Analog Output static Objects
   */
  public byte ANALOG_OUTPUT_STATIC = (byte) 40;

  /**
   * Group used by Time static Objects
   */
  public byte TIME_STATIC = (byte) 50;

  /**
   * Group used by Class 0, 1, 2, 3 Objects
   */
  public byte CLASS_STATIC = (byte) 60;

  /**
   * Group used by Internal Indications Objects
   */
  public byte IIN_STATIC = (byte) 80;

  /**
   * Group used by Binary Input events Objects
   */
  public byte BINARY_INPUT_EVENT = (byte) 2; // Group used by Binary Input events Objects

  /**
   * Group used by Counter events Objects
   */
  public byte COUNTER_EVENT = (byte) 22;

  /**
   * Group used by Analog Input events Objects
   */
  public byte ANALOG_INPUT_EVENT = (byte) 32;

  /**
   * Group used by Binary Output commands Objects
   */
  public byte BINARY_OUTPUT_COMMAND = (byte) 12;

  /**
   * Group used by Analog Output commands Objects
   */
  public byte ANALOG_OUTPUT_COMMAND = (byte) 41;

  /**
   * Group used by Time commands Objects
   */
  public byte TIME_COMMAND = (byte) 50;

  /**
   * Group used by Time Delays Objects
   */
  public byte TIME_DELAY = (byte) 52;

  /**
   * Variation used by Time Delays Objects with a 'second' object time
   */
  public byte COARSE = (byte) 1;

  /**
   * Variation used by Time Delays Objects with a 'millisecond' object time
   */
  public byte FINE = (byte) 2;

  /**
   * Variation used by Class 0 Objects (static)
   */
  public byte CLASS_0_VAR = (byte) 1;

  /**
   * Variation used by Class 1 Objects (events, high priority)
   */
  public byte CLASS_1_VAR = (byte) 2;

  /**
   * Variation used by Class 2 Objects (events, medium priority)
   */
  public byte CLASS_2_VAR = (byte) 3;

  /**
   * Variation used by Class 3 Objects (events, low priority)
   */
  public byte CLASS_3_VAR = (byte) 4;

  /**
   * Latch on control attribute
   */
  public byte LATCH_ON = (byte) 0x03;

  /**
   * Latch off control attribute
   */
  public byte LATCH_OFF = (byte) 0x04;

  /**
   * Pulse-on / Close control attribute
   */
  public byte PULSE_ON_CLOSE = (byte) 0x81;

  /**
   * Pulse-on / Trip control attribute
   */
  public byte PULSE_ON_TRIP = (byte) 0x41;

  /**
   * Data is added to application frame
   */
  public boolean WITH_DATA = true;

  /**
   * Data is not added to application frame
   */
  public boolean WITHOUT_DATA = false;
}