package com.quiznodes.server;

import com.google.rpc.context.AttributeContext;
import com.quiznodes.proto.*;
import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;

public class ServerToServerCommunication {
    private serverToServerCommunicationGrpc.serverToServerCommunicationBlockingStub stub;
    public ServerToServerCommunication(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = serverToServerCommunicationGrpc.newBlockingStub(channel);
    }

    public long replicateQuiz(Quiz quiz) {
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
            replicateResponse response = stub.replicateQuiz(request.build());
            return response.getAvailableCapacity();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long sendLeaderHeartbeat(){
        heartbeatResponse response = stub.sendLeaderHeartbeat(Empty.newBuilder().build());
        System.out.println("Got Capacity:" + response.getAvailableCapacity());
        return response.getAvailableCapacity();
    }
}
