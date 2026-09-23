package com.quiznodes.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerNode {
    private int port;
    private Server server;
    private ServerBackend backend;

    public ServerNode(int port, Manager manager) {
        this.backend = new ServerBackend();
        this.server = ServerBuilder.forPort(port)
                .addService(new ServiceHandler(manager))
                .addService(new ServerToServerHandler(manager))
                .build();
    }

    public void start() throws Exception {
        server.start();
        port = server.getPort();
        System.out.println("Server started, listening on " + port);
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /*public static void main(String[] args) throws Exception {
        int port = 0;
        ServerNode server = new ServerNode(port);
        server.start();
        server.server.awaitTermination();
    }*/
}
