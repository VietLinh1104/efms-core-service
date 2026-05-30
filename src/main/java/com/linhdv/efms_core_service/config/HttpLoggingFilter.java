package com.linhdv.efms_core_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.UUID;

@Slf4j
@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws java.io.IOException, jakarta.servlet.ServletException {

        long start = System.currentTimeMillis();
        String traceId = resolveTraceId(request);
        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String message = "HTTP {} {} -> status={} durationMs={} traceId={} userId={} companyId={}";

            if (status >= 500) {
                log.error(message, request.getMethod(), fullPath(request), status, duration, traceId,
                        headerOrDash(request, "X-User-Id"), headerOrDash(request, "X-Company-Id"));
            } else if (status >= 400 || duration > 1000) {
                log.warn(message, request.getMethod(), fullPath(request), status, duration, traceId,
                        headerOrDash(request, "X-User-Id"), headerOrDash(request, "X-Company-Id"));
            } else {
                log.info(message, request.getMethod(), fullPath(request), status, duration, traceId,
                        headerOrDash(request, "X-User-Id"), headerOrDash(request, "X-Company-Id"));
            }

            MDC.remove("traceId");
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incomingTraceId = request.getHeader(TRACE_ID_HEADER);
        if (incomingTraceId != null && !incomingTraceId.isBlank()) {
            return incomingTraceId;
        }
        return UUID.randomUUID().toString();
    }

    private String fullPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private String headerOrDash(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? "-" : value;
    }
}
