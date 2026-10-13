package com.quiznodes.client;

import com.quiznodes.server.types.*;

public class QuizEngine {
    private Quiz currentQuiz;
    private int progress = 1;
    private int score = 0;
    private int maxScore = 0;

    public QuizEngine(Quiz quiz) {
        currentQuiz = quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.currentQuiz = quiz;
        score = 0;
        maxScore = quiz.getQuestions().size();
    }

    public boolean isCorrect(String choice, Question question) {
        if (choice.equals(question.getCorrectAnswer())) {
            this.score++;
            return true;
        } else {
            return false;
        }
    }

    public void reset() {
        this.score = 0;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void increaseProgress() {
        progress++;
    }

    public int getProgress() {
        return progress;
    }
}
