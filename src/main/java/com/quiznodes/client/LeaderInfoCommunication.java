package com.quiznodes.client;

import com.quiznodes.proto.Empty;
import com.quiznodes.proto.leaderInfoCommunicationGrpc;
import com.quiznodes.proto.leaderInfoMessage;
import com.quiznodes.proto.serviceHandlerGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class LeaderInfoCommunication {
    private final ManagedChannel channel;
    private final leaderInfoCommunicationGrpc.leaderInfoCommunicationBlockingStub blockingStub;

    public LeaderInfoCommunication(String ip, int port) {
        this.channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();
        this.blockingStub = leaderInfoCommunicationGrpc.newBlockingStub(channel);
    }
    public leaderInfoMessage getLeader() {
        return blockingStub.getLeader(Empty.newBuilder().build());
    }
}
