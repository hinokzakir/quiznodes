package com.quiznodes;

import com.quiznodes.client.ClientNode;

public class Main {
    public static void main(String[] args) {
        ClientNode client = new ClientNode("localhost", 50051);
        String response = client.testRequestString("FAAAAHHHH");
        System.out.println(response);
    }
}
