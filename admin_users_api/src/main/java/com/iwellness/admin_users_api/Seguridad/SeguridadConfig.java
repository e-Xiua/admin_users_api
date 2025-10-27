package com.iwellness.admin_users_api.Seguridad;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    private static final Logger logger = LoggerFactory.getLogger(SeguridadConfig.class);

    @Autowired
    private JwtAuthEntryPoint jwtAuthEntryPoint;
    
    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Deshabilitar CSRF
            .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Sin sesiones
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/auth/**").permitAll() // Permitir acceso público a los endpoints de autenticación
                .requestMatchers("/actuator/**").permitAll() // Permitir acceso público a los endpoints de actuator (health checks)
                .requestMatchers("/usuarios/**").hasAnyAuthority("Admin", "Proveedor", "Turista")
                .requestMatchers("/debug/**").hasAuthority("Admin")// Permitir acceso a endpoints de debug
                .requestMatchers("/admin/**").hasAuthority("Admin") // Solo administradores pueden acceder a /admin/**
                .anyRequest().authenticated() // Todas las demás rutas requieren autenticación
            )
            .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthEntryPoint)); // Manejar errores de autenticación
        
        // Agregar el filtro JWT antes del filtro de autenticación de Spring Security
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(rawPassword.toString().getBytes());
                    String result = Base64.getEncoder().encodeToString(hash);
                    
                    logger.info("=============== ENCODER DEBUG ===============");
                    logger.info("Contrasena sin procesar: {}", rawPassword);
                    logger.info("Hash generado: {}", result);
                    logger.info("Clase que llamo a encode: {}", new Exception().getStackTrace()[1].getClassName());
                    logger.info("Metodo que llamo a encode: {}", new Exception().getStackTrace()[1].getMethodName());
                    logger.info("===========================================");
                    
                    return result;
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("SHA-256 algorithm not found", e);
                }
            }
            
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                String generatedHash = encode(rawPassword);
                
                logger.info("=============== MATCHER DEBUG ===============");
                logger.info("Contrasena sin procesar: {}", rawPassword);
                logger.info("Hash generado para comparacion: {}", generatedHash);
                logger.info("Hash almacenado para comparacion: {}", encodedPassword);
                logger.info("¿Coinciden?: {}", generatedHash.equals(encodedPassword));
                logger.info("===========================================");
                
                return generatedHash.equals(encodedPassword);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authManagerBuilder.authenticationProvider(customAuthenticationProvider);
        return authManagerBuilder.build();
    }

    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter(); // Filtro personalizado para JWT
    }
}