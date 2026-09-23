package com.quiznodes.server;

import com.quiznodes.proto.quizMessage;
import com.quiznodes.server.types.Quiz;

import java.util.ArrayList;
import java.util.List;

public class Manager {
    private ServerBackend serverBackend;
    private ServerNode server;
    private List<ServerToServerCommunication> servers = new ArrayList<>();
    public Manager(int port) throws Exception {
        serverBackend = new ServerBackend();
        server = new ServerNode(port, this);
        addServer("localhost", 21705);
        server.start();
    }
    public void addServer(String host, int port){
        servers.add(new ServerToServerCommunication(host, port));
        System.out.println("Server added" +  host + ":" + port);
    }

    public Quiz getQuiz(String id){
        return serverBackend.getQuiz(id);
    }

    public void addQuiz(String name, Quiz quiz){
        serverBackend.addQuiz(name, quiz);
    }

    public void replicateQuiz(Quiz quiz){
        System.out.println(servers.toString());
        servers.getFirst().replicateQuiz(quiz);
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
        Manager manager = new Manager(50051);
    }
}
