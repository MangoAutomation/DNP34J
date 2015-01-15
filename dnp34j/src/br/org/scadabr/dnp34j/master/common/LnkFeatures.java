/*
 * This interface contains DNP link-relative values
 *
 * Class    com.itlity.protocol.common.LnkFeatures
 * File     LnkFeatures.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common;


/**
 * <p>
 * LnkFeatures defines
 * <ul>
 * <li> Link frame fields identifiers
 * <li> Messages of Primary Station
 * <li> Messages of Secondary Station
 * </ul>
 *
 * @author Alexis CLERC
 */
public interface LnkFeatures {
  /**
   * First byte of a DNP3 link frame
   */
  public byte START_0 = 0x05;

  /**
   * Second byte of a DNP3 link frame
   */
  public byte START_1 = 0x64;

  /**
   * Third byte of an empty DNP3 link frame
   */
  public byte EMPTY_LENGTH = 0x05;

  /**
   * Broadcast address
   */
  public int BROADCAST = 0xFFFF;

  /**
   * Reset link function
   */
  public byte RESET_LINK = 0x00;

  /**
   * Reset user function
   */
  public byte RESET_USER = 0x01;

  /**
   * Test link function
   */
  public byte TEST_LINK = 0x02;

  /**
   * Confirmed data function
   */
  public byte CON_DATA = 0x03;

  /**
   * Unconfirmed data function
   */
  public byte UNCON_DATA = 0x04;

  /**
   * Request for link status function
   */
  public byte REQUEST = 0x09;

  /**
   * Respond for link status function
   */
  public byte RESPOND = 0x0b;

  /**
   * Acknowledgement function
   */
  public byte ACK = 0x00;

  /**
   * Non-Acknowledgement function
   */
  public byte NACK = 0x01;
}
