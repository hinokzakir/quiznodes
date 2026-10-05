package com.quiznodes.server.types;

public class ServerInfo {
    private String ip;
    private int port;

    public ServerInfo (String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }
    public int getPort() {
        return port;
    }
}
