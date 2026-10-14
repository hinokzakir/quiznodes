package com.quiznodes.debug;

import com.quiznodes.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

public class DebugCommunication {
    private final ManagedChannel channel;
    private final debugCommunicationGrpc.debugCommunicationBlockingStub blockingStub;

    public DebugCommunication(String ip, int port) {
        this.channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();
        this.blockingStub = debugCommunicationGrpc.newBlockingStub(channel);
    }
    public void sleep() {
        blockingStub.sleep(Empty.newBuilder().build());
    }
    public void killServer() {
        blockingStub.kill(Empty.newBuilder().build());
    }

    public debugHeartbeatResponse sendHeartbeat(){
        try {
            return blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).heartbeat(Empty.newBuilder().build());
        } catch (StatusRuntimeException e) {
            throw e;
        }
    }

    public void spawnServer(){
        blockingStub.spawnServer(Empty.newBuilder().build());
    }
}
