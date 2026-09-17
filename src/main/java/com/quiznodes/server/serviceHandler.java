package com.quiznodes.server;

import com.quiznodes.proto.*;

import io.grpc.stub.StreamObserver;

public class serviceHandler extends serviceHandlerGrpc.serviceHandlerImplBase {
    @Override
    public void testService(testMessage request, StreamObserver<testMessage> responseObserver) {

        testMessage.Builder builder = testMessage.newBuilder().setMsg("FOUND MESSAGE: " + request.getMsg());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
