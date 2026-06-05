package com.farmcastai.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable resiliency settings for the WeatherAI API client.
 * All values have safe defaults and can be overridden per environment via application.yml.
 */
@ConfigurationProperties(prefix = "resilience.weather-ai")
@Getter
@Setter
public class ResilienceProperties {

    /** HTTP connect timeout (seconds). */
    private int connectTimeoutSeconds = 5;

    /** HTTP read timeout (seconds). Generous because AI endpoints can be slow. */
    private int readTimeoutSeconds = 30;

    private RetryProperties retry = new RetryProperties();
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

    @Getter
    @Setter
    public static class RetryProperties {
        /** Total number of attempts (1 original + N-1 retries). */
        private int maxAttempts = 3;

        /** Base wait between retries (seconds). Actual wait = base × multiplier^attempt ± jitter. */
        private int initialWaitSeconds = 1;

        /** Exponential growth factor per retry. */
        private double multiplier = 2.0;

        /**
         * Random jitter fraction (0–1). A value of 0.5 spreads retries across ±50% of the
         * computed interval, preventing thundering herd when many clients retry simultaneously.
         */
        private double jitterFactor = 0.5;

        /** Hard cap on the per-retry wait (seconds). */
        private int maxWaitSeconds = 10;
    }

    @Getter
    @Setter
    public static class CircuitBreakerProperties {
        /** Number of recent calls used to compute the failure rate. */
        private int slidingWindowSize = 10;

        /** Minimum calls before the failure rate is evaluated. */
        private int minimumCalls = 5;

        /** Failure rate (%) that trips the circuit from CLOSED → OPEN. */
        private float failureRateThreshold = 50.0f;

        /** Slow-call rate (%) that also trips the circuit. */
        private float slowCallRateThreshold = 80.0f;

        /** A call slower than this (seconds) counts as a slow call. */
        private int slowCallDurationSeconds = 20;

        /** Time the circuit stays OPEN before allowing probe calls (seconds). */
        private int waitDurationOpenSeconds = 30;

        /** Number of probe calls allowed in HALF-OPEN state before deciding to close or re-open. */
        private int halfOpenCalls = 3;
    }
}
