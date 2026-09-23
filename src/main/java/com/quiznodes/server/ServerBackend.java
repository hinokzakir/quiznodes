package com.quiznodes.server;

import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerBackend {
    private HashMap<String, Quiz> quizzes;

    public ServerBackend() {
        quizzes = new HashMap<>();
    }

    public void addQuiz(String name, Quiz quiz) {
        quizzes.put(name, quiz);
    }

    public void addQuestionToQuiz(String name, String question, String answer, List<String> choices) {
        quizzes.get(name).addQuestion(new Question(question, answer, choices));
    }

    public Quiz getQuiz(String name) {
        return quizzes.get(name);
    }

    public List<Quiz> getAllQuizList() {
        return new ArrayList<>(quizzes.values());
    } 

    public void deleteQuiz(String name) {
        quizzes.remove(name);
    }
}
