package com.example.SoftwareProjectHexashop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${APP_LOGIN_USER:admin}") String username,
            @Value("${APP_LOGIN_PASSWORD:admin123}") String password) {
        UserDetails primaryUser = User.withUsername(username)
                .password("{noop}" + password)
                .roles("USER")
                .build();

        // Keep a fallback 'user' account for easier local testing in browsers.
        UserDetails fallbackUser = User.withUsername("user")
                .password("{noop}" + password)
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(primaryUser, fallbackUser);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
