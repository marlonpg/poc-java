package com.example.virtualthreads;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

public class VirtualThreadWebServer {
    private static final int PORT = 8080;
    
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        
        System.out.println("===========================================");
        System.out.println("Virtual Thread Web Server Started");
        System.out.println("===========================================");
        System.out.println("Server listening on port: " + PORT);
        System.out.println("Test with: http://localhost:" + PORT);
        System.out.println("Or use: curl http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop the server");
        System.out.println("===========================================\n");
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleRequest(clientSocket));
        }
    }
    
    private static void handleRequest(Socket socket) {
        try (socket;
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String request = in.readLine();
            System.out.println("Request: " + request + 
                             " [Thread: " + Thread.currentThread().getName() + "]");
            
            // Simulate blocking operation (e.g., database query, external API call)
            Thread.sleep(100);
            
            // Send HTTP response
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Connection: close");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Virtual Thread Server</title></head>");
            out.println("<body>");
            out.println("<h1>Hello from Virtual Thread!</h1>");
            out.println("<p>Thread: " + Thread.currentThread().getName() + "</p>");
            out.println("<p>Thread is Virtual: " + Thread.currentThread().isVirtual() + "</p>");
            out.println("<p>Current Time: " + System.currentTimeMillis() + "</p>");
            out.println("</body>");
            out.println("</html>");
            
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
