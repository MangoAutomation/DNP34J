/*
 * This interface contains DNP initilization-relative values
 *
 * Class    com.itlity.protocol.common.InitFeatures
 * File     InitFeatures.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common;

/**
 * <p>
 * DataMapFeatures defines
 * <ul>
 * <li>physical parameters
 * <li>buffer parameters
 * <li>display parameters for debugging purposes
 * </ul>
 *
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public interface InitFeatures {

    /**
     * Serial port
     */
    public int SERIAL = 1;

    /**
     * ETHERNET port
     */
    public int ETHERNET = 2;

    /**
     * Buffer - medium size Recommanded for application frames with data
     */
    public int M = 3000;

    /**
     * Buffer - small size Recommanded for link frames with data
     */
    public int S = 500;

    /**
     * Buffer - extra small size Recommanded for frames without data
     */
    public int XS = 20;

    /**
     * Lock object initialization attribute
     */
    public boolean LOCKED = true;

    /**
     * Lock object initialization attribute
     */
    public boolean UNLOCKED = false;

    /**
     * Value for unknown elements
     */
    public int UNKNOWN = 4;

    /**
     * Value for null elements
     */
    public int NULL = 0;

    /**
     * Value ON
     */
    public boolean ON = true;

    /**
     * Value OFF
     */
    public boolean OFF = false;

    /**
     * Estimated delay (ms) to restart an application
     */
    public long RESTART_DELAY = 1000;

}
