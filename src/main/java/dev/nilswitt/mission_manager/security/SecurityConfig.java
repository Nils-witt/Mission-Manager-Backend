package dev.nilswitt.mission_manager.security;

import dev.nilswitt.mission_manager.security.filter.CorsFilter;
import dev.nilswitt.mission_manager.security.jwt.SpringSecurityJwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * This file contains the global base access Policy which controls where authentification is required and where not
 * authorization is handled at controller level
 */
@EnableWebSecurity
@Configuration
class SecurityConfig {

    private final SpringSecurityJwtFilter jwtFilter;

    private final CorsFilter corsFilter;

    public SecurityConfig(SpringSecurityJwtFilter jwtFilter, CorsFilter corsFilter) {
        this.jwtFilter = jwtFilter;
        this.corsFilter = corsFilter;
    }

    /**
     * Controls the access to the Websocket
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Order(0)
    @Bean
    SecurityFilterChain wsFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/ws/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    /**
     * Controls the access to the REST API
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Order(1)
    @Bean
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                auth.requestMatchers("/api/token/**").permitAll();
                auth.requestMatchers("/api").permitAll();
                auth.anyRequest().authenticated();
            })
            .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(corsFilter, SpringSecurityJwtFilter.class)
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .build();
    }

    /**
     * Controls the access to the server rendered web pages
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Order(2)
    @Bean
    SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/error").permitAll();
                auth.requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll();
                auth.anyRequest().authenticated();
            })
            .formLogin(form -> form.loginPage("/login").permitAll())
            .logout(logout -> logout.permitAll())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
