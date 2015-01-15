/*
 * Lock
 *
 * Class    com.itlity.protocol.common.utils.Lock
 * File     Lock.java
 * Author   Alexis CLERC <alexis.clerc@sysaware.com>
 * (c)      SysAware <http://www.sysaware.com>
 *
 */
package br.org.scadabr.dnp34j.master.common.utils;

/**
 * <p>
 * Wait() and notify() operations in a sequence can be processed with a Lock object.
 * 
 * @author <a href="mailto:alexis.clerc@sysaware.com">Alexis CLERC
 *         &lt;alexis.clerc@sysaware.com&gt;</a>
 */
public class Lock {
    // =============================================================================
    // Attributes
    // =============================================================================

    /**
     * Lock state : LOCKED or UNLOCKED
     */
    private boolean state;

    // =============================================================================
    // Constructor
    // =============================================================================

    /**
     * Build a Lock initialized as LOCKED (default)
     */
    public Lock() {
        state = true;
    }

    /**
     * Build a Lock
     * 
     * @param s
     *            Lock initialization : LOCKED or UNLOCKED
     */
    public Lock(boolean s) {
        state = s;
    }

    // =============================================================================
    // Methods
    // =============================================================================

    /**
     * Verify if this object is locked
     * 
     * @return the state of this Lock object
     */
    public boolean isLocked() {
        return state;
    }

    /**
     * Lock this object Waiting() functions on this object are blocking now
     */
    public synchronized void lock() {
        setState(true);
    }

    /**
     * Unlock this object All waiting() functions on this object are unlocked
     */
    public synchronized void unlock() {
        setState(false);
        notifyAll();
    }

    /**
     * Wait until this object is unlocked or timeout has exceeded
     * 
     * @param timeout
     *            timeout in millisecond to wait
     * 
     * @return <tt>true</tt> if object has been unlocked <tt>false</tt> if
     *         timeout has exceeded
     */
    public synchronized boolean waiting(long timeout) {
        if (state == true) {
            try {
                wait(timeout);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return !state;
    }

    /**
     * Wait until this object is unlocked
     */
    public synchronized void waiting() {
        if (state == true) {
            try {
                wait();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return the state
     */
    public boolean isState() {
        return state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(boolean state) {
        this.state = state;
    }
}