package com.quiznodes;

import com.quiznodes.server.Manager;
import com.quiznodes.server.types.*;

import com.quiznodes.client.ClientNode;
import com.quiznodes.server.ServerNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private Button sendQuizBtn;

    @FXML
    private Button getQuizBtn;

    @FXML
    private Label clientStatusLabel;

    private Manager activeServerManager;
    private ClientNode activeClient;
    private ScheduledExecutorService heartbeatScheduler;

    @FXML
    private void handleStartServer() {
        if (activeServerManager != null) {
            try {
                activeServerManager.stopServer();
            } catch (Exception e) {
                System.err.println("Error stopping server: " + e.getMessage());
            }
            activeServerManager = null;
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
                activeServerManager = new Manager();

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
                        setServiceButtonsVisible(true);
                    });
                    startHeartbeat();
                } else {
                    Platform.runLater(() -> {
                        clientStatusLabel.setText("Error: No response from server");
                        connectClientBtn.setDisable(false);
                        setServiceButtonsVisible(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clientStatusLabel.setText("Error: " + (e.getMessage() != null ? e.getMessage() : "Connection failed"));
                    connectClientBtn.setDisable(false);
                    setServiceButtonsVisible(false);
                });
            }
        });
    }

    @FXML 
    private void handleSendQuiz() { 
        if (activeClient == null) {
            handleConnectionLost("Connection Lost (Server Offline)");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String response = activeClient.testRequestString("ping");
                if (response == null || response.isEmpty()) {
                    handleConnectionLost("Connection Lost (Server Offline)");
                } else {
                    Platform.runLater(() -> clientStatusLabel.setText("Status: Send Quiz requested"));
                    Quiz quiz = new Quiz();
                    String sendQuizResponse = activeClient.sendQuiz(quiz);
                    Platform.runLater(() -> clientStatusLabel.setText("Response: " + sendQuizResponse));
                }
            } catch (Exception e) {
                handleConnectionLost("Connection Lost (Server Offline)");
            }
        });
    }

    @FXML 
    private void handleGetQuiz() { 
        if (activeClient == null) {
            handleConnectionLost("Connection Lost (Server Offline)");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String response = activeClient.testRequestString("ping");
                if (response == null || response.isEmpty()) {
                    handleConnectionLost("Connection Lost (Server Offline)");
                } else {
                    Platform.runLater(() -> clientStatusLabel.setText("Status: Get Quiz requested"));
                }
            } catch (Exception e) {
                handleConnectionLost("Connection Lost (Server Offline)");
            }
        });
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "client-heartbeat-thread");
            t.setDaemon(true);
            return t;
        });

        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (activeClient == null) {
                stopHeartbeat();
                return;
            }
            try {
                String response = activeClient.testRequestString("ping");
                if (response == null || response.isEmpty()) {
                    handleConnectionLost("Connection Lost (Server Offline)");
                }
            } catch (Exception e) {
                handleConnectionLost("Connection Lost (Server Offline)");
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
    }

    private void handleConnectionLost(String message) {
        stopHeartbeat();
        activeClient = null;
        Platform.runLater(() -> {
            clientStatusLabel.setText("Status: " + message);
            connectClientBtn.setDisable(false);
            setServiceButtonsVisible(false);
        });
    }

    private void setServiceButtonsVisible(boolean visible) {
        if (sendQuizBtn != null) {
            sendQuizBtn.setVisible(visible);
            sendQuizBtn.setManaged(visible);
        }
        if (getQuizBtn != null) {
            getQuizBtn.setVisible(visible);
            getQuizBtn.setManaged(visible);
        }
    }

    public ClientNode getActiveClient() {
        return activeClient;
    }

    public Manager getActiveServerManager() {
        return activeServerManager;
    }
}
