package com.quiznodes.server;

import com.quiznodes.server.LeaderInfoCommunication;
import com.quiznodes.proto.quizMessage;
import com.quiznodes.proto.responseVoteMessage;
import com.quiznodes.server.types.NodeInfo;
import com.quiznodes.server.types.Quiz;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Manager {
    private ServerBackend serverBackend;
    private ServerNode server;
    private int state; // 0 = leader , 1 = candidate , 2 = follower
    private List<ServerToServerCommunication> servers = new ArrayList<>();
    private List<Long> capacities = new ArrayList<>();
    private Set<Integer> fullServers = new HashSet<>();
    private Map<String, List<Integer>> quizToServersMap = new HashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private List<Process> serversStarted = new ArrayList<>();
    private LeaderInfoCommunication leaderInfoCommunication;
    public ElectionHandler electionHandler; // under construction
    private ScheduledFuture<?> electionTimeoutTask;

    public Manager(int port, int max_capacity, boolean isLeader) throws Exception {
        serverBackend = new ServerBackend(max_capacity);
        server = new ServerNode(port, this);
        leaderInfoCommunication = new LeaderInfoCommunication("localhost", 1337);
        this.electionHandler = new ElectionHandler(0);
        if (!isLeader) {
            this.state = 2;
            resetElectionTimeout();
        } else {
            setIamLeader();
        }
        server.start();
    }

    public void addServer(String host, int port) {
        servers.add(new ServerToServerCommunication(host, port));
        capacities.add(0L);
        System.out.println("Server added" + host + ":" + port);
    }

    public void startNewServer() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                "com.quiznodes.ServerApp");

        processBuilder.inheritIO();
        System.out.println(processBuilder.directory());

        Process process = processBuilder.start();
        serversStarted.add(process);
    }

    public Quiz getQuiz(String id) {
        return serverBackend.getQuiz(id);
    }

    public void addQuiz(String name, Quiz quiz) {
        serverBackend.addQuiz(name, quiz);
    }

    public void replicateQuiz(Quiz quiz) {
        int replicationFactor;
        int nrServers = servers.size() - fullServers.size();
        if (nrServers == 1) {
            replicationFactor = 1;
        } else if (nrServers < 4) {
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
        indicesIndex = 0;
        int replicationIndex = 0;
        while(replicationIndex < replicationFactor && indicesIndex < indices.length){
            long capacity = servers.get(indices[indicesIndex]).replicateQuiz(quiz);
            if(capacity > -1) {
                quizToServersMap.get(quiz.getId()).add(indices[indicesIndex]);
                if (capacity < 1) {
                    fullServers.add(indices[indicesIndex]);
                }
                replicationIndex++;
            }
            indicesIndex++;
        }
    }

    public void stopServer() {
        server.stop();
        for (Process process : serversStarted) {
            if (process.isAlive()) {
                process.destroy();
            }
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void startLeaderHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendLeaderHeartbeat,
                0,
                5,
                TimeUnit.SECONDS);
    }

    public void sendLeaderHeartbeat() {
        double total_capacity = 0;
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            ServerToServerCommunication serverToServerCommunication = servers.get(i);
            long capacity = serverToServerCommunication.sendLeaderHeartbeat();
            if (capacity > -1){
                capacities.set(i, capacity);
                total_capacity += capacity;
            }else {
                toRemove.add(i);
                System.out.println("Should remove" + i);
            }
        }
        toRemove.sort(Comparator.reverseOrder());
        for (int i : toRemove) {
            System.out.println("Removing server" + i);
            servers.get(i).shutdown();
            servers.remove(i);
            capacities.remove(i);
            fullServers.remove(i);
            for(List<Integer> list : quizToServersMap.values()){
                list.remove(Integer.valueOf(i));
            }
        }
        double total_max_capacity = 100 * servers.size();
        System.out.println("Capacities: " + capacities);
        System.out.println("Calculation: " + total_capacity / total_max_capacity);
        if (total_capacity / total_max_capacity < 0.2) {
            try {
                startNewServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<Quiz> getAllQuizList() {
        return serverBackend.getAllQuizList();
    }

    public long getServerCapacity() {
        return serverBackend.getCapacity();
    }

    public quizMessage getQuizFromCorrectServer(String id) {
        for (int index : quizToServersMap.get(id)) {
            try {
                ServerToServerCommunication serverToServerCommunication = servers.get(index);
                return serverToServerCommunication.getQuiz(id);
            } catch (Exception e) {
                System.err.println("Server" + index + " failed getting quiz");
            }
        }
        return null;
    }

    public void joinNetwork() {
        servers.getLast().joinNetwork(server.getIp(), server.getPort());
    }

    public void setIamLeader() {
        this.state = 0;
        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancel(false);
        }
        leaderInfoCommunication.addLeader(server.getIp(), server.getPort());
        startLeaderHeartbeat();
    }

    public void setState(int newState) {
        this.state = newState;
    }

    public void startElection() {
        setState(1);
        long newTerm = electionHandler.getTerm() + 1;
        electionHandler.setTerm(newTerm);
        electionHandler.setHasVoted(true);
        electionHandler.increaseVote();

        int totalNodes = servers.size();
        int majority = (totalNodes / 2) + 1;

        if (electionHandler.getVotes() >= majority) {
            setIamLeader();
            System.out.println("I am new LEADER!!!!!!!");
            return;
        }

        for(int i = 0; i < servers.size(); i++) {
            if (this.state != 1) {
                return;
            }
            ServerToServerCommunication peer = servers.get(i);
            try {
                System.out.println("Sending RequestVote...");
                responseVoteMessage response = peer.requestVote(server.getIp(), server.getPort(), newTerm);
                System.out.println("Vote result: " + response.getVoteGranted() + " (term: " + response.getTerm() + ")");
                // cancel election if current term is to low
                if (response.getTerm() > electionHandler.getTerm()) {
                    setState(2);
                    electionHandler.setTerm(response.getTerm());
                    electionHandler.setHasVoted(false);
                    return;
                }

                if (response.getVoteGranted() && state == 1) {
                    electionHandler.increaseVote();

                    if (electionHandler.getVotes() >= majority) {
                        setIamLeader();
                        System.out.println("I am new LEADER!!!!!!!");
                        return;
                    }
                }
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted() || this.state != 1) {
                    return;
                }
                System.err.println("RequestVote failed (removing dead peer): " + e.getMessage());
                servers.remove(i);
                capacities.remove(i);
            }
        }
    }

    public synchronized void resetElectionTimeout() {
        // cancel timeout if active
        if (electionTimeoutTask != null && !electionTimeoutTask.isDone()) {
            electionTimeoutTask.cancel(true);
        }

        if (this.state != 0) {
            int timeoutSeconds = 12 + new Random().nextInt(5); // gives random timeout 12-16s
            electionTimeoutTask = heartbeatScheduler.schedule(() -> {
                System.out.println("Heartbeat timeout reached! Starting leader election...");
                startElection();
            }, timeoutSeconds, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) throws Exception {
        Manager manager = new Manager(0, 100, false);
        manager.blockUntilShutdown();
    }
}
