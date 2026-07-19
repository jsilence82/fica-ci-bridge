package com.ficabridge.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ODataClientBase} actually engages the Resilience4j RateLimiter
 * on outbound SAP OData calls, rather than just wiring the bean without effect.
 */
@WireMockTest
class ODataClientRateLimiterTest {

    private static final String BASE_PATH = "/API_CA_CONTRACTACCOUNT/ContractAccount";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ContractAccountClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        WebClient webClient = WebClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();

        // Only 2 permits per (long) period, and callers give up almost immediately if a
        // permit isn't free — mirrors "sapOData" in application.yml but tightened so the
        // test doesn't need to wait out a real refresh window.
        RateLimiterConfig tightConfig = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .timeoutDuration(Duration.ofMillis(20))
                .build();
        RateLimiter rateLimiter = RateLimiter.of("test-sapOData", tightConfig);

        client = new ContractAccountClient(webClient, OBJECT_MAPPER, rateLimiter);

        stubFor(get(urlPathEqualTo(BASE_PATH + "('100200')"))
                .willReturn(okJson("{\"ContractAccount\":\"0000100200\"}")));
    }

    @Test
    void concurrentCallsBeyondLimit_areRejectedWithRequestNotPermitted() throws Exception {
        int totalCallers = 6;
        ExecutorService executor = Executors.newFixedThreadPool(totalCallers);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < totalCallers; i++) {
            tasks.add(() -> {
                startLatch.await();
                try {
                    client.findById("100200");
                    return true;
                } catch (RequestNotPermitted ex) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(executor.submit(task));
        }
        startLatch.countDown();

        int accepted = 0;
        int rejected = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(5, TimeUnit.SECONDS)) {
                accepted++;
            } else {
                rejected++;
            }
        }
        executor.shutdown();

        // Exactly limitForPeriod (2) callers should get a permit; the rest must be rejected
        // rather than silently succeeding or hanging.
        assertThat(accepted).isEqualTo(2);
        assertThat(rejected).isEqualTo(4);
    }
}
