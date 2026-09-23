package com.quiznodes.server;

import com.quiznodes.namingservice.NameService;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerNode {
    private String ip;
    private int port;
    private String name;
    private Server server;
    private ServerBackend backend;
    private NameServiceHandler nameServiceHandler;
    private NameService nameService;


    public ServerNode(int port) {
        this.ip = "localhost";
        this.port = port;
        this.name = ip + ":" + port;
        this.backend = new ServerBackend();
        this.server = ServerBuilder.forPort(port)
                .addService(new ServiceHandler(backend))
                .build();
        this.nameServiceHandler = new NameServiceHandler();
    }

    public void start() throws Exception {
        server.start();
        this.nameService = nameServiceHandler.connectNamingService();
        if(nameService != null){
            if (nameServiceHandler.nameServiceAvailable) {
            try {
                nameService.registerNode(name, ip, port);
                System.out.println(
                        "Registered with Name Service: [Name]: " + name + ", [IP]: " + ip + ", [Port]: " + port);

                // Initial resolve + connect.
                nameServiceHandler.syncAndConnectNodes(name);
            } catch (Exception e) {
                System.err.println("Error during name service registration/resolution: " + e.getMessage());
                e.printStackTrace();
            }
        }
        }
        
    }

    public void stop() {
        if (server != null) {
            if(nameService != null){
                try {
                    nameService.unregisterNode(name);
                } catch (Exception e) {
                    System.err.println("Error during name service unregistration: " + e.getMessage());
                    e.printStackTrace();
                }
            }
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
