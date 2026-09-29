package com.quiznodes.server;

import com.quiznodes.proto.quizMessage;
import com.quiznodes.server.types.NodeInfo;
import com.quiznodes.server.types.Quiz;

import java.io.IOException;
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
    private Map<String, List<Integer>> quizToServersMap = new HashMap<>();
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor();
    private List<Process> serversStarted = new ArrayList<>();
    public Manager(int port, int max_capacity, boolean isLeader) throws Exception {
        serverBackend = new ServerBackend(max_capacity);
        server = new ServerNode(port, this);
        addServer("localhost", port);
        server.start();
    }
    public void addServer(String host, int port){
        servers.add(new ServerToServerCommunication(host, port));
        capacities.add(0L);
        System.out.println("Server added" +  host + ":" + port);
    }

    public void startNewServer() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                "com.quiznodes.ServerApp"
        );

        processBuilder.inheritIO();

        Process process = processBuilder.start();
        serversStarted.add(process);
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
        quizToServersMap.put(quiz.getId(), new ArrayList<>());
        for(int i = 0; i < replicationFactor; i++){
            long capacity = servers.get(indices[i]).replicateQuiz(quiz);
            quizToServersMap.get(quiz.getId()).add(indices[i]);
            if (capacity < 1){
                fullServers.add(indices[i]);
            }
        }
    }

    public void stopServer(){
        server.stop();
        for(Process process : serversStarted){
            if(process.isAlive()){
                process.destroy();
            }
        }
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
                10,
                TimeUnit.SECONDS
        );
    }

    public void sendLeaderHeartbeat(){
        double total_capacity = 0;
        double total_max_capacity = 100 * servers.size();
        for (int i = 0; i < servers.size(); i++) {
            ServerToServerCommunication serverToServerCommunication = servers.get(i);
            long capacity = serverToServerCommunication.sendLeaderHeartbeat();
            capacities.set(i,capacity);
            total_capacity += capacity;
        }
        System.out.println("Capacities: " + capacities);
        System.out.println("Calculation: " + total_capacity/total_max_capacity);
        if(total_capacity/total_max_capacity < 0.2){
            try {
                startNewServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<Quiz> getAllQuizList(){
        return serverBackend.getAllQuizList();
    }

    public long getServerCapacity(){
        return serverBackend.getCapacity();
    }

    public quizMessage getQuizFromCorrectServer(String id){
        for (int index: quizToServersMap.get(id)) {
            try {
                ServerToServerCommunication serverToServerCommunication = servers.get(index);
                return serverToServerCommunication.getQuiz(id);
            }catch (Exception e){
                System.err.println("Server" + index + " failed getting quiz");
            }
        }
        return null;
    }

    public void joinNetwork(){
        servers.getLast().joinNetwork(server.getIp(), server.getPort());
    }

    public void setIamLeader(){
        startLeaderHeartbeat();
    }

    public static void main(String[] args) throws Exception {
        Manager manager = new Manager(0, 100,false);
        manager.blockUntilShutdown();
    }
}
