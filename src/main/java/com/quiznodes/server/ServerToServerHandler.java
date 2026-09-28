package com.quiznodes.server;

import com.quiznodes.proto.quizMessage;
import com.quiznodes.proto.serverToServerCommunicationGrpc;
import com.quiznodes.proto.*;
import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class ServerToServerHandler extends serverToServerCommunicationGrpc.serverToServerCommunicationImplBase {
    private Manager manager;
    public ServerToServerHandler(Manager manager) {
        this.manager = manager;
    }

    @Override
    public void replicateQuiz(quizMessage request, StreamObserver<replicateResponse> responseObserver) {
        List<Question> questions = new ArrayList<>();
        for(questionMessage q : request.getQuestionsList()){
            Question question = new Question(
                    q.getQuestion(),
                    q.getCorrectAnswer(),
                    q.getChoicesList()
            );
            questions.add(question);
        }
        manager.addQuiz(request.getId(), new Quiz(questions));
        System.out.println("Quiz has been added");
        System.out.println("Questions:" + questions);
        long capacity = manager.getServerCapacity();
        responseObserver.onNext(replicateResponse.newBuilder().setAvailableCapacity(capacity).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendLeaderHeartbeat(Empty request, StreamObserver<heartbeatResponse> responseObserver) {
        long capacity = manager.getServerCapacity();
        System.out.println("Received Heartbeat");
        responseObserver.onNext(heartbeatResponse.newBuilder().setAvailableCapacity(capacity).build());
        responseObserver.onCompleted();
    }
}
