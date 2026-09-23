package com.quiznodes.server;

import com.quiznodes.server.types.Quiz;

import java.util.ArrayList;
import java.util.List;

public class Manager {
    private ServerBackend serverBackend;
    private ServerNode server;
    private List<ServerToServerCommunication> servers = new ArrayList<>();
    public Manager() throws Exception {
        serverBackend = new ServerBackend();
        server = new ServerNode(0, this);
        server.start();
    }
    public void addServer(String host, int port){
        servers.add(new ServerToServerCommunication(host, port));
    }

    public Quiz getQuiz(String id){
        return serverBackend.getQuiz(id);
    }

    public void addQuiz(String name, Quiz quiz){
        serverBackend.addQuiz(name, quiz);
    }
}
