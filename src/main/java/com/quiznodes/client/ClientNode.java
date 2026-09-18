package com.quiznodes.client;

import java.util.List;

import com.quiznodes.proto.questionMessage;
import com.quiznodes.proto.quizId;
import com.quiznodes.proto.quizMessage;
import com.quiznodes.proto.serviceHandlerGrpc;
import com.quiznodes.proto.testMessage;
import com.quiznodes.server.types.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientNode {
    private final ManagedChannel channel;
    private final serviceHandlerGrpc.serviceHandlerBlockingStub blockingStub;

    public ClientNode(String host, int port) {
        // set up connection to server
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        // client side stub
        this.blockingStub = serviceHandlerGrpc.newBlockingStub(channel);
    }

    public String testRequestString(String msg) {
        try {
            testMessage request = testMessage.newBuilder().setMsg(msg).build();
            testMessage response = blockingStub.testService(request);

            return response.getMsg();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return new String();
    }

    public Quiz getQuiz(String id) {
        try {
            if(id == null){
                return null;
            }
            quizId request = quizId.newBuilder().setId(id).build();
            quizMessage response = blockingStub.getQuiz(request);

            String newId = response.getId();
            Quiz quiz = new Quiz(newId);
            for (questionMessage q : response.getQuestionsList()) {
                String qid = q.getId();
                String correct = q.getCorrectAnswer();
                List<String> choices = q.getChoicesList();
                Question question = new Question(qid, correct, choices);
                quiz.addQuestion(question);
            }
            return quiz;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String sendQuiz(Quiz quiz) {
        try {
            quizMessage.Builder request = quizMessage.newBuilder().setId(quiz.getId());
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
                request.addQuestions(q);
            }
            quizId response = blockingStub.sendQuiz(request.build());
            return response.getId();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}