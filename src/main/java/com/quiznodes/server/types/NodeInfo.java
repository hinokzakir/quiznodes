package com.quiznodes.server.types;

public class NodeInfo {
    private int capacity;
    private int max_capacity;

    public NodeInfo(int max_capacity) {
        this.max_capacity = max_capacity;
        capacity = 0;
    }
}
