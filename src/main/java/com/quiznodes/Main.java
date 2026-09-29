package com.quiznodes;

import com.quiznodes.client.ClientNode;
import com.quiznodes.server.types.Quiz;

import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ClientNode client = new ClientNode("localhost", 2221);
        String response = client.testRequestString("FAAAAHHHH");
        System.out.println(response);

        // test send quiz
        for (int i = 0; i < 1;i++) {
            Quiz quiz = new Quiz();
            String sendQuizResponse = client.sendQuiz(quiz);
            System.out.println("Sent Quiz: " + sendQuizResponse);
            Quiz getQuiz = client.getQuiz(sendQuizResponse);
            System.out.println("Found quiz: " + getQuiz.getId());
        }
    }
}
