/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package br.org.scadabr.dnp34j.master.common;

/**
 * See page 81 of IEEE Spec
 * @author Terry Packer
 */
public class InternalIndication {

    private byte iin1;
    private byte iin2;

    //Better than nothing but couldn't get transport layer seq easily
    private byte applicationLayerSequence;

    public InternalIndication(byte iin1, byte iin2, byte applicationLayerSequence) {
        this.iin1 = iin1;
        this.iin2 = iin2;
        this.applicationLayerSequence = applicationLayerSequence;
    }

    public InternalIndication() {

    }

    public boolean broadcastRecieved() {
        return (iin1 & 0b00000001) > (byte)0;
    }

    public boolean class1DataAvailable() {
        return (iin1 & 0b00000010) > (byte)0;
    }

    public boolean class2DataAvailable() {
        return (iin1 & 0b0000100) > (byte)0;
    }

    public boolean class3DataAvailable() {
        return (iin1 & 0b00001000) > (byte)0;
    }

    public boolean needTime() {
        return (iin1 & 0b00010000) > (byte)0;
    }

    public boolean localControl() {
        return (iin1 & 0b00100000) > (byte)0;
    }

    public boolean deviceTrouble() {
        return (iin1 & 0b01000000) > (byte)0;
    }

    public boolean deviceRestart() {
        return (iin1 & 0b01000000) > (byte)0;
    }

    public boolean codeNotSupported() {
        return (iin2 & 0b00000001) > (byte)0;
    }

    public boolean objectUnknown() {
        return (iin2 & 0b00000010) > (byte)0;
    }

    public boolean parameterError() {
        return (iin2 & 0b00000100) > (byte)0;
    }

    public boolean eventBufferOverflow() {
        return (iin2 & 0b00001000) > (byte)0;
    }

    public boolean alreadyExecuting() {
        return (iin2 & 0b00010000) > (byte)0;
    }

    public boolean configurationCorrupt() {
        return (iin2 & 0b00100000) > (byte)0;
    }

    public boolean isError() {
        return deviceTrouble() || codeNotSupported() || objectUnknown() || parameterError() || eventBufferOverflow() || configurationCorrupt();
    }

    public boolean shouldNotify() {
        if(iin1 > 0 || iin2 > 0) {
            return true;
        }else {
            return false;
        }
    }

    public byte getIin1() {
        return iin1;
    }

    public void setIin1(byte iin1) {
        this.iin1 = iin1;
    }

    public byte getIin2() {
        return iin2;
    }

    public void setIin2(byte iin2) {
        this.iin2 = iin2;
    }

    public byte getApplicationLayerSequence() {
        return applicationLayerSequence;
    }

    public void setApplicationLayerSequence(byte applicationLayerSequence) {
        this.applicationLayerSequence = applicationLayerSequence;
    }

}
