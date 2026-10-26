package com.quiznodes.server;

import com.quiznodes.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

//Used for communicating with the debugger
public class DebugCommunication {
    private final ManagedChannel channel;
    private final debugCommunicationGrpc.debugCommunicationBlockingStub blockingStub;

    public DebugCommunication(String ip, int port) {
        this.channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();
        this.blockingStub = debugCommunicationGrpc.newBlockingStub(channel);
    }

    public void sendToDebugger(String ip, int port){
        joinMessage message = joinMessage.newBuilder().setIp(ip).setPort(port).build();
        blockingStub.addNewServer(message);
    }
}