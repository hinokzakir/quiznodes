package com.quiznodes.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerNode {
    private int port;
    private Server server;
    private ServerBackend backend;

    public ServerNode(int port) {
        this.port = port;
        this.backend = new ServerBackend();
        this.server = ServerBuilder.forPort(port)
                .addService(new ServiceHandler(backend))
                .build();
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 50051;
        ServerNode server = new ServerNode(port);
        server.start();
        System.out.println("Server started on port " + port);
        server.server.awaitTermination();
    }
}
