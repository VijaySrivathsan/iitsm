package com.iitsm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Authentication and role-based access:
 * <ul>
 *   <li>form login with BCrypt passwords (see PasswordConfig); CSRF protection on for every form;</li>
 *   <li>URL rules per role: {@code /employee/**}, {@code /tech/**}, {@code /admin/**};</li>
 *   <li>{@code @PreAuthorize} on sensitive service methods (method security) as a second line;</li>
 *   <li>ownership checks (own / assigned incidents) in IncidentService as the third line.</li>
 * </ul>
 * The H2 console is only reachable when it is enabled (dev profile).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${spring.h2.console.enabled:false}") boolean h2Console)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/error")
                        .permitAll();
                if (h2Console) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth.requestMatchers("/employee/**").hasRole("EMPLOYEE")
                    .requestMatchers("/tech/**").hasRole("TECHNICIAN")
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated();
            })
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"));
        if (h2Console) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }
        return http.build();
    }
}
