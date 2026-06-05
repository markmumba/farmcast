package com.farmcastai.config;

import com.farmcastai.config.properties.ResilienceProperties;
import com.farmcastai.exception.WeatherAiClientException;
import com.farmcastai.exception.WeatherAiUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j circuit breaker and retry beans for the WeatherAI API client.
 *
 * <p><b>Flow:</b>
 * <pre>
 *   Incoming call
 *     └─ CircuitBreaker (outer gate)
 *          ├─ OPEN  → CallNotPermittedException immediately (no HTTP, no retry)
 *          └─ CLOSED / HALF-OPEN
 *               └─ Retry (exponential back-off + jitter)
 *                    └─ HTTP call (connect/read timeouts via RestClient)
 * </pre>
 *
 * <p><b>Thundering-herd prevention:</b>
 * Retries use exponential random back-off.  Each wait = {@code base × multiplier^n ± jitter},
 * where {@code jitterFactor = 0.5} spreads concurrent retries across ±50% of the interval
 * so they don't all fire at the same instant.
 *
 * <p><b>What counts as a failure:</b>
 * <ul>
 *   <li>{@link WeatherAiUnavailableException} – network/connectivity error (always fails)</li>
 *   <li>{@link WeatherAiClientException} with a 5xx status – upstream server error</li>
 *   <li>4xx responses are <em>not</em> counted – they are client errors, not service failures</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResilienceConfig {

    private static final String INSTANCE = "weatherai";

    private final ResilienceProperties props;

    // ── Circuit breaker ────────────────────────────────────────────────────────

    @Bean
    public CircuitBreaker weatherAiCircuitBreaker(CircuitBreakerRegistry registry) {
        ResilienceProperties.CircuitBreakerProperties cb = props.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cb.getSlidingWindowSize())
                .minimumNumberOfCalls(cb.getMinimumCalls())
                .failureRateThreshold(cb.getFailureRateThreshold())
                .slowCallRateThreshold(cb.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofSeconds(cb.getSlowCallDurationSeconds()))
                .waitDurationInOpenState(Duration.ofSeconds(cb.getWaitDurationOpenSeconds()))
                .permittedNumberOfCallsInHalfOpenState(cb.getHalfOpenCalls())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // Count 5xx API responses and connectivity failures; ignore 4xx
                .recordException(ex -> {
                    if (ex instanceof WeatherAiClientException wex) {
                        return wex.getStatusCode().is5xxServerError();
                    }
                    return ex instanceof WeatherAiUnavailableException;
                })
                .build();

        CircuitBreaker breaker = registry.circuitBreaker(INSTANCE, config);

        // Emit structured log lines on state changes and failures
        breaker.getEventPublisher()
                .onStateTransition(e -> log.warn(
                        "WeatherAI circuit breaker: {} → {}",
                        e.getStateTransition().getFromState(),
                        e.getStateTransition().getToState()))
                .onError(e -> log.debug(
                        "WeatherAI circuit breaker recorded error: {}",
                        e.getThrowable().getMessage()))
                .onSuccess(e -> log.debug(
                        "WeatherAI circuit breaker recorded success ({}ms)",
                        e.getElapsedDuration().toMillis()))
                .onCallNotPermitted(e -> log.warn(
                        "WeatherAI circuit breaker OPEN – request rejected"));

        log.info("WeatherAI circuit breaker configured: window={} minCalls={} failureThreshold={}% "
                        + "openWait={}s halfOpenProbes={}",
                cb.getSlidingWindowSize(), cb.getMinimumCalls(),
                cb.getFailureRateThreshold(), cb.getWaitDurationOpenSeconds(),
                cb.getHalfOpenCalls());

        return breaker;
    }

    // ── Retry ──────────────────────────────────────────────────────────────────

    @Bean
    public Retry weatherAiRetry(RetryRegistry registry) {
        ResilienceProperties.RetryProperties r = props.getRetry();

        /*
         * Exponential random back-off:
         *   attempt 1 wait ≈ 1 s  ± 50% → [0.5 s, 1.5 s]
         *   attempt 2 wait ≈ 2 s  ± 50% → [1.0 s, 3.0 s]
         *   attempt 3 wait ≈ 4 s  ± 50% → [2.0 s, 6.0 s]  (capped at maxWait)
         *
         * The ±50% jitter ensures that many concurrent callers do NOT retry at the
         * exact same millisecond – i.e. no thundering herd.
         */
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(r.getMaxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        Duration.ofSeconds(r.getInitialWaitSeconds()),
                        r.getMultiplier(),
                        r.getJitterFactor(),
                        Duration.ofSeconds(r.getMaxWaitSeconds())
                ))
                // Only retry connectivity failures – never retry 4xx / 5xx API responses
                .retryOnException(ex -> ex instanceof WeatherAiUnavailableException)
                .build();

        Retry retry = registry.retry(INSTANCE, config);

        retry.getEventPublisher()
                .onRetry(e -> log.warn(
                        "WeatherAI retry attempt {}/{} after {}ms: {}",
                        e.getNumberOfRetryAttempts(),
                        r.getMaxAttempts() - 1,
                        e.getWaitInterval().toMillis(),
                        e.getLastThrowable() != null ? e.getLastThrowable().getMessage() : "unknown"))
                .onError(e -> log.error(
                        "WeatherAI all {} retry attempts exhausted: {}",
                        r.getMaxAttempts(),
                        e.getLastThrowable() != null ? e.getLastThrowable().getMessage() : "unknown"))
                .onSuccess(e -> log.debug(
                        "WeatherAI call succeeded after {} attempt(s)",
                        e.getNumberOfRetryAttempts() + 1));

        log.info("WeatherAI retry configured: maxAttempts={} initialWait={}s multiplier={} "
                        + "jitter={}% maxWait={}s",
                r.getMaxAttempts(), r.getInitialWaitSeconds(),
                r.getMultiplier(), (int) (r.getJitterFactor() * 100),
                r.getMaxWaitSeconds());

        return retry;
    }
}
