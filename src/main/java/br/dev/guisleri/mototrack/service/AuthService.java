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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(30);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            Clock clock,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
    }

    public AuthTokenResponseDTO login(LoginRequestDTO loginRequestDTO) {
        UsernamePasswordAuthenticationToken authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequestDTO.email(),
                        loginRequestDTO.password()
                );

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(authenticationRequest);
        } catch (AuthenticationException exception) {
            LOGGER.warn("Falha de login para {}", maskEmail(loginRequestDTO.email()));
            throw exception;
        }

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com email não encontrado"
                ));

        String accessToken = generateAccessToken(user.getEmail());

        String rawRefreshToken = issueRefreshToken(
                user.getId(),
                Instant.now(clock)
        );

        return new AuthTokenResponseDTO(
                accessToken,
                rawRefreshToken
        );

    }

    /** Evita gravar o e-mail inteiro nos logs: "marcos@gmail.com" vira "m***@gmail.com". */
    static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "(vazio)";
        }

        int at = email.indexOf('@');

        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokenResponseDTO refresh(RefreshTokenRequestDTO request) {
        String tokenHash =
                refreshTokenGenerator.hash(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
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
                .orElseGet(() -> {
                    refreshTokenRepository.deleteById(refreshToken.getId());
                    throw new InvalidRefreshTokenException(
                            "Refresh token inválido."
                    );
                });

        refreshTokenRepository.deleteById(refreshToken.getId());

        String newRawRefreshToken = issueRefreshToken(
                user.getId(),
                currentInstant
        );

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
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(refreshToken ->
                        refreshTokenRepository.deleteById(refreshToken.getId())
                );
    }

    /**
     * Fecha todas as sessões do usuário e abre uma nova: usado quando algo que
     * identifica a conta muda (o e-mail é o assunto do token de acesso).
     */
    @Transactional
    public AuthTokenResponseDTO issueNewSession(User user) {
        refreshTokenRepository.deleteAllByUserId(user.getId());

        return new AuthTokenResponseDTO(
                generateAccessToken(user.getEmail()),
                issueRefreshToken(user.getId(), Instant.now(clock))
        );
    }

    private String generateAccessToken(String subject) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_VALIDITY);

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

    private String issueRefreshToken(Long userId, Instant issuedAt) {
        String rawRefreshToken = refreshTokenGenerator.generate();
        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        RefreshToken refreshToken = RefreshToken.create(
                userId,
                tokenHash,
                issuedAt.plus(REFRESH_TOKEN_VALIDITY)
        );

        refreshTokenRepository.save(refreshToken);

        return rawRefreshToken;
    }

}
