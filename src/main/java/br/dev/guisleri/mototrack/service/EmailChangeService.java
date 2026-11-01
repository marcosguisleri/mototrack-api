package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.AuthTokenResponseDTO;
import br.dev.guisleri.mototrack.exception.InvalidCurrentPasswordException;
import br.dev.guisleri.mototrack.exception.InvalidEmailChangeException;
import br.dev.guisleri.mototrack.exception.InvalidVerificationCodeException;
import br.dev.guisleri.mototrack.exception.UserAlreadyExistsException;
import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.exception.VerificationCodeCooldownException;
import br.dev.guisleri.mototrack.mail.EmailSender;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class EmailChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailChangeService.class);

    private static final String EMAIL_TAKEN_MESSAGE =
            "Já existe um usuário cadastrado com este e-mail.";

    private final UserRepository userRepository;
    private final VerificationCodeService verificationCodeService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    public EmailChangeService(
            UserRepository userRepository,
            VerificationCodeService verificationCodeService,
            AuthService authService,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender
    ) {
        this.userRepository = userRepository;
        this.verificationCodeService = verificationCodeService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
    }

    /**
     * Pede a troca de e-mail: exige a senha atual e envia o código para o e-mail
     * NOVO, para provar que ele é de quem pede.
     */
    public void requestChange(User user, String newEmail, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException("Senha atual incorreta.");
        }

        String normalizedEmail = normalizeEmail(newEmail);

        if (normalizedEmail.equals(user.getEmail())) {
            throw new InvalidEmailChangeException("O novo e-mail é igual ao atual.");
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new UserAlreadyExistsException(EMAIL_TAKEN_MESSAGE);
        }

        if (verificationCodeService.wasIssuedRecently(
                user.getId(),
                VerificationPurpose.EMAIL_CHANGE
        )) {
            throw new VerificationCodeCooldownException(
                    "Aguarde um minuto antes de pedir outro código."
            );
        }

        String code = verificationCodeService.issue(
                user.getId(),
                VerificationPurpose.EMAIL_CHANGE,
                normalizedEmail
        );

        emailSender.send(
                normalizedEmail,
                "MotoTrack: confirme seu novo e-mail",
                """
                Olá, %s!

                Seu código para confirmar este e-mail na sua conta do MotoTrack é: %s

                Ele vale por %d minutos. Se você não pediu isso, ignore este e-mail.

                MotoTrack
                """.formatted(
                        user.getName(),
                        code,
                        VerificationCodeService.VALIDITY.toMinutes()
                )
        );
    }

    /**
     * Confirma o código e troca o e-mail. Como o e-mail identifica a conta nos tokens,
     * as sessões antigas são encerradas e uma nova é devolvida, para o app seguir logado.
     */
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public AuthTokenResponseDTO confirmChange(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com id: %s não encontrado".formatted(userId)
                ));

        VerificationCode verificationCode = verificationCodeService.consume(
                userId,
                VerificationPurpose.EMAIL_CHANGE,
                code
        );

        String oldEmail = user.getEmail();
        String newEmail = verificationCode.getNewEmail();

        // Outra conta pode ter ficado com o endereço entre o pedido e a confirmação.
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new UserAlreadyExistsException(EMAIL_TAKEN_MESSAGE);
        }

        user.changeEmail(newEmail);

        User savedUser;

        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException(EMAIL_TAKEN_MESSAGE);
        }

        AuthTokenResponseDTO tokens = authService.issueNewSession(savedUser);

        LOGGER.info("E-mail alterado (usuário {}).", savedUser.getId());

        emailSender.send(
                oldEmail,
                "MotoTrack: o e-mail da sua conta foi alterado",
                """
                Olá, %s!

                O e-mail da sua conta do MotoTrack foi trocado para %s.

                Se não foi você, redefina sua senha o quanto antes.

                MotoTrack
                """.formatted(savedUser.getName(), savedUser.getEmail())
        );

        return tokens;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
