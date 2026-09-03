package com.paymentproof.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final Map<String, RequestBucket> clientBuckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Rate limit sensitive endpoints: /api/incidents/*/investigate and /api/incidents/*/resolve
        if (path.contains("/investigate") || path.contains("/resolve")) {
            long currentMinute = System.currentTimeMillis() / 60000;
            String key = clientIp + ":" + currentMinute;

            RequestBucket bucket = clientBuckets.computeIfAbsent(key, k -> new RequestBucket(currentMinute));
            if (bucket.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for client: {} on path: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Rate limit exceeded\", \"message\": \"Too many sensitive investigation requests. Please wait a minute.\"}");
                return false;
            }

            // Periodically clean old minute buckets
            if (clientBuckets.size() > 1000) {
                clientBuckets.entrySet().removeIf(e -> e.getValue().minute < currentMinute - 2);
            }
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestBucket {
        final long minute;
        final AtomicInteger count = new AtomicInteger(0);

        RequestBucket(long minute) {
            this.minute = minute;
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
