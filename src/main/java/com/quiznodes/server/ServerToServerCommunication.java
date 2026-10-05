package com.quiznodes.server;

import com.google.rpc.context.AttributeContext;
import com.quiznodes.proto.*;
import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerToServerCommunication {
    private serverToServerCommunicationGrpc.serverToServerCommunicationBlockingStub stub;
    private String host;
    private int port;
    ManagedChannel channel;

    public ServerToServerCommunication(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = serverToServerCommunicationGrpc.newBlockingStub(channel);
        this.host = host;
        this.port = port;
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
        } catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED
                    ||  e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                System.out.println("Replicate Quiz failed");
                return -1;
            }
            throw e;
        }
    }

    public long sendLeaderHeartbeat() {
        try {
            heartbeatResponse response = stub.withDeadlineAfter(3, TimeUnit.SECONDS).sendLeaderHeartbeat(Empty.newBuilder().build());
            System.out.println("Got Capacity:" + response.getAvailableCapacity());
            return response.getAvailableCapacity();
        }catch (StatusRuntimeException e) {
            if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED
                    ||  e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                System.out.println("Heartbeat Failed");
                return -1;
            }
            throw e;
        }
    }

    public quizMessage getQuiz(String id) {
        quizId request = quizId.newBuilder().setId(id).build();
        return stub.getQuiz(request);
    }

    public void joinNetwork(String ip, int port) {
        joinMessage message = joinMessage.newBuilder().setIp(ip).setPort(port).build();
        stub.joinNetwork(message);
    }

    public responseVoteMessage requestVote(String ip, int port, long term) {
        requestVoteMessage message = requestVoteMessage.newBuilder()
            .setIp(ip)
            .setPort(port)
            .setTerm(term)
            .build();
        
        // handle response from other node
        responseVoteMessage response = stub.requestVote(message);
        return response;     
    }

    public void shutdown(){
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public int getPort() {
        return port;
    }
    public String getHost() {
        return host;
    }

}
