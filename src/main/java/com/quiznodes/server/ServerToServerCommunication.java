package com.quiznodes.server;

import com.google.rpc.context.AttributeContext;
import com.quiznodes.proto.*;
import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;
import com.quiznodes.server.types.ServerInfo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;

public class ServerToServerCommunication {
    private serverToServerCommunicationGrpc.serverToServerCommunicationBlockingStub stub;
    private String host;
    private int port;

    public ServerToServerCommunication(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = serverToServerCommunicationGrpc.newBlockingStub(channel);
        this.host = host;
        this.port = port;
    }

    public long replicateQuiz(Quiz quiz) {
        try {
            quizMessage.Builder request = quizMessage.newBuilder().setId(quiz.getId()).setName(quiz.getName());
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

    public long sendLeaderHeartbeatFirst(List <serverInfo> servers) {
        heartbeatRequest.Builder request = heartbeatRequest.newBuilder();
        for(int i = 0; i < servers.size(); i++) {
            request.addServers(servers.get(i));
        }
        heartbeatResponse response = stub.sendLeaderHeartbeatFirst(request.build());
        System.out.println("Got Capacity:" + response.getAvailableCapacity());
        return response.getAvailableCapacity();
    }

    public long sendLeaderHeartbeat() {
        Empty.Builder request = Empty.newBuilder();
        heartbeatResponse response = stub.sendLeaderHeartbeat(request.build());
        System.out.println("Got Capacity:" + response.getAvailableCapacity());
        return response.getAvailableCapacity();
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

    public int getPort() {
        return port;
    }
    public String getHost() {
        return host;
    }

}
