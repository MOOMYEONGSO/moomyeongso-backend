package org.example.moomyeongso.common.config;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.domain.auth.jwt.JwtAuthenticationEntryPoint;
import org.example.moomyeongso.domain.auth.jwt.JwtAuthenticationFilter;
import org.example.moomyeongso.domain.auth.jwt.JwtSecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtSecurityProperties jwtSecurityProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> permitAllPaths = new ArrayList<>(safePaths(jwtSecurityProperties.getIgnorePaths()));
        permitAllPaths.addAll(safePaths(jwtSecurityProperties.getOptionalAuthPaths()));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (!permitAllPaths.isEmpty()) {
                        auth.requestMatchers(permitAllPaths.toArray(new String[0])).permitAll();
                    }
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**")
                            .hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(customizer ->
                        customizer.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private List<String> safePaths(List<String> paths) {
        return paths == null ? Collections.emptyList() : paths;
    }
}
