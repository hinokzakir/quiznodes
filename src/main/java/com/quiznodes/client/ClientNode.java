package com.quiznodes.client;

import com.quiznodes.proto.serviceHandlerGrpc;
import com.quiznodes.proto.testMessage;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientNode {
    private final ManagedChannel channel;
    private final serviceHandlerGrpc.serviceHandlerBlockingStub blockingStub;

    public ClientNode(String host, int port) {
        // set up connection to server
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        // client side stub
        this.blockingStub = serviceHandlerGrpc.newBlockingStub(channel);
    }

    public String testRequestString(String msg) {
        try {
            testMessage request = testMessage.newBuilder().setMsg(msg).build();
            testMessage response = blockingStub.testService(request);

            return response.getMsg();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return new String();
    }
}