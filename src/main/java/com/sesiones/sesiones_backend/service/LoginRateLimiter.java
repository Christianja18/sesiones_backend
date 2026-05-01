package com.sesiones.sesiones_backend.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.sesiones.sesiones_backend.config.SecurityProperties;
import com.sesiones.sesiones_backend.exception.RateLimitExceededException;

@Component
public class LoginRateLimiter {

    private final Map<String, Deque<Instant>> failuresByKey = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;

    public LoginRateLimiter(SecurityProperties securityProperties, Clock clock) {
        this.clock = clock;
        this.maxAttempts = securityProperties.getLogin().getMaxAttempts();
        this.windowSeconds = securityProperties.getLogin().getWindowSeconds();
    }

    public void assertAllowed(String email, String clientIp) {
        String key = buildKey(email, clientIp);
        Deque<Instant> failures = failuresByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (failures) {
            removeExpired(failures);
            if (failures.size() >= maxAttempts) {
                throw new RateLimitExceededException("Demasiados intentos de login. Intenta nuevamente mas tarde");
            }
        }
    }

    public void recordFailure(String email, String clientIp) {
        String key = buildKey(email, clientIp);
        Deque<Instant> failures = failuresByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (failures) {
            removeExpired(failures);
            failures.addLast(Instant.now(clock));
        }
    }

    public void reset(String email, String clientIp) {
        failuresByKey.remove(buildKey(email, clientIp));
    }

    private void removeExpired(Deque<Instant> failures) {
        Instant threshold = Instant.now(clock).minusSeconds(windowSeconds);
        while (!failures.isEmpty() && failures.peekFirst().isBefore(threshold)) {
            failures.removeFirst();
        }
    }

    private String buildKey(String email, String clientIp) {
        return email.toLowerCase(Locale.ROOT) + "|" + clientIp;
    }
}
