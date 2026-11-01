package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidVerificationCodeException;
import br.dev.guisleri.mototrack.mail.EmailSender;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.repository.RefreshTokenRepository;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeService verificationCodeService;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    public PasswordResetService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            VerificationCodeService verificationCodeService,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationCodeService = verificationCodeService;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
    }

    /**
     * Envia o código de recuperação se a conta existir. Não diz se existe: quem chama
     * recebe a mesma resposta nos dois casos. Pedidos repetidos em menos de um minuto
     * são ignorados.
     */
    public void requestReset(String email) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(email));

        if (user.isEmpty()) {
            return;
        }

        if (verificationCodeService.wasIssuedRecently(
                user.get().getId(),
                VerificationPurpose.PASSWORD_RESET
        )) {
            return;
        }

        String code = verificationCodeService.issue(
                user.get().getId(),
                VerificationPurpose.PASSWORD_RESET,
                null
        );

        emailSender.send(
                user.get().getEmail(),
                "MotoTrack: código para redefinir sua senha",
                """
                Olá, %s!

                Seu código para redefinir a senha é: %s

                Ele vale por %d minutos. Se você não pediu isso, ignore este e-mail: \
                sua senha continua a mesma.

                MotoTrack
                """.formatted(
                        user.get().getName(),
                        code,
                        VerificationCodeService.VALIDITY.toMinutes()
                )
        );
    }

    /**
     * Troca a senha com o código recebido por e-mail e encerra todas as sessões da
     * conta. Código errado, vencido ou de conta inexistente dão a mesma resposta.
     */
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new InvalidVerificationCodeException(
                        VerificationCodeService.INVALID_CODE_MESSAGE
                ));

        verificationCodeService.consume(
                user.getId(),
                VerificationPurpose.PASSWORD_RESET,
                code
        );

        user.changePasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenRepository.deleteAllByUserId(user.getId());

        LOGGER.info("Senha redefinida por código (usuário {}).", user.getId());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
