package com.quiznodes.server;

import com.quiznodes.server.LeaderInfoCommunication;
import com.quiznodes.proto.quizMessage;
import com.quiznodes.proto.responseVoteMessage;
import com.quiznodes.proto.serverInfo;
import com.quiznodes.server.types.NodeInfo;
import com.quiznodes.server.types.Quiz;
//import com.quiznodes.server.types.ServerInfo;
import com.quiznodes.server.types.ServerInfo;

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
        for (int i = 0; i < replicationFactor; i++) {
            long capacity = servers.get(indices[i]).replicateQuiz(quiz);
            quizToServersMap.get(quiz.getId()).add(indices[i]);
            if (capacity < 1) {
                fullServers.add(indices[i]);
            }
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
        double total_max_capacity = 100 * servers.size();
        for (int i = 0; i < servers.size(); i++) {
            ServerToServerCommunication serverToServerCommunication = servers.get(i);
            try {
                long capacity = serverToServerCommunication.sendLeaderHeartbeat();
                capacities.set(i, capacity);
                total_capacity += capacity;
            } catch (Exception e) {
                System.err.println("Failed to heartbeat peer (removing): " + e.getMessage());
                servers.remove(i);
                capacities.remove(i);
                total_max_capacity = 100 * servers.size();
            }
        }
        if (servers.isEmpty()) return;
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

    public void sendLeaderHeartbeatFirst() {
        System.out.println("Sending my list of nodes, amount: " + servers.size());
        double total_capacity = 0;
        double total_max_capacity = 100 * servers.size();
        List<serverInfo> serverInfoList = new ArrayList<>();
        for(ServerToServerCommunication s : servers){
            serverInfo server = serverInfo.newBuilder().setIp(s.getHost()).setPort(s.getPort()).build();
            serverInfoList.add(server);
        }
        for (int i = 0; i < servers.size(); i++) {
            ServerToServerCommunication serverToServerCommunication = servers.get(i);
            long capacity = serverToServerCommunication.sendLeaderHeartbeatFirst(serverInfoList);
            capacities.set(i, capacity);
            total_capacity += capacity;
        }
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
        sendLeaderHeartbeatFirst();
        startLeaderHeartbeat();
    }

    public void setState(int newState) {
        this.state = newState;
    }

    public void startElection() {
        resetElectionTimeout();
        setState(1);
        electionHandler.resetVotes();
        long newTerm = electionHandler.getTerm() + 1;
        electionHandler.setTerm(newTerm);
        electionHandler.setHasVoted(true);
        electionHandler.increaseVote();

        int totalNodes = servers.size();
        int majority;
        majority = (totalNodes / 2) + 1;

        System.out.println("Starting election term " + newTerm + ", votes: " 
            + electionHandler.getVotes() + "/" + majority);

        if(totalNodes <= 2) {
            System.out.println("Only " + totalNodes + " in network. election impossible...");

            if (isDesignatedSpawner()) {
                try {
                    System.out.println("Spawning new ServerApp");
                    startNewServer();
                } catch (IOException e) {
                    System.err.println("Failed to start new server: " + e.getMessage());
                }
            }
        }

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
                    System.out.println("Got vote from " + peer.getPort() 
                    + ", votes: " + electionHandler.getVotes() + " / " + majority);

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
                totalNodes = servers.size();
                majority = (totalNodes / 2) + 1;
                System.out.println("Total Nodes updated: " + totalNodes);
                if(totalNodes <= 2) {
                    System.out.println("Only " + totalNodes + " in network. election impossible...");

                    if (isDesignatedSpawner()) {
                        try {
                            System.out.println("Spawning new ServerApp");
                            startNewServer();
                        } catch (IOException ioe) {
                            System.err.println("Failed to start new server: " + ioe.getMessage());
                        }
                    }
                }
            }
        }
    }

    public synchronized void resetElectionTimeout() {
        // cancel timeout if active
        if (electionTimeoutTask != null && !electionTimeoutTask.isDone()) {
            electionTimeoutTask.cancel(false);
        }

        if (this.state != 0) {
            int timeoutSeconds = 12 + new Random().nextInt(5); // gives random timeout 12-16s
            electionTimeoutTask = heartbeatScheduler.schedule(() -> {
                System.out.println("Heartbeat timeout reached! Starting leader election...");
                // remove the leader from servers list
                startElection();
            }, timeoutSeconds, TimeUnit.SECONDS);
        }
    }

    private boolean isDesignatedSpawner() {
        // test for deploying new server node
        int myPort = server.getPort();
        for (ServerToServerCommunication peer : servers) {
            if (peer.getPort() < myPort) {
                return false; // A peer has a lower port, let them spawn it
            }
        }
        return true; // I have the lowest port among all active nodes
    }

    public synchronized void updateServerList(List<serverInfo> newServerList) {
        Set<String> leaderServerKeys = new HashSet<>();

        for(serverInfo info : newServerList){
            String ip = info.getIp();
            int port = info.getPort();
            if(ip.equals(server.getIp()) && port == server.getPort()) {
                continue;
            }
            String key = ip + ":" + port;
            leaderServerKeys.add(key);

            boolean exists = false;
            for(ServerToServerCommunication peer : servers) {
                if (peer.getHost().equals(ip) && peer.getPort() == port){
                    exists = true;
                    break;
                }
            }

            if(!exists){
                addServer(ip, port);
            }
        }

        servers.removeIf(peer -> {
                    String peerKey = peer.getHost() + ":" + peer.getPort();
                    boolean keep = leaderServerKeys.contains(peerKey);
                    if (!keep) {
                        System.out.println("Removing obsolete server connection: " + peerKey);
                    }
                    return !keep;
                });
    }

    public int getState() {
        return state;
    }

    public static void main(String[] args) throws Exception {
        Manager manager = new Manager(0, 100, false);
        manager.blockUntilShutdown();
    }
}
