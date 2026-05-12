package com.terminal.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Estaticos
                        .requestMatchers("/*.html", "/*.js", "/*.css", "/").permitAll()

                        // Auth publica
                        .requestMatchers("/users/login", "/users/register", "/users/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/routes").permitAll()

                        // ADMIN y EMPRESA pueden crear, editar o eliminar rutas
                        .requestMatchers(HttpMethod.POST, "/routes").hasAnyRole("ADMIN", "EMPRESA")
                        .requestMatchers(HttpMethod.PUT, "/routes/*").hasAnyRole("ADMIN", "EMPRESA")
                        .requestMatchers(HttpMethod.DELETE, "/routes/*").hasAnyRole("ADMIN", "EMPRESA")

                        // Reservas
                        .requestMatchers(HttpMethod.POST, "/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET, "/reservations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/reservations/user/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/reservations/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/reservations/*/confirm").hasRole("ADMIN")

                        // Pagos
                        .requestMatchers(HttpMethod.POST, "/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/payments/simulate").authenticated()
                        .requestMatchers(HttpMethod.POST, "/payments/init").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/payments/status/*").authenticated()

                        // Usuarios: solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}