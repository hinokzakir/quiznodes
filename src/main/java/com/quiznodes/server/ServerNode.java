package com.quiznodes.server;

import com.quiznodes.namingservice.NameService;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.net.Inet4Address;
import java.net.UnknownHostException;

//Creates the server for the server node
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

        try {
            this.ip = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
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
                    manager.sendToDebugger(ip, port);
                    System.out.println(
                            "Registered with Name Service: [Name]: " + name + ", [IP]: " + ip + ", [Port]: " + port);

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


}
