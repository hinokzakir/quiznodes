package com.quiznodes.server;

import com.quiznodes.proto.leaderInfoCommunicationGrpc;
import com.quiznodes.proto.leaderInfoMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

//Used for communicating with the leader info server
public class LeaderInfoCommunication {
    private final ManagedChannel channel;
    private final leaderInfoCommunicationGrpc.leaderInfoCommunicationBlockingStub blockingStub;

    public LeaderInfoCommunication(String ip, int port) {
        this.channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();
        this.blockingStub = leaderInfoCommunicationGrpc.newBlockingStub(channel);
    }
    public void addLeader(String ip, int port) {
        leaderInfoMessage message = leaderInfoMessage.newBuilder().setIp(ip).setPort(port).build();
        blockingStub.addLeader(message);
    }
}
