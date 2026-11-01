package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.model.VerificationPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera códigos numéricos curtos e guarda só o hash deles. Como são só 1.000.000
 * de combinações, o hash usa uma chave do servidor (HMAC): quem lesse o banco não
 * conseguiria descobrir os códigos por força bruta sem essa chave.
 */
@Component
public class VerificationCodeGenerator {

    public static final int CODE_LENGTH = 6;

    private static final int CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] key;

    public VerificationCodeGenerator(
            @Value("${security.jwt.secret}") String secret
    ) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generate() {
        return String.format("%0" + CODE_LENGTH + "d", secureRandom.nextInt(CODE_BOUND));
    }

    public String hash(Long userId, VerificationPurpose purpose, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));

            byte[] hash = mac.doFinal(
                    "%d:%s:%s".formatted(userId, purpose.name(), code)
                            .getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "HmacSHA256 is not available.",
                    exception
            );
        }
    }

    public boolean matches(
            String expectedHash,
            Long userId,
            VerificationPurpose purpose,
            String code
    ) {
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                hash(userId, purpose, code).getBytes(StandardCharsets.UTF_8)
        );
    }
}
