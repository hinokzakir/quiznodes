package com.quiznodes.client;

import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class QuizController {

    @FXML
    private javafx.scene.control.Label quizTitleLabel;

    @FXML
    private Label progressLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private javafx.scene.control.Label questionLabel;

    @FXML
    private VBox choicesBox;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button submitBtn;

    @FXML
    private Button nextBtn;

    private Quiz quiz;
    private QuizEngine quizEngine;

    private ToggleGroup toggleGroup;

    @FXML
    private void handleSubmit() {
        if (toggleGroup != null && toggleGroup.getSelectedToggle() != null) {
            RadioButton selectedRb = (RadioButton) toggleGroup.getSelectedToggle();
            String selectedAnswer = selectedRb.getText();
            System.out.println("The user picked: " + selectedAnswer);

            if (quizEngine.isCorrect(selectedAnswer, quiz.getQuestions().get(quizEngine.getProgress() - 1))) {
                scoreLabel.setText("Score: " + quizEngine.getScore());
            }
            submitBtn.setVisible(false);
            submitBtn.setManaged(false);

            toggleGroup = new ToggleGroup();
            choicesBox.getChildren().clear();

            feedbackLabel.setText("No more questions");
            questionLabel.setText("Result: " + quizEngine.getScore() + " / " + quizEngine.getProgress());

        }
    }

    @FXML
    private void handleNext() {
        // calculate if correct
        if (toggleGroup != null && toggleGroup.getSelectedToggle() != null) {
            RadioButton selectedRb = (RadioButton) toggleGroup.getSelectedToggle();
            String selectedAnswer = selectedRb.getText();
            System.out.println("The user picked: " + selectedAnswer);

            if (quizEngine.isCorrect(selectedAnswer, quiz.getQuestions().get(quizEngine.getProgress() - 1))) {
                scoreLabel.setText("Score: " + quizEngine.getScore());
            }

            quizEngine.increaseProgress();
            Question q = quiz.getQuestions().get(quizEngine.getProgress() - 1);
            questionLabel.setText(q.getQuestion());
            progressLabel.setText("Question " + quizEngine.getProgress() + " / " + quiz.getQuestions().size());
            toggleGroup = new ToggleGroup();
            choicesBox.getChildren().clear();
            List<String> choices = q.getChoices();

            for (String choice : choices) {
                RadioButton rb = new RadioButton(choice);
                rb.setToggleGroup(toggleGroup);
                rb.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
                rb.setTextFill(Color.web("#ffffff"));
                choicesBox.getChildren().add(rb);
            }

            if (quiz.getQuestions().size() < quizEngine.getProgress() + 1) {
                nextBtn.setVisible(false);
                nextBtn.setManaged(false);
            }

        } else {
            System.out.println("User didn't select an answer!");
            // You can show a warning to the user here
        }
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        this.quizEngine = new QuizEngine(quiz);
        loadInitialState();
    }

    private void loadInitialState() {
        // set lable
        quizTitleLabel.setText(quiz.getName());

        // set question
        Question q = quiz.getQuestions().get(0);
        questionLabel.setText(q.getQuestion());

        // set progress
        progressLabel.setText("Question " + quizEngine.getProgress() + " / " + quiz.getQuestions().size());

        // choices
        List<String> choices = q.getChoices();

        toggleGroup = new ToggleGroup();
        choicesBox.getChildren().clear();

        for (String choice : choices) {
            RadioButton rb = new RadioButton(choice);
            rb.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            rb.setTextFill(Color.web("#ffffff"));
            rb.setToggleGroup(toggleGroup);
            choicesBox.getChildren().add(rb);
        }

        if (quiz.getQuestions().size() > 1) {
            nextBtn.setVisible(true);
            nextBtn.setManaged(true);
        }
    }

}
