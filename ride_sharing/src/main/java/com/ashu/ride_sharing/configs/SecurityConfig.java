package com.ashu.ride_sharing.configs;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.ashu.ride_sharing.models.enums.UserRole;
import com.ashu.ride_sharing.security.JWT_AuthenticationFilter;


import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    private final JWT_AuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_URLS ={
        "/api/v1/auth/**", // All endpoints under /api/v1/auth
        "/v3/api-docs/**", // OpenAPI docs
        "/swagger-ui/**",   // Swagger UI
        "/swagger-ui.html",
        "/webjars/**",
        "/health-check",
        "/api/v1/rides/search", // Make ride search public
        "/api/v1/rides/{rideId}"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
            .cors(cors->cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(
                authz -> authz
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    // Secure API groups by prefix (match all subpaths)
// .requestMatchers(HttpMethod.GET, "/api/v1/user").hasAnyRole(UserRole.DRIVER.name(), UserRole.PASSENGER.name(), UserRole.ADMIN.name())
//                     .requestMatchers("/api/v1/driver").hasAnyRole(UserRole.DRIVER.name())
//                     .requestMatchers("/api/v1/admin").hasAnyRole(UserRole.ADMIN.name())

                    .requestMatchers("/api/v1/user/**").hasAnyRole(UserRole.DRIVER.name(), UserRole.PASSENGER.name(), UserRole.ADMIN.name())
                    .requestMatchers("/api/v1/driver/**").hasRole(UserRole.DRIVER.name())
                    .requestMatchers("/api/v1/admin/**").hasRole(UserRole.ADMIN.name())
                    .anyRequest()
                    .authenticated()
            )
            .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers->headers.frameOptions(frameOptions->frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }
}
