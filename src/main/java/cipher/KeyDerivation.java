package cipher;

import java.util.Objects;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public final class KeyDerivation {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH_BITS = 256;

    private KeyDerivation(){

    }

    public static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(salt, "salt");

        if (password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        if (salt.length != SALT_LENGTH) {
            throw new IllegalArgumentException("Salt must be 16 bytes long.");
        }

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();

            if(keyBytes == null || keyBytes.length != KEY_LENGTH_BITS / 8){
                throw new GeneralSecurityException("Invalid derived key length.");
            }

            try{
                return new SecretKeySpec(keyBytes, "AES");
            } finally{
                Arrays.fill(keyBytes, (byte) 0);
            }
        } finally{
            spec.clearPassword();
        }
    }

}
