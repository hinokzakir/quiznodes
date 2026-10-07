package com.quiznodes.server;

import com.quiznodes.proto.*;
import com.quiznodes.server.LeaderInfoCommunication;
import com.quiznodes.server.types.NodeInfo;
import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;
//import com.quiznodes.server.types.ServerInfo;
import com.quiznodes.server.types.ServerInfo;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Manager {
    private ServerBackend serverBackend;
    private ServerNode server;
    private int state; // 0 = leader , 1 = candidate , 2 = follower
    private Map<Integer, ServerToServerCommunication> servers = new HashMap<>();
    private Map<Integer,Long> capacities = new HashMap<>();
    private Set<Integer> fullServers = new HashSet<>();
    private Map<String, List<Integer>> quizToServersMap = new HashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private List<Process> serversStarted = new ArrayList<>();
    private LeaderInfoCommunication leaderInfoCommunication;
    public ElectionHandler electionHandler; // under construction
    private ScheduledFuture<?> electionTimeoutTask;
    private static AtomicInteger nextServerId = new AtomicInteger(0);
    boolean startUpFlag = false;

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
        int id = nextServerId.getAndIncrement();
        servers.put(id,new ServerToServerCommunication(host, port));
        capacities.put(id, 0L);
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
        List<Integer> indices = new ArrayList<>();
        for(int id: servers.keySet()) {
            if(!fullServers.contains(id)) {
                indices.add(id);
            }
        }

        indices.sort((a, b) -> Long.compare(capacities.get(b), capacities.get(a)));
        quizToServersMap.put(quiz.getId(), new ArrayList<>());
        int replicationIndex = 0;
        for(int id: indices) {
            long capacity = servers.get(id).replicateQuiz(quiz);
            if(capacity > -1) {
                quizToServersMap.get(quiz.getId()).add(id);
                if (capacity < 1) {
                    fullServers.add(id);
                }
                replicationIndex++;
            }
            if(replicationIndex == replicationFactor) {
                break;
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
        List<Integer> toRemove = new ArrayList<>();
        for (int i: servers.keySet()) {
            ServerToServerCommunication serverToServerCommunication = servers.get(i);
            long capacity = serverToServerCommunication.sendLeaderHeartbeat();
            if (capacity > -1){
                capacities.put(i, capacity);
                total_capacity += capacity;
            }else {
                toRemove.add(i);
                System.out.println("Should remove" + i);
            }
        }
        toRemove.sort(Comparator.reverseOrder());
        if(servers.size() - toRemove.size() < 3 && startUpFlag){
            try {
                startNewServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if(servers.size() - toRemove.size() >= 3){
            startUpFlag = true;
        }
        for (int i : toRemove) {
            System.out.println("Removing server" + i);
            servers.get(i).shutdown();
            servers.remove(i);
            capacities.remove(i);
            fullServers.remove(i);
            redistributeRemovedServerQuizzes(i, toRemove);
            for(List<Integer> list : quizToServersMap.values()){
                list.remove(Integer.valueOf(i));
            }
        }
        if (servers.isEmpty()) return;
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

    public void sendLeaderHeartbeatFirst() {
        System.out.println("Sending my list of nodes, amount: " + servers.size());
        double total_capacity = 0;
        double total_max_capacity = 100 * servers.size();
        List<serverInfo> serverInfoList = new ArrayList<>();
        for(ServerToServerCommunication s : servers.values()){
            System.out.println(s.getPort());
            serverInfo server = serverInfo.newBuilder().setIp(s.getHost()).setPort(s.getPort()).build();
            serverInfoList.add(server);
        }
        for (int i: servers.keySet()) {
            ServerToServerCommunication serverToServerCommunication = servers.get(i);
            long capacity = serverToServerCommunication.sendLeaderHeartbeatFirst(serverInfoList);
            capacities.put(i, capacity);
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
        System.out.println(servers.keySet());
        System.out.println(nextServerId.intValue());
        servers.get(nextServerId.get() - 1).joinNetwork(server.getIp(), server.getPort());
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

        for(int i: new ArrayList<>(servers.keySet())) {
            if (this.state != 1) {
                return;
            }
            ServerToServerCommunication peer = servers.get(i);
            try {
                System.out.println("Sending RequestVote...");
                responseVoteMessage response = peer.requestVote(server.getIp(), server.getPort(), newTerm);
                System.out.println("Vote result: " + response.getVoteGranted() + " (term: " + response.getTerm() + ")");
                // cancel election if current term is too low
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
                }
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted() || this.state != 1) {
                    return;
                }
                System.err.println("RequestVote failed (removing dead peer): " + e.getMessage());
                servers.get(i).shutdown();
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
        if (electionHandler.getVotes() >= majority) {
            setIamLeader();
            System.out.println("I am new LEADER!!!!!!!");
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
    public void redistributeRemovedServerQuizzes(int serverIndex, List<Integer> serversToRemove) {
        for(String quizId: quizToServersMap.keySet()){
            List<Integer> serversToNotReplicateTo = quizToServersMap.get(quizId);
            if(serversToNotReplicateTo.contains(serverIndex)) {
                int serverToGetQuizFrom = -1;
                for (int server : serversToNotReplicateTo) {
                    if (!serversToRemove.contains(server)) {
                        serverToGetQuizFrom = server;
                        break;
                    }
                }
                if (serverToGetQuizFrom == -1) {
                    System.out.println("Quiz Lost: " + quizId);
                } else {
                    Quiz quiz = buildQuiz(servers.get(serverToGetQuizFrom).getQuiz(quizId));

                    List<Integer> indices = new ArrayList<>();
                    for (int i : servers.keySet()) {
                        if (!fullServers.contains(i) && !serversToRemove.contains(i) && !serversToNotReplicateTo.contains(i)) {
                            indices.add(i);
                        }
                    }

                    if(indices.isEmpty()) {
                        System.out.println("No server to replicate quiz to :" + quizId);
                        continue;
                    }

                    indices.sort((a, b) -> Long.compare(capacities.get(b), capacities.get(a)));
                    int serverToReplicateTo = indices.getFirst();
                    quizToServersMap.get(quizId).add(serverToReplicateTo);
                    servers.get(serverToReplicateTo).replicateQuiz(quiz);
                }
            }
        }
    }

    private Quiz buildQuiz (quizMessage response){
        String newId = response.getId();
        String name = response.getName();
        Quiz quiz = new Quiz(newId, name);
        for (questionMessage q : response.getQuestionsList()) {
            String qid = q.getId();
            String correct = q.getCorrectAnswer();
            List<String> choices = q.getChoicesList();
            Question question = new Question(qid, correct, choices);
            quiz.addQuestion(question);
        }
        return quiz;
    }

    private boolean isDesignatedSpawner() {
        // test for deploying new server node
        int myPort = server.getPort();
        for (ServerToServerCommunication peer : servers.values()) {
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
            String key = ip + ":" + port;
            leaderServerKeys.add(key);

            boolean exists = false;
            for(ServerToServerCommunication peer : servers.values()) {
                if (peer.getHost().equals(ip) && peer.getPort() == port){
                    exists = true;
                    break;
                }
            }

            if(!exists){
                addServer(ip, port);
            }
        }

        List<Integer> toRemove = new ArrayList<>();

        for (int id:servers.keySet()) {
            ServerToServerCommunication peer = servers.get(id);
            String peerKey = peer.getHost() + ":" + peer.getPort();

            if (!leaderServerKeys.contains(peerKey)) {
                System.out.println("Removing obsolete server connection: " + peerKey);
                toRemove.add(id);
            }
        }

        for (int id : toRemove) {
            servers.get(id).shutdown();
            servers.remove(id);
            capacities.remove(id);
        }
    }

    public int getState() {
        return state;
    }

    public List<quizList> getAllQuizzesFromAllServers(){
        List<quizList> quizLists = new ArrayList<>();
        for (ServerToServerCommunication peer : servers.values()) {
            try {
                quizLists.add(peer.getAllQuizzes());
            } catch (Exception e) {
                System.out.println("Skipping getting quizzes from: " + peer.getHost() + ":" + peer.getPort());
            }
        }
        return quizLists;
    }

    public static void main(String[] args) throws Exception {
        Manager manager = new Manager(0, 100, false);
        manager.blockUntilShutdown();
    }
}
