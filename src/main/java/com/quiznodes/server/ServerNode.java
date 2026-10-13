package com.quiznodes.server;

import com.quiznodes.namingservice.NameService;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerNode {
    private String ip;
    private int port;
    private String name;
    private Server server;
    private NameServiceHandler nameServiceHandler;
    private NameService nameService;
    private Manager manager;
    private boolean started = false;

    public ServerNode(int port, Manager manager) {
        this.ip = "localhost";
        this.port = port;
        this.name = ip + ":" + port;
        this.server = ServerBuilder.forPort(port)
                .addService(new ServiceHandler(manager))
                .addService(new ServerToServerHandler(manager))
                .addService(new DebugHandler(manager))
                .build();
        this.nameServiceHandler = new NameServiceHandler(manager);
        this.manager = manager;
    }

    public void start() throws Exception {
        if (!started) {
            server.start();
            started = true;
        }
        this.port = server.getPort();
        this.name = ip + ":" + port;
        this.nameService = nameServiceHandler.connectNamingService();
        if (nameService != null) {
            if (nameServiceHandler.nameServiceAvailable) {
                try {
                    nameService.registerNode(name, ip, port);
                    manager.addServer(ip, port);
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
            if (nameService != null) {
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

    public void awaitTermination() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }


    /*
     * public static void main(String[] args) throws Exception {
     * int port = 0;
     * ServerNode server = new ServerNode(port);
     * server.start();
     * server.server.awaitTermination();
     * }
     */
}
