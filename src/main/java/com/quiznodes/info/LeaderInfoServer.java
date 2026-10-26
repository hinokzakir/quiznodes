package com.quiznodes.info;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

//Sets up the leader info server
public class LeaderInfoServer {
    private String ip;
    private int port;
    private Server server;
    public LeaderInfoServer(String ip, int port) throws IOException, InterruptedException {
        this.ip = "localhost";
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(new LeaderInfoHandler())
                .build();
        this.server.start();
        System.out.println("Server started, listening on " + this.ip + ":" + this.port);
        this.server.awaitTermination();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        LeaderInfoServer leaderInfoServer = new LeaderInfoServer("localhost", 1337);
    }
}
