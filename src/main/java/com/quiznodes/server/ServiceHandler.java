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
        quizMessage response = manager.getQuizFromCorrectServer(request.getId());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override 
    public void getAllQuiz(Empty request, StreamObserver<quizList> responseObserver) {
        List<Quiz> quizzes = manager.getAllQuizList();

        quizList.Builder response = quizList.newBuilder();

        for(Quiz q : quizzes) {
            quizMessage.Builder quiz = quizMessage.newBuilder();
            quiz.setId(q.getId());
            for(Question question : q.getQuestions()){
                questionMessage questionmessage = questionMessage.newBuilder()
                    .addAllChoices(question.getChoices())
                    .setCorrectAnswer(question.getCorrectAnswer())
                    .setId(question.getId())
                    .build();
                
                quiz.addQuestions(questionmessage);
            }   
            response.addQuizzes(quiz.build());
        }
        responseObserver.onNext(response.build());
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
        manager.replicateQuiz(quiz);

        quizId.Builder builder = quizId.newBuilder().setId(quiz.getId());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
