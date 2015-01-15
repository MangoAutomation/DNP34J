/*
 * This interface contains DNP application-relative values
 *
 * Class    com.itlity.protocol.common.AppFeatures
 * File     AppFeatures.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common;


/**
 * <p>
 * AppFeatures defines
 * <ul>
 * <li> Maximum size of transport and application frame
 * <li> Application Functions
 * <li> Internal Indications interpretation
 * </ul>
 *
 * @author Alexis CLERC
 */
public interface AppFeatures {
  /**
   * Confirmation function
   */
  public byte CONFIRM = (byte) 0x00;

  /**
   * Read function
   */
  public byte READ = (byte) 0x01;

  /**
   * Write function
   */
  public byte WRITE = (byte) 0x02;

  /**
   * Select function
   */
  public byte SELECT = (byte) 0x03;

  /**
   * Operate function
   */
  public byte OPERATE = (byte) 0x04;

  /**
   * Direct Operate function
   */
  public byte DIRECT_OPERATE = (byte) 0x05;

  /**
   * Direct Operate without Acknowledgement function
   */
  public byte DIRECT_OPERATE_NO_ACK = (byte) 0x06;

  /**
   * Warm restart function
   */
  public byte WARM_RESTART = (byte) 0x0d;

  /**
   * Cold restart function
   */
  public byte COLD_RESTART = (byte) 0x0e;

  /**
   * Delay measurement function
   */
  public byte DELAY_MEASUREMENT = (byte) 0x17;

  /**
   * Response function
   */
  public byte RESPONSE = (byte) 0x81;

  /**
   * Unsolicited Response function
   */
  public byte UNSOLICITED_RESPONSE = (byte) 0x82;

  /**
   * Maximum size of a transport frame
   */
  public int TRANSPORT_FRAME_SIZE_MAX = 250;

  /**
   * Maximum size of a application frame
   */
  public int APPLICATION_FRAME_SIZE_MAX = 2048;

  /**
   * Device Restart IIN
   */
  public byte DEVICE_RESTART = (byte) 0x80;

  /**
   * Time Synchronisation IIN
   */
  public byte TIME_SYNCHRO = (byte) 0x10;

  /**
   * Class 1 available IIN
   */
  public byte CLASS_1_AVAILABLE = (byte) 0x02;

  /**
   * Class 2 available IIN
   */
  public byte CLASS_2_AVAILABLE = (byte) 0x04;

  /**
   * Class 3 available IIN
   */
  public byte CLASS_3_AVAILABLE = (byte) 0x08;
}