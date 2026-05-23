package com.linhdv.efms_core_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String email = request.getHeader("X-User-Email");
        String permissionsRaw = request.getHeader("X-User-Permission");

        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities = parsePermissions(permissionsRaw);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, null,
                    authorities);

            // Lưu userId vào details để AuditService có thể đọc mà không cần inject
            // HttpServletRequest vào Service layer
            authentication.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parsePermissions(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("[]"))
            return List.of();

        // Remove brackets [] and whitespace
        String cleaned = raw.replaceAll("[\\[\\]\\s]", "");
        if (cleaned.isBlank())
            return List.of();

        return Arrays.stream(cleaned.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
