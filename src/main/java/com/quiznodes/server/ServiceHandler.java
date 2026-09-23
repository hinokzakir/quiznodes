package com.quiznodes.server;

import java.util.ArrayList;
import java.util.List;

import com.quiznodes.proto.*;
import com.quiznodes.server.types.*;

import io.grpc.stub.StreamObserver;

public class ServiceHandler extends serviceHandlerGrpc.serviceHandlerImplBase {
    private final Manager manager;

    public ServiceHandler(Manager manager) {
        this.manager = manager;
    }

    @Override
    public void testService(testMessage request, StreamObserver<testMessage> responseObserver) {

        testMessage.Builder builder = testMessage.newBuilder().setMsg("FOUND MESSAGE: " + request.getMsg());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getQuiz(quizId request, StreamObserver<quizMessage> responseObserver) {
        String id = request.getId().toString();

        Quiz quiz = manager.getQuiz(id);

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

    @Override
    public void sendQuiz(quizMessage request, StreamObserver<quizId> responseObserver) {
        int count = request.getQuestionsCount();
        String quizIdString = request.getId();
        Quiz quiz = new Quiz(request.getId());

        for (int i = 0; i < count; i++) {
            String qid = request.getQuestions(i).getId();
            String correct = request.getQuestions(i).getCorrectAnswer();
            List<String> choices = request.getQuestions(i).getChoicesList();
            Question q = new Question(qid, correct, choices);
            quiz.addQuestion(q);
        }
        manager.addQuiz(quizIdString, quiz);
        manager.replicateQuiz(quiz);

        quizId.Builder builder = quizId.newBuilder().setId(quiz.getId());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
