package com.quiznodes;

import com.quiznodes.server.Manager;
import java.util.Scanner;

public class ServerApp {
    public static void main(String[] args) {
        final int port = 0;

        try {
            Manager manager = new Manager(port, 100, false);

            // Shutdown hook for graceful exit on Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nStopping server on port " + port + "...");
                manager.stopServer();

            }));


            // Keep the main thread alive waiting for gRPC server termination
            manager.blockUntilShutdown();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
