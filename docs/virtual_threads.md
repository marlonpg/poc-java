# Virtual Threads in Java

## What Are Virtual Threads?

Virtual Threads are lightweight threads managed by the Java Virtual Machine (JVM) that make it significantly easier to write, maintain, and observe high-throughput concurrent applications. They were introduced as a standard feature in **Java 21** (preview in Java 19 and 20).

Virtual Threads are also known as "green threads" or "user-level threads" and represent a fundamental shift in how Java handles concurrency.

## Architecture & Deep Dive: How It Works

### Traditional Platform Threads vs Virtual Threads

**Platform Threads:**
- One-to-one mapping between Java threads and OS threads
- OS threads are expensive resources (typically 1-2 MB of memory per thread)
- Context switching has high overhead
- Operating system scheduler manages thread lifecycle
- Limited scalability due to OS thread limits

**Virtual Threads:**
- Many-to-few mapping: thousands of virtual threads can run on a small number of carrier threads (platform threads)
- Extremely lightweight (~100 bytes each)
- Managed entirely by the JVM (Java scheduler, not OS scheduler)
- Non-blocking scheduling: when a virtual thread blocks, only that thread is suspended, not the carrier thread
- Can run millions of virtual threads on typical hardware

### How Virtual Threads Work

1. **Virtual Thread Creation**: When you create a virtual thread, the JVM schedules it on a `ForkJoinPool`-based scheduler (by default)
2. **Carrier Thread**: Virtual threads are "mounted" on carrier threads (platform threads from a thread pool)
3. **Unmounting on Blocking**: When a virtual thread encounters a blocking operation (I/O, lock acquisition), it's automatically unmounted from its carrier thread
4. **Remounting**: Once the blocking operation completes, the virtual thread is remounted on an available carrier thread
5. **Scheduler**: The JVM maintains a scheduler that efficiently manages which virtual thread runs on which carrier thread

### Memory Model

- **Virtual Thread**: ~100-200 bytes of memory
- **Platform Thread**: 1-2 MB of memory per thread
- Scalability: You can create millions of virtual threads vs thousands of platform threads

## Before Virtual Threads: Traditional Concurrency Patterns

### Pattern 1: Thread Per Request Model
```java
// Traditional approach - one thread per request
public class TraditionalHttpServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            // Create a new thread for each request
            new Thread(() -> handleRequest(clientSocket)).start();
        }
    }
    
    private static void handleRequest(Socket socket) {
        try {
            // Process request
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            // ... handle I/O
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```
**Limitations:**
- Can only handle ~1000-2000 concurrent connections
- High memory consumption
- Thread creation overhead

### Pattern 2: Thread Pool Pattern
```java
// Using ExecutorService with fixed thread pool
ExecutorService executor = Executors.newFixedThreadPool(100);

for (int i = 0; i < 10000; i++) {
    executor.submit(() -> {
        // Do work
        performBlockingOperation();
    });
}
```
**Limitations:**
- Limited by pool size
- Queue building up if more tasks than threads
- Still memory-intensive

### Pattern 3: Async/Reactive Pattern
```java
// Using CompletableFuture or Reactive frameworks
CompletableFuture.supplyAsync(() -> {
    return fetchData();
})
.thenApply(data -> processData(data))
.thenAccept(result -> sendResponse(result));
```
**Limitations:**
- Complex callback chains
- Harder to debug
- Steep learning curve

## Virtual Threads: The New Approach

### Simple Synchronous Code (Just Works!)
```java
// Virtual threads allow simple, synchronous code
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

for (int i = 0; i < 100000; i++) {
    executor.submit(() -> {
        // Simple blocking code
        performBlockingOperation();
        // No callbacks, no complexity
    });
}

executor.shutdown();
executor.awaitTermination(1, TimeUnit.HOURS);
```

### Creating and Running Virtual Threads

```java
// Method 1: Using Thread.ofVirtual()
Thread vThread = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> {
        System.out.println("Running in virtual thread: " + Thread.currentThread());
        performWork();
    });

vThread.join();

// Method 2: Using Executor
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

executor.submit(() -> {
    System.out.println("Task running in virtual thread");
    performBlockingIO();
});

executor.shutdown();
```

### Handling Blocking Operations

```java
// Virtual threads handle blocking operations transparently
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

for (int i = 0; i < 1000000; i++) {
    executor.submit(() -> {
        try {
            // This blocking operation unmounts the virtual thread
            Thread.sleep(1000); // I/O operation
            
            // Virtual thread automatically remounted after sleep
            System.out.println("Completed: " + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });
}

executor.shutdown();
executor.awaitTermination(2, TimeUnit.HOURS);
```

## Pros and Cons

### Pros ✅

| Advantage | Description |
|-----------|-------------|
| **Massive Scalability** | Run millions of virtual threads instead of thousands of platform threads |
| **Simplicity** | Write straightforward synchronous code instead of async/reactive complexity |
| **Memory Efficiency** | ~100 bytes per virtual thread vs ~1-2 MB per platform thread |
| **Easier Debugging** | Thread stack traces are cleaner and easier to understand |
| **Better Resource Utilization** | Carrier threads are not blocked during I/O operations |
| **Lower Latency** | Faster response times for concurrent operations |
| **Backward Compatible** | Existing code can benefit from virtual threads with minimal changes |
| **Natural Exception Handling** | Traditional try-catch works without callback complications |

