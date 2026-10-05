package com.quiznodes.server;
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
        manager.resetElectionTimeout();
        System.out.println("Received Heartbeat");
        responseObserver.onNext(heartbeatResponse.newBuilder().setAvailableCapacity(capacity).build());
        responseObserver.onCompleted();
    }

    // this is the message sent once to update followers server list
    @Override
    public void sendLeaderHeartbeatFirst(heartbeatRequest request, StreamObserver<heartbeatResponse> responseObserver) {
        long capacity = manager.getServerCapacity();
        List<serverInfo> leaderServers = request.getServersList();
        manager.resetElectionTimeout();
        System.out.println("Received Heartbeat and new server list");

        // here i need to add something to update the availible list of connected peers
        if(manager.getState() != 0) {
            manager.updateServerList(leaderServers);
        }

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

    @Override
    public void joinNetwork(joinMessage request, StreamObserver<Empty> responseObserver) {
        manager.addServer(request.getIp(), request.getPort());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    
    }
    
    @Override
    public void requestVote(requestVoteMessage request, StreamObserver<responseVoteMessage> responseObserver) {
        // handling voting request from server
        responseVoteMessage.Builder builder = responseVoteMessage.newBuilder();
        
        // first we need to check if we already have voted

        if(request.getTerm() > manager.electionHandler.getTerm()) {
            manager.setState(2); // set server to candidate since we are 1 term behind
            manager.electionHandler.setTerm(request.getTerm());
            manager.electionHandler.setHasVoted(false);
        }

        long localTerm = manager.electionHandler.getTerm();
        boolean hasVoted = manager.electionHandler.getHasVoted();

        boolean voteGranted = ((request.getTerm() >= localTerm) && !hasVoted);

        if(voteGranted) {
            manager.electionHandler.setHasVoted(voteGranted);
            manager.resetElectionTimeout();
        }
        System.out.println("Vote requested by " + request.getPort() + " for term " 
            + request.getTerm() + ", granted: " + voteGranted);
        
        builder.setVoteGranted(voteGranted);

        builder.setTerm(manager.electionHandler.getTerm());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
