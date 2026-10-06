package com.quiznodes.server.types;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Quiz {
    private String id;
    private String name;
    private List<Question> questions;

    public Quiz() {
        this.id = UUID.randomUUID().toString();
        questions = new ArrayList<>();
    }

    public Quiz(String id, String name) {
        this.id = id;
        this.name = name;
        questions = new ArrayList<>();
    }

    public Quiz(String id, String name, List<Question> questions) {
        this.questions = questions;
        this.name = name;
        this.id = id;
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

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.name + " (" + questions.size() + " questions)";
    }
}
