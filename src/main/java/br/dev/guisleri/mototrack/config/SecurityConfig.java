package br.dev.guisleri.mototrack.config;

import br.dev.guisleri.mototrack.dto.ApiErrorResponseDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain createFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) ->
                                writeErrorResponse(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "Acesso negado.",
                                        objectMapper
                                )
                        )
                )

                .httpBasic(basic -> basic
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader(
                                    HttpHeaders.WWW_AUTHENTICATE,
                                    "Basic realm=\"Realm\""
                            );
                            writeErrorResponse(
                                    response,
                                    HttpStatus.UNAUTHORIZED,
                                    "Autenticação necessária ou credenciais inválidas.",
                                    objectMapper
                            );
                        })
                );

        return http.build();
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String message,
            ObjectMapper objectMapper
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponseDTO(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        Map.of()
                )
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
