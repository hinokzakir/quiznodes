package com.quiznodes.server;

import com.quiznodes.proto.questionMessage;
import com.quiznodes.server.LeaderInfoCommunication;
import com.quiznodes.proto.quizMessage;
import com.quiznodes.proto.responseVoteMessage;
import com.quiznodes.server.types.NodeInfo;
import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;

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
        startLeaderHeartbeat();
    }

    public void setState(int newState) {
        this.state = newState;
    }

    public void startElection() {
        setState(1);
        electionHandler.resetVotes();
        long newTerm = electionHandler.getTerm() + 1;
        electionHandler.setTerm(newTerm);
        electionHandler.setHasVoted(true);
        electionHandler.increaseVote();

        int totalNodes = servers.size();
        int majority = (totalNodes / 2) + 1;

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

        List<Integer> serversToRemove = new ArrayList<>();

        for(int i: servers.keySet()) {
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
                serversToRemove.add(i);
                totalNodes = servers.size();
                majority = (totalNodes / 2) + 1;
            }
        }
        for(int index: serversToRemove) {
            servers.remove(index);
            capacities.remove(index);
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
        Quiz quiz = new Quiz(newId);
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
        // Example: The node with the smallest port number is responsible for spawning
        int myPort = server.getPort();
        for (ServerToServerCommunication peer : servers.values()) {
            if (peer.getPort() < myPort) {
                return false; // A peer has a lower port, let them spawn it
            }
        }
        return true; // I have the lowest port among all active nodes
    }

    public static void main(String[] args) throws Exception {
        Manager manager = new Manager(0, 100, false);
        manager.blockUntilShutdown();
    }
}
