package com.quiznodes.server.types;

import java.util.UUID;

public class Question {
    private String id;
    private final String question;
    private final String answer;
    public Question(String question, String answer) {
        this.id = UUID.randomUUID().toString();
        this.question = question;
        this.answer = answer;
    }
    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getId() {
        return id;
    }
}
