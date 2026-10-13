package com.quiznodes;

import com.quiznodes.client.LeaderInfoCommunication;
import com.quiznodes.proto.leaderInfoMessage;
import com.quiznodes.server.Manager;
import com.quiznodes.server.types.*;

import com.quiznodes.client.ClientNode;
import com.quiznodes.client.QuizController;
import javafx.fxml.FXMLLoader;
import com.quiznodes.client.JsonConverter;
import com.quiznodes.server.ServerNode;
import io.grpc.StatusRuntimeException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.File;

import java.util.List;

public class NodeController {

    @FXML
    private TextField clientHostField;

    @FXML
    private TextField clientPortField;

    @FXML
    private Button connectClientBtn;

    @FXML
    private Button sendQuizBtn;

    @FXML
    private Button uploadQuizBtn;

    @FXML
    private Button getQuizBtn;

    @FXML
    private VBox quizListCard;

    @FXML
    private ListView<Quiz> quizListView;

    @FXML
    private Label clientStatusLabel;

    private ClientNode activeClient;
    private ScheduledExecutorService heartbeatScheduler;
    private List<Quiz> quizList;
    private LeaderInfoCommunication leaderInfoCommunication;
    private String leaderIp;
    private int leaderPort;

    @FXML
    public void initialize() {

        leaderInfoCommunication = new LeaderInfoCommunication("localhost", 1337);
        leaderInfoMessage response = leaderInfoCommunication.getLeader();
        this.leaderIp = response.getIp();
        this.leaderPort = response.getPort();

        quizListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("Selected item: " + newValue.getId());
                handleListItem(newValue);
            }
        });

    }

    @FXML
    private void handleUploadQuiz(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Quiz JSON File");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(extFilter);

        Window stage = sendQuizBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());

            // Use your JsonConverter to parse the file
            JsonConverter converter = new JsonConverter();
            Quiz newQuiz = converter.convertQuiz(selectedFile);

            if (newQuiz != null) {
                System.out.println("Quiz successfully parsed!");
                CompletableFuture.runAsync(() -> {
                    boolean retry = true;
                    while (retry) {
                        try {
                            String response = activeClient.testRequestString("ping");
                            if (response == null || response.isEmpty()) {
                                handleConnectionLost("Connection Lost (Server Offline)");
                            } else {
                                Platform.runLater(() -> clientStatusLabel.setText("Status: Send Quiz requested"));
                                String sendQuizResponse = activeClient.sendQuiz(newQuiz);
                                Platform.runLater(() -> clientStatusLabel.setText("Response: " + sendQuizResponse));
                                retry = false;
                            }
                        } catch (Exception e) {
                            handleConnectionLost("Connection Lost (Server Offline)");
                        }
                    }
                });
            } else {
                System.err.println("Failed to parse the quiz file.");
            }
        } else {
            System.out.println("File selection cancelled.");
        }

    }

    @FXML
    private void handleConnectClient() {
        CompletableFuture.runAsync(() -> {
            try {
                // this can be removed later, now leader info is updated at button press
                leaderInfoMessage leaderResponse = leaderInfoCommunication.getLeader();
                this.leaderIp = leaderResponse.getIp();
                this.leaderPort = leaderResponse.getPort();

                ClientNode clientNode = new ClientNode(this.leaderIp, this.leaderPort);
                String response = clientNode.testRequestString("ping");

                if (response != null && !response.isEmpty()) {
                    activeClient = clientNode;
                    List<Quiz> fetchedQuizzes = clientNode.getAllQuiz();
                    this.quizList = fetchedQuizzes;

                    Platform.runLater(() -> {
                        clientStatusLabel.setText("Status: Connected (" + response + ")");
                        connectClientBtn.setDisable(false);
                        setServiceButtonsVisible(true);

                        if (quizListView != null && fetchedQuizzes != null) {
                            quizListView.setItems(FXCollections.observableArrayList(fetchedQuizzes));
                        }
                        setListVisible(true);
                    });
                    startHeartbeat();
                } else {
                    Platform.runLater(() -> {
                        clientStatusLabel.setText("Error: No response from server");
                        connectClientBtn.setDisable(false);
                        setServiceButtonsVisible(false);
                        setListVisible(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clientStatusLabel
                            .setText("Error: " + (e.getMessage() != null ? e.getMessage() : "Connection failed"));
                    connectClientBtn.setDisable(false);
                    setServiceButtonsVisible(false);
                    setListVisible(false);
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
            boolean retry = true;
            while (retry) {
                try {
                    String response = activeClient.testRequestString("ping");
                    if (response == null || response.isEmpty()) {
                        handleConnectionLost("Connection Lost (Server Offline)");
                    } else {
                        Platform.runLater(() -> clientStatusLabel.setText("Status: Send Quiz requested"));
                        Quiz quiz = new Quiz();
                        String sendQuizResponse = activeClient.sendQuiz(quiz);
                        Platform.runLater(() -> clientStatusLabel.setText("Response: " + sendQuizResponse));
                        retry = false;
                    }
                } catch (Exception e) {
                    handleConnectionLost("Connection Lost (Server Offline)");
                }
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

    @FXML
    private void handleListItem(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/quiz.fxml"));
            Scene scene = new Scene(loader.load());

            QuizController controller = loader.getController();
            controller.setQuiz(quiz);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle(quiz.getName());
            popupStage.setScene(scene);
            popupStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                } else {
                    List<Quiz> updatedQuizzes = activeClient.getAllQuiz();
                    if (updatedQuizzes != null) {
                        this.quizList = updatedQuizzes;
                        Platform.runLater(() -> {
                            if (quizListView != null) {
                                quizListView.setItems(FXCollections.observableArrayList(updatedQuizzes));
                            }
                        });
                    }
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
        // stopHeartbeat();
        if (activeClient != null) {
            try {
                activeClient.shutdown();
            } catch (Exception ignored) {
            }
            activeClient = null;
        }
        leaderInfoMessage infoMessage = leaderInfoCommunication.getLeader();
        leaderIp = infoMessage.getIp();
        leaderPort = infoMessage.getPort();
        activeClient = new ClientNode(leaderIp, leaderPort);
        Platform.runLater(() -> {
            clientStatusLabel.setText("Status: " + message);
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
        if (uploadQuizBtn != null) {
            uploadQuizBtn.setVisible(visible);
            uploadQuizBtn.setManaged(visible);
        }
    }

    private void setListVisible(boolean visible) {
        if (quizListCard != null) {
            quizListCard.setVisible(visible);
            quizListCard.setManaged(visible);
        }
    }

    public ClientNode getActiveClient() {
        return activeClient;
    }
}
