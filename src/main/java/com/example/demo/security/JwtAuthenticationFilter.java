package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // ✅ Skip JWT validation ONLY for public auth endpoints
        // These endpoints do NOT need authentication:
        if (path.equals("/api/auth/register") ||
            path.equals("/api/auth/login") ||
            path.equals("/api/auth/create-test-user") ||
            path.equals("/api/auth/test") ||
            path.equals("/api/auth/test-email") ||
            path.equals("/api/auth/email-config-status") ||
            path.equals("/api/auth/send-registration-otp") ||
            path.equals("/api/auth/send-login-otp") ||
            path.equals("/api/auth/register-with-otp") ||
            path.equals("/api/auth/login-with-otp") ||
            path.equals("/") ||
            path.startsWith("/h2-console/") ||
            path.contains("swagger") ||
            path.contains("api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // All other /api/auth/* endpoints (profile, update-profile, etc.) REQUIRE authentication

        final String authHeader = request.getHeader("Authorization");

        // ✅ If no token, let Spring Security handle as unauthorized later — don't manually block here
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.isTokenValid(token)) {
                throw new RuntimeException("Invalid or expired JWT");
            }

            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);
            Role role = Role.valueOf(jwtUtil.extractRole(token));

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // 🔧 Create a User object with all necessary fields
                User user = new User();
                user.setId(userId);
                user.setUsername(username);
                user.setEmail(username); // ✅ Also set email (username could be email)
                user.setRole(role);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user, // ✅ User object is now the principal
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid or expired JWT token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
