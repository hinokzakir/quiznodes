package com.quiznodes.debug;

public class DebugInfo {
    private String ip;
    private int port;
    private double capacity;

    public DebugInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }
}
