/*
 * Circular frame queue
 *
 * Class    com.itlity.protocol.common.utils.Queue
 * File     Queue.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common.utils;


/**
 * <p>
 * Circular frame queue for a link, transport, application
 * or user layer frame management, with 2 pointers
 * <ul>
 * <li> offset : read pointer
 * <li> marker : write pointer
 * </ul>
 *
 * Data window is between these 2 values
 * It contains an entry for each frame queued, which length is the value of the entry
 * This object is intended to be used with byte[] or Buffer objects
 *
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC</a>
 */
public class Queue {
  //=============================================================================
  // Attributes
  //=============================================================================  

  /**
   * Queue capacity
   */
  public int size;

  /**
   * Store the length of each frame queued
   */
  public int[] array;

  /**
   * Read access pointer
   */
  public int offset;

  /**
   * Write access pointer
   */
  public int marker;

  /**
   * Length of last frame processed
   */
  public int result;

  //=============================================================================
  // Constructor
  //=============================================================================

  /**
   * Build a frame queue with default capacity
   */
  public Queue() {
    size     = 20;
    array    = new int[size];
    offset   = 0;
    marker   = 0;
  }

  /**
   * Build a frame queue with specified capacity
   *
   * @param length   capacity
   */
  public Queue(int length) {
    size     = length;
    array    = new int[size];
    offset   = 0;
    marker   = 0;
  }

  //=============================================================================
  // Methods
  //=============================================================================

  /**
   * Push the size of current frame queued
   *
   * @param length  this size
   */
  public void push(int length) {
    array[marker] = length;
    marker = (marker + 1) % size;
  }

  /**
   * Pop next frame of the queue, which is intended to be read
   *
   * @return The length of this frame
   */
  public int pop() {
    result   = array[offset];
    offset   = (offset + 1) % size;

    return result;
  }

  /**
   * Verify if the queue is empty
   *
   * @return The queue is empty
   */
  public boolean empty() {
    return (offset == marker);
  }
}
