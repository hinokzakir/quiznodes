package com.quiznodes.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.quiznodes.server.types.*;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class DebugController {

    @FXML
    private ListView<String> serverListView;

    private Map<String, DebugCommunication> servers = new HashMap<>();

    private DebugServer server;

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
    }
}
