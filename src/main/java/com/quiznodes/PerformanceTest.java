package com.quiznodes;

import com.quiznodes.client.ClientNode;
import com.quiznodes.server.types.Quiz;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PerformanceTest {
    private final ClientNode node;
    private final PrintWriter logWriter;

    private PerformanceTest(ClientNode node, String logFile) {
        this.node = node;

        //Initializes the file to store the performance logs
        try {
            File logDirectory = new File("logfiles");
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }

            logWriter = new PrintWriter(new FileWriter(
                    new File(logDirectory, logFile)
            ));

            logWriter.println("nrMessages,latency,throughput,bandwidth,nrNodes,payloadSize");

        } catch (IOException e) {
            throw new RuntimeException("Could not create log file", e);
        }
    }

    /*
     * A simple performance test that uses the retrieve function
     * to test response time, throughput and bandwidth.
     * The results are written to a log file
     */
    private void testQuizRetrieval(int nrMessages, int payloadSize, int nrNodes) {

        node.deleteAllQuizzes();

        for (int i = 0; i < nrMessages; i++) {
            Quiz quiz = new Quiz(payloadSize);
            node.sendQuiz(quiz);
        }

        long t1 = System.currentTimeMillis();
        List<Quiz> quizzes = node.getAllQuiz();
        long t2 = System.currentTimeMillis();

        System.out.println("retrieved " + quizzes.size() + " quizzes in " + (t2 - t1) + " ms (sequential)");

        long latency = t2 - t1;



        double throughput = calculateThroughput(nrMessages, latency);


        double bandwidth = calculateBandwidth(nrMessages, payloadSize, latency);


        logWriter.println(
                nrMessages + "," +
                        latency + "," +
                        throughput + "," +
                        bandwidth + "," +
                        nrNodes + "," +
                        payloadSize
        );
        logWriter.flush();
        node.deleteAllQuizzes();
    }

    private double calculateBandwidth(int nrMessages, int payloadSize, long timeMs) {
        long totalBytes = (long) nrMessages * payloadSize;
        return totalBytes / (timeMs / 1000.0);
    }

    private double calculateThroughput(int nrMessages, long timeMs) {
        return nrMessages / (timeMs / 1000.0);
    }

    private void close() {
        logWriter.close();
    }

    /*
     * Runs to performance test for both messenger implementations.
     * Runs the test using different payload sizes and amounts of messages
     * Repeats the test 7 times
     */
    public static void run(String host, int port) {
        System.out.println("Starting performance test...");
        for(int i = 0; i < 7; i++) {
            ClientNode node = new ClientNode(host, port);
            PerformanceTest test = new PerformanceTest(node, "amount_scaling.log" + i);

            int[] nrMessages = {10, 100, 250, 500, 1000};
            int[] sizes = {64, 1024, 10240, 102400};
            int[] nrNodes = {3,5,10,50,100};

            for (int nr : nrMessages) {
                test.testQuizRetrieval(nr, 1024, 3);
            }

            test.close();

            test = new PerformanceTest(node, "size_scaling.log" + i);

            for (int size : sizes) {
                test.testQuizRetrieval(100, size, 3);
            }

            test.close();

            test = new PerformanceTest(node, "node_scaling.log" + i);

            for (int nr : nrNodes) {
                Scanner scanner = new Scanner(System.in);

                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                test.testQuizRetrieval(100, 1024, nr);
            }

            test.close();

        }
        System.out.println("Performance test finished.");
    }
}
