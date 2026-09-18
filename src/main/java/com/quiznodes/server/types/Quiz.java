package com.quiznodes.server.types;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Quiz {
    private String id;
    private List<Question> questions;
    public Quiz() {
        this.id = UUID.randomUUID().toString();
        questions = new ArrayList<>();
    }

    public Quiz(String id) {
        this.id = id;
        questions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }
    public void addQuestion(Question question) {
        questions.add(question);
    }

    public List<Question> getQuestions() {
        return questions;
    }
}
