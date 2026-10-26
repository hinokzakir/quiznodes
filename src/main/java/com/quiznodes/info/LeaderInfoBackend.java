package com.quiznodes.info;

//Keeps track of current leader
public class LeaderInfoBackend {
    private String currentLeaderIp;
    private int currentLeaderPort;
    public void addLeader(String ip, int port) {
        currentLeaderIp = ip;
        currentLeaderPort = port;
        System.out.println("Current leader IP: " + currentLeaderIp);
        System.out.println("Current leader Port: " + currentLeaderPort);
    }
    public String getCurrentLeaderIp() {
        return currentLeaderIp;
    }
    public int getCurrentLeaderPort() {
        return currentLeaderPort;
    }
}
