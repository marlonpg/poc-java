package com.example.gatling;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Gatling load test simulation for Virtual Thread Web Server
 * Tests the server performance with various load scenarios
 */
public class VirtualThreadServerSimulation extends Simulation {

    // Configuration
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int USERS = Integer.parseInt(System.getProperty("users", "100"));
    private static final int RAMP_UP_DURATION = Integer.parseInt(System.getProperty("rampUp", "60"));
    private static final int TEST_DURATION = Integer.parseInt(System.getProperty("duration", "300"));

    // HTTP Configuration
    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader("Gatling/VirtualThreadTest")
            .shareConnections();

    // Scenario: Simple GET request
    ScenarioBuilder scenario = scenario("Virtual Thread Server Test")
            .exec(
                    http("Home Page")
                            .get("/")
                            .check(status().is(200))
                            .check(responseTimeInMillis().lt(5000L))
            )
            .pause(1);

    // Load profile: Ramp up users over time
    {
        setUp(
                scenario.injectOpen(
                        rampOpenUsersPerSec(1)
                                .to(USERS / RAMP_UP_DURATION)
                                .during(RAMP_UP_DURATION),
                        constantUsersPerSec(USERS / RAMP_UP_DURATION)
                                .during(TEST_DURATION)
                )
                        .protocols(httpProtocol)
        )
                .assertions(
                        global().responseTime().mean().lt(1000),
                        global().successfulRequests().percent().gte(95.0)
                );
    }
}
