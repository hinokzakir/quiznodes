package com.quiznodes.server;

import com.quiznodes.proto.empty;
import com.quiznodes.proto.quizMessage;
import com.quiznodes.proto.serverToServerCommunicationGrpc;
import com.quiznodes.proto.*;
import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class ServerToServerHandler extends serverToServerCommunicationGrpc.serverToServerCommunicationImplBase {
    private ServerBackend serverBackend;
    public ServerToServerHandler(ServerBackend serverBackend){
        this.serverBackend = serverBackend;
    }
    @Override
    public void replicateQuiz(quizMessage request, StreamObserver<empty> responseObserver) {
        List<Question> questions = new ArrayList<>();
        for(questionMessage q : request.getQuestionsList()){
            Question question = new Question(
                    q.getQuestion(),
                    q.getCorrectAnswer(),
                    q.getChoicesList()
            );
            questions.add(question);
        }
        serverBackend.addQuiz(request.getId(), new Quiz(questions));
    }
}
