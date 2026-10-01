package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.AuthTokenResponseDTO;
import br.dev.guisleri.mototrack.dto.LoginRequestDTO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    public AuthTokenResponseDTO login(LoginRequestDTO loginRequestDTO) {
        UsernamePasswordAuthenticationToken authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequestDTO.email(),
                loginRequestDTO.password()
        );

        Authentication authentication = authenticationManager.authenticate(authenticationRequest);

        String accessToken = generateAccessToken(authentication);

        return new AuthTokenResponseDTO(accessToken);

    }

    private String generateAccessToken(Authentication authentication) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(15, ChronoUnit.MINUTES);

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwtClaimsSet.builder()
                                .issuedAt(issuedAt)
                                .expiresAt(expiresAt)
                                .subject(authentication.getName())
                                .build()
                )
        ).getTokenValue();
    }

}
