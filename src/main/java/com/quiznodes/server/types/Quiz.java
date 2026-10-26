package com.quiznodes.server.types;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//Class representing a quiz
public class Quiz {
    private String id;
    private String name;
    private List<Question> questions;
    private String testingPayload;

    public Quiz() {
        this.id = UUID.randomUUID().toString();
        this.name = id;
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

    //For performance testing
    public Quiz(int payloadSize){
        this.testingPayload = "x".repeat(payloadSize);
        this.id = UUID.randomUUID().toString();
        this.name = id;
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

    public String getName() {
        return name;
    }

    public String getTestingPayload() {
        return testingPayload;
    }

    @Override
    public String toString() {
        return this.name + " (" + questions.size() + " questions)";
    }
}
