package cipher;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

public final class KeyDerivation {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String KEY_ALGORITHM = "AES";

    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH = 16;

    private KeyDerivation() {
    }

    public static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {

        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(salt, "Salt cannot be null");

        if (password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        if (salt.length != SALT_LENGTH) {
            throw new IllegalArgumentException("Salt must be exactly " + SALT_LENGTH + " bytes long.");
        }

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);

        byte[] keyBytes = null;

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);

            keyBytes = factory.generateSecret(spec).getEncoded();

            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);

        } finally {
            spec.clearPassword();

            if (keyBytes != null) {
                Arrays.fill(keyBytes, (byte) 0);
            }
        }
    }
}
