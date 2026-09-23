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
    public void replicateQuiz(quizMessage request, StreamObserver<Empty> responseObserver) {
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
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
