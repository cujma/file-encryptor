package cipher;

import java.util.Objects;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;



public class Encryptor extends FileCipher{

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public Encryptor(Path inputPath, Path outputPath) {
        super(inputPath, outputPath);
    }

    public Encryptor(Path inputPath) {
        super(inputPath);
    }

    private byte[] generateNonce(){
        byte[] nonce = new byte[12];

        SECURE_RANDOM.nextBytes(nonce);

        return nonce;
    }

    private byte[] encryptData(byte[] data, SecretKey key, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        return cipher.doFinal(data);
    }

    private byte[] combineData(byte[] salt, byte[] nonce, byte[] encryptedData) {
        byte[] outputData = new byte[salt.length + nonce.length + encryptedData.length];

        System.arraycopy(salt, 0, outputData, 0, salt.length);
        System.arraycopy(nonce, 0, outputData, salt.length, nonce.length);
        System.arraycopy(encryptedData, 0, outputData, salt.length + nonce.length, encryptedData.length);

        return outputData;
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    public void encrypt(char[] password, SaveMode saveMode) throws IOException, GeneralSecurityException {
        byte[] data;
        byte[] salt;
        byte[] nonce;
        byte[] encryptedData;
        byte[] outputData;
        SecretKey key;

        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(saveMode, "saveMode");

        data = readInput();
        salt = generateSalt();
        key = KeyDerivation.deriveKey(password, salt);
        nonce = generateNonce();

        encryptedData = encryptData(data, key, nonce);
        outputData = combineData(salt, nonce, encryptedData);

        saveOutput(outputData, saveMode);
    }
}
