package com.quiznodes.debug;

import com.quiznodes.proto.debugHeartbeatResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.quiznodes.server.types.*;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class DebugController {
    private Map<String, DebugCommunication>  servers = new HashMap<>();
    private Map<String, Long> capacities = new HashMap<>();
    private String leader;
    private DebugServer server;
    private ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    @FXML
    private ListView<String> serverListView;


    @FXML
    private void initialize() {
        try {
            server = new DebugServer("localhost", 0, this);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(); // or log it, or show in UI
        }
    }

    @FXML
    private void handleConnect() {
    }

    public void addServer(String ip, int port) {
        servers.put(ip + ":" + port, new DebugCommunication(ip, port));
        serverListView.getItems().add(ip + ":" + port);
        System.out.println("addServer ip:"+ip+":"+port);
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

    public void sendHeartbeat(){
        for (String id : servers.keySet()){
            DebugCommunication server =  servers.get(id);
            debugHeartbeatResponse response = server.sendHeartbeat();
            capacities.put(id, response.getCapacity());
            if(response.getLeader()){
                leader = id;
            }
            System.out.println(id + " Capacity: " + response.getCapacity());
            System.out.println(id + " Leader: " + response.getLeader());
        }
    }
}
