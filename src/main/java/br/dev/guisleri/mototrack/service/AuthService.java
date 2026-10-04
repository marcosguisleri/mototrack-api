package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.AuthTokenResponseDTO;
import br.dev.guisleri.mototrack.dto.LoginRequestDTO;
import br.dev.guisleri.mototrack.dto.RefreshTokenRequestDTO;
import br.dev.guisleri.mototrack.exception.InvalidRefreshTokenException;
import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.RefreshToken;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.RefreshTokenRepository;
import br.dev.guisleri.mototrack.repository.UserRepository;
import br.dev.guisleri.mototrack.security.RefreshTokenGenerator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, Clock clock, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, RefreshTokenGenerator refreshTokenGenerator) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
    }

    public AuthTokenResponseDTO login(LoginRequestDTO loginRequestDTO) {
        UsernamePasswordAuthenticationToken authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequestDTO.email(),
                loginRequestDTO.password()
        );

        Authentication authentication = authenticationManager.authenticate(authenticationRequest);

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com email não encontrado"
                ));

        String accessToken = generateAccessToken(user.getEmail());

        String rawRefreshToken = refreshTokenGenerator.generate();

        String tokenHash =
                refreshTokenGenerator.hash(rawRefreshToken);

        Instant refreshTokenExpiresAt =
                Instant.now(clock).plus(30, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.create(
                user.getId(),
                tokenHash,
                refreshTokenExpiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return new AuthTokenResponseDTO(
                accessToken,
                rawRefreshToken
        );

    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokenResponseDTO refresh(RefreshTokenRequestDTO request) {
        String tokenHash =
                refreshTokenGenerator.hash(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "Refresh token inválido."
                ));

        Instant currentInstant = Instant.now(clock);

        if (refreshToken.isExpired(currentInstant)) {
            refreshTokenRepository.deleteById(refreshToken.getId());

            throw new InvalidRefreshTokenException(
                    "Refresh token expirado."
            );
        }

        User user = userRepository
                .findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "Refresh token inválido."
                ));

        refreshTokenRepository.deleteById(refreshToken.getId());

        String newRawRefreshToken =
                refreshTokenGenerator.generate();

        String newTokenHash =
                refreshTokenGenerator.hash(newRawRefreshToken);

        Instant newRefreshTokenExpiresAt =
                currentInstant.plus(30, ChronoUnit.DAYS);

        RefreshToken newRefreshToken = RefreshToken.create(
                user.getId(),
                newTokenHash,
                newRefreshTokenExpiresAt
        );

        refreshTokenRepository.save(newRefreshToken);

        String accessToken =
                generateAccessToken(user.getEmail());

        return new AuthTokenResponseDTO(
                accessToken,
                newRawRefreshToken
        );
    }

    @Transactional
    public void logout(RefreshTokenRequestDTO request) {
        String tokenHash =
                refreshTokenGenerator.hash(request.refreshToken());

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(refreshToken ->
                        refreshTokenRepository.deleteById(refreshToken.getId())
                );
    }

    private String generateAccessToken(String subject) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(15, ChronoUnit.MINUTES);

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwtClaimsSet.builder()
                                .issuedAt(issuedAt)
                                .expiresAt(expiresAt)
                                .subject(subject)
                                .build()
                )
        ).getTokenValue();
    }

}
