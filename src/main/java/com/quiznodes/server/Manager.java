package com.quiznodes.server;

import com.quiznodes.proto.quizMessage;
import com.quiznodes.server.types.NodeInfo;
import com.quiznodes.server.types.Quiz;

import java.util.ArrayList;
import java.util.List;

public class Manager {
    private ServerBackend serverBackend;
    private ServerNode server;
    private List<ServerToServerCommunication> servers = new ArrayList<>();
    private List<Integer> capacities = new ArrayList<>();
    public Manager(int port, int max_capacity) throws Exception {
        serverBackend = new ServerBackend(max_capacity);
        server = new ServerNode(port, this);
        addServer("localhost", 2222);
        addServer("localhost", 2223);
        server.start();
    }
    public void addServer(String host, int port){
        servers.add(new ServerToServerCommunication(host, port));
        capacities.add(0);
        System.out.println("Server added" +  host + ":" + port);
    }

    public Quiz getQuiz(String id){
        return serverBackend.getQuiz(id);
    }

    public void addQuiz(String name, Quiz quiz){
        serverBackend.addQuiz(name, quiz);
    }

    public void replicateQuiz(Quiz quiz){
        int minServerIndex = Integer.MAX_VALUE;
        for(int i = 0; i < capacities.size(); i++){
            if(capacities.get(i) < minServerIndex){
                minServerIndex = i;
            }
        }
        servers.get(minServerIndex).replicateQuiz(quiz);
    }

    public void stopServer(){
        server.stop();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public List<Quiz> getAllQuizList(){
        return serverBackend.getAllQuizList();
    }
    public static void main(String[] args) throws Exception {
        Manager manager = new Manager(50051, 100);
    }
}
