package com.quiznodes.debug;

import com.quiznodes.proto.debugHeartbeatResponse;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.grpc.StatusRuntimeException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;

public class DebugController {
    private Map<String, DebugCommunication> servers = new HashMap<>();
    private Map<String, Long> capacities = new HashMap<>();
    private String leader;
    private DebugServer server;
    private ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private int nrQuizzes = 0;

    @FXML
    private Button shutdownButton;

    @FXML
    private Button spawnButton;

    @FXML
    private ListView<String> serverListView;

    @FXML
    private Label quizCountLabel;

    @FXML
    private Label capacityLabel;

    @FXML
    private void initialize() {
        Thread serverThread = new Thread(() -> {
            try {
                server = new DebugServer("localhost", 1112, this);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        startLeaderHeartbeat();
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @FXML
    private void handleSpawn() {
        DebugCommunication leaderServer = servers.get(leader);
        if (leaderServer != null) {
            leaderServer.spawnServer();
        }
    }

    @FXML
    private void handleShutdown() {
        String selected = serverListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            String id = selected.split("\\s*\\|")[0];
            System.out.println("Selected ID: " + id);
            DebugCommunication server = servers.get(id);
            System.out.println(server);
            server.killServer();
        }

        Platform.runLater(this::refreshList);

    }

    @FXML
    private void handleSleep() {
        String selected = serverListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            String id = selected.split("\\s*\\|")[0];
            System.out.println("Selected ID: " + id);
            DebugCommunication server = servers.get(id);
            try {
                server.sleep();
            } catch (StatusRuntimeException e) {
                System.out.println(id + " sleeping");
                servers.remove(id);
            }
        }

    }

    @FXML
    private void handleShutdownAll(){
        for (DebugCommunication server : servers.values()) {
            server.killServer();
        }
        servers.clear();
        Platform.runLater(this::refreshList);
    }

    @FXML
    private void handleShutdownDebug(){
        int killed = 0;
        for (String id : servers.keySet()) {
            if(!Objects.equals(id, leader)) {
                DebugCommunication server = servers.get(id);
                server.killServer();
                killed++;
                if (killed == 17) {
                    break;
                }
            }
        }
    }

    public void addServer(String ip, int port) {
        String id = ip + ":" + port;
        servers.put(id, new DebugCommunication(ip, port));
        System.out.println("addServer ip:" + id);
        Platform.runLater(() -> serverListView.getItems().add(formatEntry(id, null, false)));
        Thread hbThread = new Thread(() -> {
            try {
                debugHeartbeatResponse response = servers.get(id).sendHeartbeat();
                capacities.put(id, response.getCapacity());
                if (response.getLeader())
                    leader = id;
                Platform.runLater(this::refreshList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        hbThread.setDaemon(true);
        hbThread.start();
    }
    public void startLeaderHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "client-heartbeat-thread");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                0,
                5,
                TimeUnit.SECONDS);
    }

    public void sendHeartbeat() {
        List<String> toRemove =  new ArrayList<>();
        double tot_capacity = 0;
        for (String id : servers.keySet()) {
            try {
                DebugCommunication server = servers.get(id);
                debugHeartbeatResponse response = server.sendHeartbeat();
                capacities.put(id, response.getCapacity());
                tot_capacity += response.getCapacity();
                if (response.getLeader()) {
                    leader = id;
                    nrQuizzes = response.getNrQuizzes();
                }
                System.out.println(id + " Capacity: " + response.getCapacity());
                System.out.println(id + " Leader: " + response.getLeader());
            } catch (StatusRuntimeException e) {
                System.out.println("Removing: " + id);
                toRemove.add(id);
            }
        }
        for (String id : toRemove) {
            servers.remove(id);
            capacities.remove(id);
        }
        double capacity = tot_capacity / (capacities.size() * 100);
        Platform.runLater(()-> capacityLabel.setText("System Capacity: " + capacity));
        Platform.runLater(()-> quizCountLabel.setText("Number of Quizzes: " + nrQuizzes));
        Platform.runLater(this::refreshList);
    }

    private void refreshList() {
        serverListView.getItems().clear();
        for (String id : servers.keySet()) {
            serverListView.getItems().add(formatEntry(id, capacities.get(id), Objects.equals(id, leader)));
        }
    }

    private String formatEntry(String id, Long capacity, boolean leader) {
        String capStr = capacity == null ? "..." : String.valueOf(capacity);
        return id + " | capacity: " + capStr + " | leader: " + leader;
    }
}
