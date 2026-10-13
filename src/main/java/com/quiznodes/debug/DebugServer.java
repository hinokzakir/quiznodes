package com.quiznodes.debug;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class DebugServer {
    private String ip;
    private int port;
    private Server server;
    public DebugServer(String ip, int port, DebugController controller) throws IOException, InterruptedException {
        this.ip = "localhost";
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(new DebugHandler(controller))
                .build();
        this.server.start();
        this.server.awaitTermination();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        DebugServer debugServer =  new DebugServer("localhost", 1112, new DebugController());
    }
}
