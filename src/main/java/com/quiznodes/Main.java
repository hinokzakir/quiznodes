package com.quiznodes;

import com.quiznodes.client.ClientNode;
import com.quiznodes.server.types.Quiz;

public class Main {
    public static void main(String[] args) {
        ClientNode client = new ClientNode("localhost", 50051);
        String response = client.testRequestString("FAAAAHHHH");
        System.out.println(response);

        // test send quiz
        Quiz quiz = new Quiz();
        String sendQuizResponse = client.sendQuiz(quiz);
        System.out.println("Sent Quiz: " + sendQuizResponse);
        Quiz getQuiz = client.getQuiz(sendQuizResponse);
        System.out.println("Found quiz: " + getQuiz.getId());
    }
}
