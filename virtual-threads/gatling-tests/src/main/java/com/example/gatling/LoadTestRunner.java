package com.example.gatling;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Load Testing Tool for Virtual Threads
 * Simulates concurrent users and collects metrics
 */
public class LoadTestRunner {
    
    private final String baseUrl;
    private final int numUsers;
    private final int rampUpSeconds;
    private final int testDurationSeconds;
    private final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong totalRequests = new AtomicLong(0);

    public LoadTestRunner(String baseUrl, int numUsers, int rampUpSeconds, int testDurationSeconds) {
        this.baseUrl = baseUrl;
        this.numUsers = numUsers;
        this.rampUpSeconds = rampUpSeconds;
        this.testDurationSeconds = testDurationSeconds;
    }

    public void runTest() throws InterruptedException {
        System.out.println("==========================================");
        System.out.println("Load Test Configuration");
        System.out.println("==========================================");
        System.out.println("Server: " + baseUrl);
        System.out.println("Users: " + numUsers);
        System.out.println("Ramp-up: " + rampUpSeconds + " seconds");
        System.out.println("Duration: " + testDurationSeconds + " seconds");
        System.out.println("==========================================\n");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (testDurationSeconds * 1000L);
        long rampUpEnd = startTime + (rampUpSeconds * 1000L);

        // Ramp up users gradually
        System.out.println("Ramping up users...");
        double usersPerSecond = (double) numUsers / rampUpSeconds;
        int usersStarted = 0;

        long currentTime = startTime;
        while (currentTime < rampUpEnd && usersStarted < numUsers) {
            int usersToAdd = (int) ((currentTime - startTime) / 1000.0 * usersPerSecond) - usersStarted;
            
            for (int i = 0; i < usersToAdd; i++) {
                executor.submit(() -> runUserSession(endTime));
                usersStarted++;
            }
            
            if (usersToAdd > 0) {
                System.out.println("Users started: " + usersStarted + "/" + numUsers);
            }
            
            currentTime = System.currentTimeMillis();
            Thread.sleep(100);
        }

        // Start remaining users
        for (int i = usersStarted; i < numUsers; i++) {
            executor.submit(() -> runUserSession(endTime));
        }

        System.out.println("Ramp-up complete. Maintaining " + numUsers + " users...\n");

        // Wait for all tasks to complete
        executor.shutdown();
        executor.awaitTermination(testDurationSeconds + 60, TimeUnit.SECONDS);

        // Print results
        printResults();
    }

    private void runUserSession(long endTime) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            while (System.currentTimeMillis() < endTime) {
                makeRequest(httpClient);
            }
        } catch (IOException e) {
            failureCount.incrementAndGet();
        }
    }

    private void makeRequest(CloseableHttpClient httpClient) {
        long startTime = System.nanoTime();
        try {
            HttpGet request = new HttpGet(baseUrl + "/");

            httpClient.execute(request, response -> {
                long endTime = System.nanoTime();
                long responseTimeMs = (endTime - startTime) / 1_000_000;
                responseTimes.add(responseTimeMs);

                if (response.getCode() == 200) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                totalRequests.incrementAndGet();
                return null;
            });

            // Small delay between requests
            Thread.sleep(50);
        } catch (Exception e) {
            failureCount.incrementAndGet();
            totalRequests.incrementAndGet();
        }
    }

    private void printResults() {
        System.out.println("\n==========================================");
        System.out.println("Load Test Results");
        System.out.println("==========================================\n");

        int totalReqs = successCount.get() + failureCount.get();
        System.out.println("Total Requests: " + totalReqs);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", (successCount.get() * 100.0 / totalReqs)));
        System.out.println();

        if (!responseTimes.isEmpty()) {
            Collections.sort(responseTimes);
            
            long minTime = responseTimes.get(0);
            long maxTime = responseTimes.get(responseTimes.size() - 1);
            double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long p50 = responseTimes.get((int) (responseTimes.size() * 0.50));
            long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
            long p99 = responseTimes.get((int) (responseTimes.size() * 0.99));

            System.out.println("Response Times (ms):");
            System.out.println("  Min: " + minTime);
            System.out.println("  Max: " + maxTime);
            System.out.println("  Avg: " + String.format("%.2f", avgTime));
            System.out.println("  P50: " + p50);
            System.out.println("  P95: " + p95);
            System.out.println("  P99: " + p99);
            System.out.println();

            double throughput = totalReqs / (testDurationSeconds + rampUpSeconds);
            System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        }

        System.out.println("\n==========================================\n");
    }

    public static void main(String[] args) {
        String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
        int users = Integer.parseInt(System.getProperty("users", "100"));
        int rampUp = Integer.parseInt(System.getProperty("rampUp", "60"));
        int duration = Integer.parseInt(System.getProperty("duration", "300"));

        LoadTestRunner tester = new LoadTestRunner(baseUrl, users, rampUp, duration);
        try {
            tester.runTest();
        } catch (InterruptedException e) {
            System.err.println("Test interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
