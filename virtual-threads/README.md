# Virtual Threads POC - Java 21

This project demonstrates Java Virtual Threads with a simple web server implementation.

## Prerequisites

- Java 21 or higher
- Maven 3.6+

## Project Structure

```
poc-java/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── virtualthreads/
│                       ├── VirtualThreadWebServer.java
│                       └── SimpleHttpClient.java
├── docs/
│   └── virtual_threads.md
├── pom.xml
└── README.md
```

## Running the Web Server

### Option 1: Using Maven
```bash
mvn clean compile exec:java
```

### Option 2: Using Maven with custom class
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.virtualthreads.VirtualThreadWebServer"
```

### Option 3: Manual compilation and execution
```bash
mvn clean compile
java -cp target/classes com.example.virtualthreads.VirtualThreadWebServer
```

The server will start on **http://localhost:8080**

## Testing the Server

### Option 1: Using the provided HTTP Client
In a **new terminal**, run:
```bash
mvn exec:java -Dexec.mainClass="com.example.virtualthreads.SimpleHttpClient"
```

This will send 100 concurrent requests to test the server's performance.

### Option 2: Using curl
```bash
curl http://localhost:8080
```

### Option 3: Using your browser
Open your browser and navigate to: http://localhost:8080

### Option 4: Load testing with multiple requests
**Windows PowerShell:**
```powershell
1..10 | ForEach-Object { Start-Job { curl http://localhost:8080 } }
Get-Job | Wait-Job | Receive-Job
```

**Linux/Mac:**
```bash
for i in {1..10}; do curl http://localhost:8080 & done
```

## What to Observe

When running the server and client, you'll notice:

1. **Virtual Thread Names**: Each request is handled by a virtual thread (e.g., `VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1`)
2. **Scalability**: The server can handle hundreds or thousands of concurrent requests easily
3. **Low Memory**: Despite many concurrent connections, memory usage remains low
4. **Simple Code**: No complex async/reactive patterns needed

## Stopping the Server

Press `Ctrl+C` in the terminal where the server is running.

## Documentation

For detailed information about Virtual Threads, see [docs/virtual_threads.md](docs/virtual_threads.md)

## Key Features

- ✅ Java 21 Virtual Threads
- ✅ Simple synchronous HTTP server
- ✅ Handles blocking I/O efficiently
- ✅ Load testing client included
- ✅ Demonstrates massive scalability

## License

This is a proof-of-concept project for learning purposes.
