package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidVerificationCodeException;
import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.repository.VerificationCodeRepository;
import br.dev.guisleri.mototrack.security.VerificationCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Códigos de uso único enviados por e-mail. Cada usuário tem no máximo um código por
 * finalidade: pedir outro invalida o anterior. Um código vale por 15 minutos e
 * aceita até 5 tentativas erradas.
 */
@Service
public class VerificationCodeService {

    public static final Duration VALIDITY = Duration.ofMinutes(15);
    public static final Duration COOLDOWN = Duration.ofMinutes(1);

    // A mesma mensagem para código errado, vencido ou inexistente: não revela qual.
    static final String INVALID_CODE_MESSAGE = "Código inválido ou expirado.";

    private final VerificationCodeRepository verificationCodeRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final Clock clock;

    public VerificationCodeService(
            VerificationCodeRepository verificationCodeRepository,
            VerificationCodeGenerator verificationCodeGenerator,
            Clock clock
    ) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.verificationCodeGenerator = verificationCodeGenerator;
        this.clock = clock;
    }

    /** Se já foi enviado um código para isto há menos de um minuto. */
    @Transactional
    public boolean wasIssuedRecently(Long userId, VerificationPurpose purpose) {
        Instant limit = Instant.now(clock).minus(COOLDOWN);

        return verificationCodeRepository
                .findByUserIdAndPurposeForUpdate(userId, purpose)
                .map(code -> code.wasCreatedAfter(limit))
                .orElse(false);
    }

    /** Cria um código novo (apagando o anterior) e devolve o texto dele, que só existe agora. */
    @Transactional
    public String issue(Long userId, VerificationPurpose purpose, String newEmail) {
        verificationCodeRepository.deleteByUserIdAndPurpose(userId, purpose);

        String code = verificationCodeGenerator.generate();
        Instant now = Instant.now(clock);

        verificationCodeRepository.save(
                VerificationCode.create(
                        userId,
                        purpose,
                        verificationCodeGenerator.hash(userId, purpose, code),
                        newEmail,
                        now,
                        now.plus(VALIDITY)
                )
        );

        return code;
    }

    /**
     * Confere e consome o código. Errado conta uma tentativa (e na quinta o código
     * morre); certo apaga o código e o devolve. As tentativas erradas ficam gravadas
     * mesmo com a exceção, por isso o noRollbackFor.
     */
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public VerificationCode consume(
            Long userId,
            VerificationPurpose purpose,
            String code
    ) {
        VerificationCode verificationCode = verificationCodeRepository
                .findByUserIdAndPurposeForUpdate(userId, purpose)
                .orElseThrow(() -> new InvalidVerificationCodeException(INVALID_CODE_MESSAGE));

        if (verificationCode.isExpired(Instant.now(clock)) || !verificationCode.hasAttemptsLeft()) {
            verificationCodeRepository.deleteById(verificationCode.getId());

            throw new InvalidVerificationCodeException(INVALID_CODE_MESSAGE);
        }

        if (!verificationCodeGenerator.matches(
                verificationCode.getCodeHash(),
                userId,
                purpose,
                code
        )) {
            verificationCode.registerFailedAttempt();

            if (verificationCode.hasAttemptsLeft()) {
                verificationCodeRepository.save(verificationCode);
            } else {
                verificationCodeRepository.deleteById(verificationCode.getId());
            }

            throw new InvalidVerificationCodeException(INVALID_CODE_MESSAGE);
        }

        verificationCodeRepository.deleteById(verificationCode.getId());

        return verificationCode;
    }

}
