package com.andon.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(antMatcher("/login"), antMatcher("/error"), antMatcher("/health")).permitAll()
                .requestMatchers(antMatcher("/h2-console/**")).hasRole("ADMIN")
                .requestMatchers(antMatcher(HttpMethod.GET, "/events/new"),
                                 antMatcher(HttpMethod.POST, "/events"))
                    .hasAnyRole("OPERATOR", "SUPERVISOR", "ADMIN")
                .requestMatchers(antMatcher(HttpMethod.POST, "/events/*/status"))
                    .hasAnyRole("SUPERVISOR", "ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/events", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(antMatcher("/h2-console/**")))
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder enc,
            @Value("${andon.viewer.password:viewer123}") String viewerPw,
            @Value("${andon.operator.password:operator123}") String operatorPw,
            @Value("${andon.supervisor.password:supervisor123}") String supervisorPw,
            @Value("${andon.admin.password:admin123}") String adminPw) {
        return new InMemoryUserDetailsManager(
            User.withUsername("viewer").password(enc.encode(viewerPw)).roles("VIEWER").build(),
            User.withUsername("operator").password(enc.encode(operatorPw)).roles("OPERATOR").build(),
            User.withUsername("supervisor").password(enc.encode(supervisorPw)).roles("SUPERVISOR").build(),
            User.withUsername("admin").password(enc.encode(adminPw)).roles("ADMIN").build());
    }
}