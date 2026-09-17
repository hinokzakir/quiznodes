package com.quiznodes.server;

import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;

import java.util.HashMap;

public class serverBackend {
    HashMap<String, Quiz> quizzes;
    public serverBackend() {}
    public void addQuiz(String name, Quiz quiz){
        quizzes.put(name, quiz);
    }
    public void addQuestionToQuiz(String name, String question, String answer){
        quizzes.get(name).addQuestion(new Question(question, answer));
    }
    public Quiz getQuiz(String name){
        return quizzes.get(name);
    }

    public void deleteQuiz(String name){
        quizzes.remove(name);
    }
}
