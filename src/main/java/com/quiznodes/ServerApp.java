package com.quiznodes;

import com.quiznodes.server.Manager;
import java.util.Scanner;

public class ServerApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int selectedPort = 0;
        while (true) {
            System.out.print("Enter port number for Server Node (1-65535): ");
            String input = scanner.nextLine().trim();
            try {
                int parsed = Integer.parseInt(input);
                if (parsed >= 1 && parsed <= 65535) {
                    selectedPort = parsed;
                    break;
                }
                System.out.println("Port number must be between 1 and 65535.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid port number.");
            }
        }
        final int port = selectedPort;

        try {
            Manager manager = new Manager(port, 100, true);

            // Shutdown hook for graceful exit on Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nStopping server on port " + port + "...");
                manager.stopServer();
            }));

            System.out.println("Server node running on port " + port + ". Press Ctrl+C to stop.");

            // Keep the main thread alive waiting for gRPC server termination
            manager.blockUntilShutdown();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
