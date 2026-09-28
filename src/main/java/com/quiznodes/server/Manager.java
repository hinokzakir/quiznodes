package com.quiznodes.server;

import com.quiznodes.proto.quizMessage;
import com.quiznodes.server.types.NodeInfo;
import com.quiznodes.server.types.Quiz;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Manager {
    private ServerBackend serverBackend;
    private ServerNode server;
    private List<ServerToServerCommunication> servers = new ArrayList<>();
    private List<Long> capacities = new ArrayList<>();
    private Set<Integer> fullServers = new HashSet<>();
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor();
    public Manager(int port, int max_capacity, boolean isLeader) throws Exception {
        serverBackend = new ServerBackend(max_capacity);
        server = new ServerNode(port, this);
        addServer("localhost", 2222);
        addServer("localhost", 2223);
        addServer("localhost", port);
        if (isLeader) {
            startLeaderHeartbeat();
        }
        server.start();
    }
    public void addServer(String host, int port){
        servers.add(new ServerToServerCommunication(host, port));
        capacities.add(0L);
        System.out.println("Server added" +  host + ":" + port);
    }

    public Quiz getQuiz(String id){
        return serverBackend.getQuiz(id);
    }

    public void addQuiz(String name, Quiz quiz){
        serverBackend.addQuiz(name, quiz);
    }

    public void replicateQuiz(Quiz quiz){
        int replicationFactor;
        int nrServers = servers.size() - fullServers.size();
        if(nrServers == 1){
            replicationFactor = 1;
        } else if(nrServers < 4){
            replicationFactor = 2;
        } else {
            replicationFactor = 3;
        }
        Integer[] indices = new Integer[capacities.size() - fullServers.size()];
        int indicesIndex = 0;
        for (int i = 0; i < capacities.size(); i++) {
            if (!fullServers.contains(i)) {
                indices[indicesIndex] = i;
                indicesIndex++;
            }
        }

        Arrays.sort(indices, (a, b) -> Long.compare(capacities.get(b), capacities.get(a)));

        for(int i = 0; i < replicationFactor; i++){
            long capacity = servers.get(indices[i]).replicateQuiz(quiz);
            if (capacity < 1){
                fullServers.add(indices[i]);
            }
        }
    }

    public void stopServer(){
        server.stop();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void startLeaderHeartbeat(){
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendLeaderHeartbeat,
                0,
                5,
                TimeUnit.SECONDS
        );
    }

    public void sendLeaderHeartbeat(){
        for (int i = 0; i < servers.size(); i++) {
            ServerToServerCommunication serverToServerCommunication = servers.get(i);
            long capacity = serverToServerCommunication.sendLeaderHeartbeat();
            capacities.set(i,capacity);
        }
        System.out.println("Capacities: " + capacities);
    }

    public List<Quiz> getAllQuizList(){
        return serverBackend.getAllQuizList();
    }

    public long getServerCapacity(){
        return serverBackend.getCapacity();
    }

    public static void main(String[] args) throws Exception {
        Manager manager = new Manager(50051, 100,false);
    }
}