### Cons ❌

| Limitation | Description |
|-----------|------------|
| **Java 21+ Requirement** | Virtual threads are only available in Java 21 and later |
| **CPU-Bound Tasks** | No benefit for CPU-intensive work (benefits are for I/O-bound tasks) |
| **Thread-Local Variables** | Heavy use of ThreadLocal can still cause issues; consider using ScopedValue instead |
| **Pinning Risk** | In rare cases, virtual threads can get "pinned" to carrier threads (e.g., inside synchronized blocks or JNI calls) |
| **Monitoring Overhead** | JVM must track and schedule millions of virtual threads |
| **Learning Curve** | New mental model for developers used to traditional threading |
| **Not for JDK < 21** | Legacy applications stuck on older Java versions cannot use virtual threads |

## Code Examples

### Example 1: Web Server with Virtual Threads
```java
import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

public class VirtualThreadWebServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        
        System.out.println("Server started on port 8080");
        
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
                             " [" + Thread.currentThread().getName() + "]");
            
            // Simulate blocking operation
            Thread.sleep(100);
            
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println();
            out.println("Hello from virtual thread!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Example 2: Database Connection Pool with Virtual Threads
```java
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseVirtualThreadExample {
    public static void main(String[] args) throws InterruptedException {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        
        // Simulate 10,000 concurrent database queries
        for (int i = 1; i <= 10000; i++) {
            int queryId = i;
            executor.submit(() -> executeQuery(queryId));
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        System.out.println("All queries completed");
    }
    
    private static void executeQuery(int queryId) {
        try {
            System.out.println("[Query " + queryId + "] Starting on " + 
                             Thread.currentThread().getName());
            
            // Simulate database query (blocking I/O)
            Thread.sleep(500);
            
            System.out.println("[Query " + queryId + "] Completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Example 3: Comparing Virtual Threads vs Platform Threads
```java
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualVsPlatformThreads {
    
    static void demonstratePlatformThreads() throws InterruptedException {
        System.out.println("\n=== Platform Threads (Limited to ~1000) ===");
        long startTime = System.currentTimeMillis();
        
        var executor = Executors.newFixedThreadPool(1000);
        for (int i = 0; i < 10000; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Platform threads completed in: " + duration + "ms");
    }
    
    static void demonstrateVirtualThreads() throws InterruptedException {
        System.out.println("\n=== Virtual Threads (Can handle 1M+) ===");
        long startTime = System.currentTimeMillis();
        
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 10000; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Virtual threads completed in: " + duration + "ms");
    }
    
    public static void main(String[] args) throws InterruptedException {
        // Platform threads struggle with 10,000 concurrent operations
        demonstratePlatformThreads();
        
        // Virtual threads handle 10,000+ concurrent operations easily
        demonstrateVirtualThreads();
    }
}
```

### Example 4: Using StructuredConcurrency (Preview)
```java
import java.util.concurrent.*;

public class StructuredConcurrencyExample {
    
    static class Task {
        String name;
        int duration;
        
        Task(String name, int duration) {
            this.name = name;
            this.duration = duration;
        }
    }
    
    static void performTask(Task task) throws InterruptedException {
        Thread.sleep(task.duration);
        System.out.println(task.name + " completed on " + 
                         Thread.currentThread().getName());
    }
    
    public static void main(String[] args) throws InterruptedException {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var tasks = new Task[]{
            new Task("Task-1", 500),
            new Task("Task-2", 300),
            new Task("Task-3", 700)
        };
        
        long startTime = System.currentTimeMillis();
        
        // Submit all tasks and wait for completion
        var futures = new CompletableFuture[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            futures[i] = CompletableFuture.runAsync(
                () -> {
                    try {
                        performTask(tasks[i]);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                executor
            );
        }
        
        CompletableFuture.allOf(futures).join();
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("All tasks completed in: " + duration + "ms");
        executor.shutdown();
    }
}
```

## Best Practices

1. **Use `newVirtualThreadPerTaskExecutor()` for I/O-bound workloads** - Best when you have many concurrent I/O operations
2. **Avoid ThreadLocal abuse** - Use `ScopedValue` instead for better performance with virtual threads
3. **Be careful with synchronized blocks** - Virtual threads can get pinned; prefer locks like `ReentrantLock`
4. **Monitor your application** - Use JDK Flight Recorder to observe virtual thread behavior
5. **Not a silver bullet for CPU-bound work** - Virtual threads shine with blocking I/O; use thread pools for CPU-intensive tasks
6. **Keep carrier threads available** - Avoid blocking operations in critical sections that could starve the scheduler

## Conclusion

Virtual Threads represent a revolutionary change in how Java handles concurrency, allowing developers to write simple, maintainable code while achieving massive scalability. They transform Java from a language struggling with high concurrency to one that can easily handle millions of concurrent operations with minimal complexity.

For I/O-intensive applications (web servers, database clients, microservices), Virtual Threads are a game-changer. However, they don't replace thread pools for CPU-bound work, and their adoption requires Java 21 or later.
