package com.example.gatling;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Sustained load test
 * Maintains a consistent high load for an extended period
 */
public class SustainedLoadSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int SUSTAINED_USERS = 500;
    private static final int TEST_DURATION = 600; // 10 minutes

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("*/*")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader("Gatling/SustainedLoad")
            .shareConnections();

    ScenarioBuilder sustainedScenario = scenario("Sustained Load Test")
            .exec(
                    http("Request")
                            .get("/")
                            .check(status().is(200))
            )
            .pause(100, 500);

    {
        setUp(
                sustainedScenario.injectOpen(
                        // Quick ramp up
                        rampOpenUsersPerSec(1).to(SUSTAINED_USERS).during(60),
                        // Sustained load for extended period
                        constantUsersPerSec(SUSTAINED_USERS).during(TEST_DURATION)
                )
                        .protocols(httpProtocol)
        )
                .assertions(
                        global().responseTime().mean().lt(500),
                        global().responseTime().percentile(95.0).lt(1000),
                        global().responseTime().percentile(99.0).lt(5000),
                        global().successfulRequests().percent().gte(99.0)
                );
    }
}
