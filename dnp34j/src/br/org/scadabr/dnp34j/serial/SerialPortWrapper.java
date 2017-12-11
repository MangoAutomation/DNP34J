/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All
 *            rights reserved.
 * @author Terry Packer
 */
package br.org.scadabr.dnp34j.serial;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper to further aid in abstracting dnp34j from a serial port implementation
 * 
 * @author Terry Packer
 *
 */
public interface SerialPortWrapper {

    /**
     * Close the Serial Port
     */
    void close() throws Exception;

    /**
     * 
     */
    void open() throws Exception;

    /**
     * 
     * Return the input stream for an open port
     * 
     * @return
     */
    InputStream getInputStream();

    /**
     * Return the output stream for an open port
     * 
     * @return
     */
    OutputStream getOutputStream();

}
