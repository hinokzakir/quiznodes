package com.quiznodes.server.types;

import java.util.UUID;
import java.util.List;

public class Question {
    private String id;
    private final String question;
    private final String correctAnswer;
    private List<String> choices;

    public Question(String question, String correctAnswer, List<String> choices) {
        this.id = UUID.randomUUID().toString();
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.choices = choices;
    }

    public Question(String id, String question, String correctAnswer, List<String> choices) {
        this.id = id;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.choices = choices;
    }

    public String getQuestion() {
        return question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getChoices() {
        return choices;
    }

    public String getId() {
        return id;
    }
}
