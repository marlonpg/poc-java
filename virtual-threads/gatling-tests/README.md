# Gatling Load Tests for Virtual Threads

This module contains Gatling load testing simulations written in **Java** (not Scala) to compare Virtual Threads performance.

## Prerequisites

- Java 21+
- Maven 3.6+
- Virtual Thread Web Server running on `http://localhost:8080`

## Project Structure

```
gatling-tests/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/gatling/
│   │   │       ├── VirtualThreadServerSimulation.java
│   │   │       ├── StressTestSimulation.java
│   │   │       └── SustainedLoadSimulation.java
│   │   └── resources/
│   │       └── logback.xml
│   └── gatling/
│       └── results/
├── pom.xml
└── README.md
```

## Available Simulations

### 1. VirtualThreadServerSimulation
**Default test** - Ramps up users gradually and maintains sustained load

```bash
mvn clean gatling:test -Dgatling.simulationClass=com.example.gatling.VirtualThreadServerSimulation
```

**Parameters:**
- `baseUrl`: Server URL (default: `http://localhost:8080`)
- `users`: Number of users to simulate (default: `100`)
- `rampUp`: Ramp-up duration in seconds (default: `60`)
- `duration`: Test duration in seconds (default: `300`)

**Example with custom parameters:**
```bash
mvn clean gatling:test \
  -Dgatling.simulationClass=com.example.gatling.VirtualThreadServerSimulation \
  -DbaseUrl=http://localhost:8080 \
  -Dusers=500 \
  -DrampUp=120 \
  -Dduration=600
```

### 2. StressTestSimulation
**Stress testing** - Progressively increases load to find breaking point

```bash
mvn clean gatling:test -Dgatling.simulationClass=com.example.gatling.StressTestSimulation
```

**What it does:**
- 10 users for 30 seconds
- Ramps to 50 users over 60 seconds
- Ramps to 200 users over 90 seconds
- Holds 200 users for 120 seconds

### 3. SustainedLoadSimulation
**Endurance testing** - Maintains high load for extended period

```bash
mvn clean gatling:test -Dgatling.simulationClass=com.example.gatling.SustainedLoadSimulation
```

**What it does:**
- 500 concurrent users
- 10 minute sustained load
- Measures stability and consistency

## Running Tests

### Quick Start

**Terminal 1 - Start the server:**
```powershell
cd ..\
mvn clean compile exec:java
```

**Terminal 2 - Run Gatling test:**
```powershell
cd gatling-tests
mvn clean gatling:test
```

### Running Specific Simulations

```bash
# VirtualThreadServerSimulation (default)
mvn gatling:test

# StressTestSimulation
mvn gatling:test -Dgatling.simulationClass=com.example.gatling.StressTestSimulation

# SustainedLoadSimulation
mvn gatling:test -Dgatling.simulationClass=com.example.gatling.SustainedLoadSimulation
```

### Comparing Virtual Threads vs Platform Threads

**Step 1: Test Virtual Threads (Current)**
```bash
mvn clean gatling:test -Dgatling.simulationClass=com.example.gatling.VirtualThreadServerSimulation -Dusers=1000 -Dduration=300
```

**Step 2: Switch server to Platform Threads**
- Edit `VirtualThreadWebServer.java`
- Change: `Executors.newVirtualThreadPerTaskExecutor()` 
- To: `Executors.newFixedThreadPool(200)`

**Step 3: Restart server and test again**
```bash
mvn clean gatling:test -Dgatling.simulationClass=com.example.gatling.VirtualThreadServerSimulation -Dusers=1000 -Dduration=300
```

**Step 4: Compare results**
Check the HTML reports generated in `target/gatling/`

## Understanding Results

### Key Metrics

| Metric | Description |
|--------|-------------|
| **Response Time Mean** | Average response time in ms |
| **Response Time Min/Max** | Minimum and maximum response times |
| **Percentiles (P50, P95, P99)** | Response times at different percentiles |
| **Requests/sec** | Throughput |
| **Success Rate** | Percentage of successful requests |
| **Errors** | Failed requests and error types |

### Generated Reports

After each test, Gatling generates an HTML report:
```
target/gatling/results/
└── <timestamp>/
    ├── index.html
    ├── stats.json
    └── simulation.log
```

**Open the HTML report:**
```powershell
start target/gatling/results/<timestamp>/index.html
```

## Test Scenarios

### Scenario 1: Normal Load (100-500 users)
```bash
mvn gatling:test -Dusers=500 -DrampUp=120 -Dduration=300
```
**Expected for Virtual Threads:** 
- Consistent ~10-50ms latency
- 95%+ success rate

**Expected for Platform Threads (pool=200):**
- Increasing latency above 200 users
- Queueing delays
- Potential failures

### Scenario 2: High Load (1000+ users)
```bash
mvn gatling:test -Dusers=2000 -DrampUp=60 -Dduration=300
```
**Expected for Virtual Threads:**
- Still responsive
- Handles 2000+ concurrent users
- 95%+ success rate

**Expected for Platform Threads:**
- Queue buildup
- 50%+ request failures
- High latency (500ms+)

### Scenario 3: Stress Test
```bash
mvn gatling:test -Dgatling.simulationClass=com.example.gatling.StressTestSimulation
```
Finds the breaking point of the system

### Scenario 4: Sustained Load
```bash
mvn gatling:test -Dgatling.simulationClass=com.example.gatling.SustainedLoadSimulation
```
Tests stability under extended high load

## Advanced Configuration

### Custom Simulations

Create new simulation classes following this pattern:

```java
public class MySimulation extends Simulation {
    HttpProtocolBuilder httpProtocol = http.baseUrl("http://localhost:8080");
    
    ScenarioBuilder scenario = scenario("My Test")
            .exec(http("Request").get("/"))
            .pause(1);
    
    {
        setUp(scenario.injectOpen(constantUsersPerSec(10).during(60))
                .protocols(httpProtocol))
                .assertions(global().successfulRequests().percent().gte(95.0));
    }
}
```

Run it with:
```bash
mvn gatling:test -Dgatling.simulationClass=com.example.gatling.MySimulation
```

### Modifying Assertions

Edit assertions in the simulation to check different criteria:

```java
.assertions(
    global().responseTime().mean().lt(500),           // Mean < 500ms
    global().responseTime().percentile(95.0).lt(1000), // P95 < 1000ms
    global().responseTime().percentile(99.0).lt(5000), // P99 < 5000ms
    global().successfulRequests().percent().gte(99.0)  // 99% success
)
```

## Troubleshooting

### Server Connection Error
- Ensure server is running: `http://localhost:8080`
- Check firewall settings
- Try: `curl http://localhost:8080`

### Out of Memory
Increase heap size:
```bash
MAVEN_OPTS="-Xmx2G" mvn gatling:test
```

### Reports not generated
Check: `target/gatling/results/` directory exists and has content

## Performance Analysis Tips

1. **Warm up first**: Run a small test before the main one
2. **Isolate tests**: Close other applications during tests
3. **Monitor server**: Watch CPU/memory during tests with `jconsole`
4. **Multiple runs**: Run each test 2-3 times for consistency
5. **Document conditions**: Note JVM version, OS, hardware

## References

- [Gatling Documentation](https://gatling.io/docs/gatling/)
- [Gatling Java API](https://gatling.io/docs/gatling/reference/current/core/injection/)
- [Virtual Threads Documentation](../../docs/virtual_threads.md)

## License

POC project for learning purposes.
