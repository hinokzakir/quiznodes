package com.quiznodes.server;
import com.quiznodes.proto.*;
import io.grpc.stub.StreamObserver;

public class DebugHandler extends debugCommunicationGrpc.debugCommunicationImplBase{
    Manager manager;
    public DebugHandler(Manager manager){
        this.manager = manager;
    }
    @Override
    public void sleep(Empty request, StreamObserver<Empty> responseObserver) {
        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            System.err.println("Problem with sleeping");
        }
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void kill(Empty request, StreamObserver<Empty> responseObserver) {
        manager.stopServer();
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void heartbeat(Empty request, StreamObserver<debugHeartbeatResponse> responseObserver) {
        long capacity= manager.getServerCapacity();
        boolean isLeader = manager.amILeader();
        debugHeartbeatResponse response = debugHeartbeatResponse.newBuilder().setCapacity(capacity).setLeader(isLeader).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
