package com.quiznodes.debug;

import com.quiznodes.proto.debugHeartbeatResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    public void sendHeartbeat(){
        for (String id : servers.keySet()){
            DebugCommunication server =  servers.get(id);
            debugHeartbeatResponse response = server.sendHeartbeat();
            capacities.put(id, response.getCapacity());
            if(response.getLeader()){
                leader = id;
            }
        }
    }
}
