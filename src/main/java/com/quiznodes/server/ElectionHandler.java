package com.quiznodes.server;

//Class used during elections
public class ElectionHandler {
    private boolean hasVoted = false;
    private int votes = 0;
    private long term;

    public ElectionHandler(long term) {
        this.term = term;
    }

    public void increaseVote() {
        this.votes++;
    }

    public int getVotes() {
        return votes;
    }

    public void setTerm(long newTerm) {
        this.term = newTerm;
    }

    public long getTerm() {
        return term;
    }

    public void setHasVoted(boolean b) {
        this.hasVoted = b;
    }

    public boolean getHasVoted() {
        return hasVoted;
    }

    public void resetVotes() {
        votes = 0;
    }

}