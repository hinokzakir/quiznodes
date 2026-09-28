package com.quiznodes.server.types;

public class NodeInfo {
    private long space;
    private long max_capacity;

    public NodeInfo(int max_capacity) {
        this.max_capacity = max_capacity;
        space = 0;
    }

    public long getMax_capacity() {
        return max_capacity;
    }

    public long getCapacity() {
        return max_capacity - space;
    }
    public void quizAdded(){
        space++;
    }
}
