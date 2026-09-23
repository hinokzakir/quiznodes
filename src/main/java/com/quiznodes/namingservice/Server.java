package com.quiznodes.namingservice;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Server implements NameService {

    private final Map<String, Map<String, Integer>> serverMap = new ConcurrentHashMap<>();
    private final static int port = 1099;

    @Override
    public Map<String, Map<String, Integer>> resolveGroup() {
        // return all server nodes ip + port pairs
        return new ConcurrentHashMap<>(serverMap);
    }

    @Override
    public void registerNode(String name, String ip, int port) {
        Map<String, Integer> nodeInfo = new ConcurrentHashMap<>();
        nodeInfo.put(ip, port);
        serverMap.put(name, nodeInfo);
        System.out.println("Registered node '" + name + "' with ip " + ip + " and port " + port);

        // print all registered nodes
        System.out.println("Current registered nodes:");
        for (Map.Entry<String, Map<String, Integer>> entry : serverMap.entrySet()) {
            for (Map.Entry<String, Integer> nodeEntry : entry.getValue().entrySet()) {
                System.out.println(" - " + entry.getKey() + " -> " + nodeEntry.getKey() + ":" + nodeEntry.getValue());
            }
        }
    }

    @Override
    public void unregisterNode(String name) {
        serverMap.remove(name);
        System.out.println("Unregistered node '" + name + "'");
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            NameService stub = (NameService) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("QuizNodeNamingService", stub);
            
            System.out.println("====== QuizNode Server Node Namingservice ======");
            System.out.println("Listening on port: " + port);
        } catch (Exception e) {
            System.err.println("Server initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}