# Load Testing Setup

This directory contains tools for load testing and comparing the Virtual Threads server against Platform Threads implementation.

## Prerequisites

### 1. Install 'hey' Load Testing Tool

**hey** is a fast HTTP load generator written in Go. Download the binary for your OS:

- **Windows**: Download from [releases](https://github.com/rakyll/hey/releases)
  - Extract and add to your PATH, or place in this directory
  
- **macOS**: 
  ```bash
  brew install hey
  ```

- **Linux**:
  ```bash
  go install github.com/rakyll/hey@latest
  ```

Or download pre-built binaries from: https://github.com/rakyll/hey/releases

### 2. Verify Installation

```powershell
hey -h
```

You should see the help output if installed correctly.

## Running Load Tests

### Step 1: Start the Web Server

In one terminal, start the server (from the virtual-threads directory):

```powershell
mvn clean compile exec:java
```

The server will be available at `http://localhost:8080`

### Step 2: Run the Load Test

In another terminal, run from the load-testing directory:

```powershell
.\run-load-test.ps1
```

**Optional parameters:**
```powershell
# Custom server URL
.\run-load-test.ps1 -ServerUrl http://localhost:9090

# Custom load parameters
.\run-load-test.ps1 -Requests 20000 -Connections 2000 -Duration 60

# Custom test name
.\run-load-test.ps1 -TestName virtual-threads-test-1
```

### Step 3: View Results

Results are saved to the `results/` directory:
- Text output: `results/test-name.txt`
- JSON output: `results/test-name.json`

## Comparing Results

After running multiple tests, generate a comparison report:

```powershell
python .\compare-results.py
```

This will:
1. Parse all test files in `results/`
2. Display individual test metrics
3. Show comparison summary (best/worst)
4. Calculate improvements
5. Export to CSV for further analysis

## Testing Scenario: Virtual Threads vs Platform Threads

### Test 1: Virtual Threads (Current Implementation)

```powershell
.\run-load-test.ps1 -TestName virtual-threads-1000-10k -Connections 1000 -Requests 10000
```

### Test 2: Platform Threads (Fixed Pool)

After switching the server to use `Executors.newFixedThreadPool(200)`:

```powershell
.\run-load-test.ps1 -TestName platform-threads-1000-10k -Connections 1000 -Requests 10000
```

### Compare Results

```powershell
python .\compare-results.py
```

## Key Metrics to Compare

| Metric | Description |
|--------|-------------|
| **Throughput (req/s)** | Requests handled per second (higher is better) |
| **Avg Latency** | Average response time in milliseconds (lower is better) |
| **Min/Max** | Minimum and maximum response times |
| **P50/P95/P99** | Response time percentiles (lower is better) |
| **Total Data** | Amount of data transferred |

## Load Test Parameters Explained

```powershell
.\run-load-test.ps1 -Requests 10000 -Connections 1000 -Duration 30
```

- **Requests (-n)**: Total number of HTTP requests to send
- **Connections (-c)**: Number of concurrent connections
- **Duration (-z)**: How long to run the test (in seconds)

## Expected Results

### With Virtual Threads:
- ✅ **High throughput** (thousands of requests/sec)
- ✅ **Consistent latency** even with many connections
- ✅ **Can handle 1000+ concurrent connections easily**

### With Platform Threads (limited pool):
- ⚠️ **Lower throughput** (limited by thread pool size)
- ⚠️ **Queuing delay** when connections exceed pool size
- ⚠️ **Struggles at 1000+ concurrent connections**

## Sample Output

```
Summary:
  Total:        30.0108 secs
  Slowest:      2.1234 secs
  Fastest:      0.0123 secs
  Average:      0.0891 secs
  Requests/sec: 332.87

Response time histogram:
  0.012 [1]     |
  0.224 [4322]  |████████████████████████████████████████
  0.436 [5123]  |█████████████████████████████████████████████████
  0.648 [423]   |████
  0.860 [132]   |█
  1.073 [10]    |

Latency distribution:
  10% in 0.0234 secs
  25% in 0.0456 secs
  50% in 0.0789 secs
  75% in 0.1234 secs
  90% in 0.1876 secs
  95% in 0.2145 secs
  99% in 0.3456 secs
```

## Tips for Testing

1. **Warm up**: Run a quick test first to warm up the JVM
2. **Multiple runs**: Run each configuration 2-3 times for consistency
3. **Monitor resources**: Use `jconsole` or task manager to monitor memory/CPU
4. **Isolate tests**: Make sure no other heavy processes are running
5. **Document conditions**: Note OS, JVM version, and hardware used

## Troubleshooting

### hey command not found
- Ensure 'hey' is installed and in PATH
- Or download binary and place in this directory

### Connection refused
- Make sure the web server is running on port 8080
- Check server logs for errors

### Out of memory
- Start with smaller numbers: `-Requests 1000 -Connections 100`
- Increase JVM heap: `java -Xmx2G ...`

## Next Steps

1. Run tests on Virtual Threads implementation
2. Modify the server to use `newFixedThreadPool()` for comparison
3. Run identical tests on Platform Threads
4. Use `compare-results.py` to analyze differences
5. Review metrics to understand the performance improvements

## References

- [hey GitHub](https://github.com/rakyll/hey)
- [Virtual Threads Documentation](../docs/virtual_threads.md)
