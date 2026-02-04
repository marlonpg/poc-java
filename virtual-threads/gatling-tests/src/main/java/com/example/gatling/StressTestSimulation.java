package com.example.gatling;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Advanced stress test simulation
 * Gradually increases load to find breaking point
 */
public class StressTestSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("*/*")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader("Gatling/StressTest")
            .shareConnections();

    ScenarioBuilder stressScenario = scenario("Stress Test - Gradual Load Increase")
            .exec(
                    http("Request")
                            .get("/")
                            .check(status().is(200))
            )
            .pause(500, 1000);

    {
        setUp(
                stressScenario.injectOpen(
                        // Start small
                        rampOpenUsersPerSec(1).to(10).during(30),
                        // Increase gradually
                        rampOpenUsersPerSec(10).to(50).during(60),
                        // More aggressive
                        rampOpenUsersPerSec(50).to(200).during(90),
                        // Hold at high load
                        constantUsersPerSec(200).during(120)
                )
                        .protocols(httpProtocol)
        )
                .assertions(
                        global().successfulRequests().percent().gte(90.0)
                );
    }
}
