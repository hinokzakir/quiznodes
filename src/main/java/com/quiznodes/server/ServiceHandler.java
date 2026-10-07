package com.quiznodes.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<quizList> quizLists = manager.getAllQuizzesFromAllServers();
        Set<String> quizSet = new HashSet<>();
        quizList.Builder response = quizList.newBuilder();

        for(quizList qList : quizLists) {
            for(quizMessage message: qList.getQuizzesList()){
                if (!quizSet.contains(message.getId())){
                    quizSet.add(message.getId());
                    response.addQuizzes(message);
                }
            }
        }
        System.out.println("Nr of quizzes: " + quizSet.size());
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendQuiz(quizMessage request, StreamObserver<quizId> responseObserver) {
        int count = request.getQuestionsCount();
        String quizIdString = request.getId();
        String quizName = request.getName();
        Quiz quiz = new Quiz(quizIdString, quizName);

        for (int i = 0; i < count; i++) {
            String qid = request.getQuestions(i).getId();
            String question = request.getQuestions(i).getQuestion();
            String correct = request.getQuestions(i).getCorrectAnswer();
            List<String> choices = request.getQuestions(i).getChoicesList();
            Question q = new Question(qid, question, correct, choices);
            quiz.addQuestion(q);
        }
        manager.replicateQuiz(quiz);

        quizId.Builder builder = quizId.newBuilder().setId(quiz.getId());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
