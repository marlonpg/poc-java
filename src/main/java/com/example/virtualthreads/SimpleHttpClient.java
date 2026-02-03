package com.example.virtualthreads;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple HTTP client to test the Virtual Thread Web Server
 * Sends multiple concurrent requests to demonstrate scalability
 */
public class SimpleHttpClient {
    private static final String SERVER_URL = "http://localhost:8080";
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("===========================================");
        System.out.println("Virtual Thread HTTP Client");
        System.out.println("===========================================");
        System.out.println("Make sure the server is running first!");
        System.out.println("Starting load test...\n");
        
        int numberOfRequests = 100;
        long startTime = System.currentTimeMillis();
        
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        
        // Send multiple concurrent requests
        for (int i = 1; i <= numberOfRequests; i++) {
            int requestId = i;
            executor.submit(() -> sendRequest(requestId));
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("\n===========================================");
        System.out.println("Load Test Results");
        System.out.println("===========================================");
        System.out.println("Total Requests: " + numberOfRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + errorCount.get());
        System.out.println("Total Time: " + duration + "ms");
        System.out.println("Average Time per Request: " + (duration / (double) numberOfRequests) + "ms");
        System.out.println("===========================================");
    }
    
    private static void sendRequest(int requestId) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                successCount.incrementAndGet();
                System.out.println("Request #" + requestId + " - Success (200 OK) [" + 
                                 Thread.currentThread().getName() + "]");
            } else {
                errorCount.incrementAndGet();
                System.err.println("Request #" + requestId + " - Failed with code: " + responseCode);
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            System.err.println("Request #" + requestId + " - Error: " + e.getMessage());
        }
    }
}
