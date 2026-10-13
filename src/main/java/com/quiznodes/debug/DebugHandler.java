package com.quiznodes.debug;

import com.quiznodes.proto.Empty;
import com.quiznodes.proto.debugCommunicationGrpc;
import com.quiznodes.proto.joinMessage;
import io.grpc.stub.StreamObserver;

public class DebugHandler extends debugCommunicationGrpc.debugCommunicationImplBase{
    DebugController controller;
    public DebugHandler(DebugController controller){
        this.controller = controller;
    }
    @Override
    public void addNewServer(joinMessage request, StreamObserver<Empty> responseObserver) {
        controller.addServer(request.getIp(),request.getPort());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
