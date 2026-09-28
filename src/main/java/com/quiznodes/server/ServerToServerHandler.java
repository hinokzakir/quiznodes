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
        manager.addQuiz(request.getId(), new Quiz(request.getId(),questions));
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

    @Override
    public void getQuiz(quizId request, StreamObserver<quizMessage> responseObserver) {
        String id = request.getId();

        Quiz quiz = manager.getQuiz(id);
        System.out.println("Quiz: " + quiz);

        quizMessage.Builder builder = quizMessage.newBuilder().setId(quiz.getId());

        List<Question> questions = quiz.getQuestions();

        for (Question question : questions) {
            questionMessage.Builder q = questionMessage.newBuilder()
                    .setId(question.getId())
                    .setQuestion(question.getQuestion())
                    .setCorrectAnswer(question.getCorrectAnswer());

            for (String choice : question.getChoices()) {
                q.addChoices(choice);
            }

            q.build();
            builder.addQuestions(q);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
