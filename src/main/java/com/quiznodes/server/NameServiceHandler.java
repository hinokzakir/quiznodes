package com.quiznodes.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.quiznodes.namingservice.*;;

public class NameServiceHandler {
    private NameService nameService;
    public boolean nameServiceAvailable = false;
    public final Set<String> discoveredNodes = ConcurrentHashMap.newKeySet();
    private Manager manager;

    public NameServiceHandler(Manager manager) {
        this.manager = manager;
    }

    public NameService connectNamingService() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            this.nameService = (NameService) registry.lookup("QuizNodeNamingService");
            System.out.println("Connected to Name Service at localhost:1099");
            nameServiceAvailable = true;
            return nameService;
        } catch (Exception e) {
            System.out.println("Name Service not available at localhost:1099");
            return null;
        }
    }

    public void syncAndConnectNodes(String name) throws Exception {
        Map<String, Map<String, Integer>> nodes = nameService.resolveGroup();
        if (nodes.size() == 1) {
            System.out.println("I am alone. I am leader");
            manager.setIamLeader();
        } else {
            manager.setState(2); // follower
        }
        for (Map.Entry<String, Map<String, Integer>> entry : nodes.entrySet()) {
            String nodeName = entry.getKey();
            if (nodeName.equals(name)) {
                continue;
            }

            for (Map.Entry<String, Integer> nodeEntry : entry.getValue().entrySet()) {
                String nodeIp = nodeEntry.getKey();
                int nodePort = nodeEntry.getValue();
                String nodeId = nodeName + "@" + nodeIp + ":" + nodePort;

                if (!discoveredNodes.add(nodeId)) {
                    continue;
                }

                manager.addServer(nodeIp, nodePort);
                manager.joinNetwork();
                System.out.println("Connecting to node " + nodeName + " at " + nodeIp + ":" + nodePort);
                // this is when i we know how to connect server nodes
                // boolean ok = connect(nodeIp, nodePort);
                // System.out.println("Connect to " + nodeName + " -> " + (ok ? "OK" :
                // "FAILED"));
            }
        }
    }

    public void registerNode(String name, String ip, int port) {
        if (nameServiceAvailable) {
            try {
                // Initial resolve + connect.
                nameService.registerNode(name, ip, port);
                System.out.println(
                        "Registered with Name Service: [Name]: " + name + ", [IP]: " + ip + ", [Port]: " + port);
                syncAndConnectNodes(name);
            } catch (Exception e) {
                System.err.println("Error during name service registration/resolution: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
