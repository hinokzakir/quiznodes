package com.quiznodes.server;
import com.quiznodes.proto.*;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

//Used for handling incoming messages from the debugger
public class DebugHandler extends debugCommunicationGrpc.debugCommunicationImplBase{
    Manager manager;
    public DebugHandler(Manager manager){
        this.manager = manager;
    }
    @Override
    public void sleep(Empty request, StreamObserver<Empty> responseObserver) {
        manager.setSleep(true);
        manager.sleep();
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void kill(Empty request, StreamObserver<Empty> responseObserver) {
        manager.shutdownNode();
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void heartbeat(Empty request, StreamObserver<debugHeartbeatResponse> responseObserver) {
        long capacity= manager.getServerCapacity();
        boolean isLeader = manager.amILeader();
        int nrQuizzes = 0;
        if(isLeader){
            nrQuizzes = manager.getNrQuizzes();
        }
        debugHeartbeatResponse response = debugHeartbeatResponse.newBuilder()
                .setCapacity(capacity)
                .setLeader(isLeader)
                .setNrQuizzes(nrQuizzes)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void spawnServer(Empty request, StreamObserver<Empty> responseObserver) {
        try {
            manager.startNewServer();
        } catch (IOException e) {
            System.err.println("Problem with starting server");
        }
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
