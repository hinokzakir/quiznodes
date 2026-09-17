package com.quiznodes;

import com.quiznodes.client.clientNode;

public class Main {
    public static void main(String[] args) {
        clientNode client = new clientNode("localhost", 50051);
        String response = client.testRequestString("FAAAAHHHH");
        System.out.println(response);
    }
}
