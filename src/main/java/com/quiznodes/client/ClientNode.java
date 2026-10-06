package com.quiznodes.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.quiznodes.proto.Empty;
import com.quiznodes.proto.questionMessage;
import com.quiznodes.proto.quizId;
import com.quiznodes.proto.quizList;
import com.quiznodes.proto.quizMessage;
import com.quiznodes.proto.serviceHandlerGrpc;
import com.quiznodes.proto.testMessage;
import com.quiznodes.server.types.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

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
            testMessage response = blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).testService(request);

            return response.getMsg();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new StatusRuntimeException(e.getStatus());
            }
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return new String();
    }

    public Quiz getQuiz(String id) {
        try {
            if (id == null) {
                return null;
            }
            quizId request = quizId.newBuilder().setId(id).build();
            quizMessage response = blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).getQuiz(request);

            String newId = response.getId();
            Quiz quiz = new Quiz(newId);
            for (questionMessage q : response.getQuestionsList()) {
                String qid = q.getId();
                String correct = q.getCorrectAnswer();
                List<String> choices = q.getChoicesList();
                String questionString = q.getQuestion();
                Question question = new Question(qid, questionString, correct, choices);
                quiz.addQuestion(question);
            }
            return quiz;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new StatusRuntimeException(e.getStatus());
            }
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
            quizId response = blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).sendQuiz(request.build());
            return response.getId();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new StatusRuntimeException(e.getStatus());
            }
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public List<Quiz> getAllQuiz() {
        try {
            Empty request = Empty.newBuilder().build();
            quizList response = blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).getAllQuiz(request);

            List<Quiz> quizzes = new ArrayList<>();
            for (quizMessage qMsg : response.getQuizzesList()) {
                Quiz quiz = new Quiz(qMsg.getId());
                for (questionMessage qm : qMsg.getQuestionsList()) {
                    Question question = new Question(qm.getId(), qm.getQuestion(), qm.getCorrectAnswer(),
                            qm.getChoicesList());
                    quiz.addQuestion(question);
                }
                quizzes.add(quiz);
            }
            return quizzes;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new StatusRuntimeException(e.getStatus());
            }
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}