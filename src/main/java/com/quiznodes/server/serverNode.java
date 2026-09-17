package com.quiznodes.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class serverNode {
    private int port;
    private Server server;

    public serverNode(int port) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(new serviceHandler())
                .build();
    }

    public static void main(String[] args) throws Exception {
        int port = 50051;
        serverNode server = new serverNode(port);
        server.server.start();
        System.out.println("Server started on port " + port);
        server.server.awaitTermination();
    }
}
