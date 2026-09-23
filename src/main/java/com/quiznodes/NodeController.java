package com.quiznodes;

import com.quiznodes.client.ClientNode;
import com.quiznodes.server.ServerNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletableFuture;

public class NodeController {

    @FXML
    private TextField serverPortField;

    @FXML
    private Button startServerBtn;

    @FXML
    private Label serverStatusLabel;

    @FXML
    private TextField clientHostField;

    @FXML
    private TextField clientPortField;

    @FXML
    private Button connectClientBtn;

    @FXML
    private Label clientStatusLabel;

    private ServerNode activeServer;
    private ClientNode activeClient;

    @FXML
    private void handleStartServer() {
        if (activeServer != null) {
            try {
                activeServer.stop();
            } catch (Exception e) {
                System.err.println("Error stopping server: " + e.getMessage());
            }
            activeServer = null;
            serverStatusLabel.setText("Status: Stopped");
            startServerBtn.setText("Start Server");
            serverPortField.setDisable(false);
            return;
        }

        String portText = serverPortField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                serverStatusLabel.setText("Error: Port must be between 1 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            serverStatusLabel.setText("Error: Invalid port number format");
            return;
        }

        serverStatusLabel.setText("Status: Starting server on port " + port + "...");
        startServerBtn.setDisable(true);

        CompletableFuture.runAsync(() -> {
            try {
                ServerNode serverNode = new ServerNode(port);
                serverNode.start();
                activeServer = serverNode;

                Platform.runLater(() -> {
                    serverStatusLabel.setText("Status: Running on port " + port);
                    startServerBtn.setText("Stop Server");
                    startServerBtn.setDisable(false);
                    serverPortField.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    serverStatusLabel.setText("Error: " + e.getMessage());
                    startServerBtn.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void handleConnectClient() {
        String host = clientHostField.getText().trim();
        String portText = clientPortField.getText().trim();

        if (host.isEmpty()) {
            clientStatusLabel.setText("Error: Host address cannot be empty");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                clientStatusLabel.setText("Error: Port must be between 1 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            clientStatusLabel.setText("Error: Invalid port number format");
            return;
        }

        clientStatusLabel.setText("Status: Connecting to " + host + ":" + port + "...");
        connectClientBtn.setDisable(true);

        CompletableFuture.runAsync(() -> {
            try {
                ClientNode clientNode = new ClientNode(host, port);
                String response = clientNode.testRequestString("ping");

                if (response != null && !response.isEmpty()) {
                    activeClient = clientNode;
                    Platform.runLater(() -> {
                        clientStatusLabel.setText("Status: Connected (" + response + ")");
                        connectClientBtn.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        clientStatusLabel.setText("Error: No response from server");
                        connectClientBtn.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clientStatusLabel.setText("Error: " + (e.getMessage() != null ? e.getMessage() : "Connection failed"));
                    connectClientBtn.setDisable(false);
                });
            }
        });
    }

    public ClientNode getActiveClient() {
        return activeClient;
    }

    public ServerNode getActiveServer() {
        return activeServer;
    }
}
