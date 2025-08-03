package org.example.mpj;

import java.io.Serializable;

/**
 * Result object for packet processing operations.
 * Implements Serializable for MPJ communication.
 */
public class PacketProcessingResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String originalPacket;
    private String sourceIP;
    private long timestamp;
    private double processingTime; // in milliseconds
    private int processorRank;
    private boolean isValid;
    private boolean shouldBlock;
    private String errorMessage;

    public PacketProcessingResult() {
        this.timestamp = System.currentTimeMillis();
        this.isValid = false;
        this.shouldBlock = false;
    }

    // Getters and setters
    public String getOriginalPacket() {
        return originalPacket;
    }

    public void setOriginalPacket(String originalPacket) {
        this.originalPacket = originalPacket;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(double processingTime) {
        this.processingTime = processingTime;
    }

    public int getProcessorRank() {
        return processorRank;
    }

    public void setProcessorRank(int processorRank) {
        this.processorRank = processorRank;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public boolean shouldBlock() {
        return shouldBlock;
    }

    public void setShouldBlock(boolean shouldBlock) {
        this.shouldBlock = shouldBlock;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "PacketProcessingResult{" +
                "sourceIP='" + sourceIP + '\'' +
                ", processingTime=" + processingTime +
                ", processorRank=" + processorRank +
                ", isValid=" + isValid +
                ", shouldBlock=" + shouldBlock +
                '}';
    }
}
