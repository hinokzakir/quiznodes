package com.quiznodes.info;

import com.quiznodes.proto.Empty;
import com.quiznodes.proto.leaderInfoCommunicationGrpc;
import com.quiznodes.proto.leaderInfoMessage;
import io.grpc.stub.StreamObserver;

//Handles incoming gRPC messaged to the leader info server
public class LeaderInfoHandler extends leaderInfoCommunicationGrpc.leaderInfoCommunicationImplBase {
    private LeaderInfoBackend leaderInfoBackend;
    public LeaderInfoHandler() {
        leaderInfoBackend = new LeaderInfoBackend();
    }
    @Override
    public void addLeader(leaderInfoMessage request, StreamObserver<Empty> responseObserver) {
        leaderInfoBackend.addLeader(request.getIp(), request.getPort());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLeader(Empty request, StreamObserver<leaderInfoMessage> responseObserver) {
        leaderInfoMessage message = leaderInfoMessage.newBuilder()
                .setIp(leaderInfoBackend.getCurrentLeaderIp())
                .setPort(leaderInfoBackend.getCurrentLeaderPort())
                .build();
        responseObserver.onNext(message);
        responseObserver.onCompleted();
    }
}
