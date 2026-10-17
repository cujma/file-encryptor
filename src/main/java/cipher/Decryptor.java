package cipher;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

public class Decryptor extends FileCipher {

    public Decryptor(Path inputPath, Path outputPath) {
        super(inputPath, outputPath);
    }

    public Decryptor(Path inputPath) {
        super(inputPath);
    }

    private byte[] decryptData(byte[] encryptedData, SecretKey key, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        return cipher.doFinal(encryptedData);
    }

    public void decrypt(char[] password, SaveMode saveMode) throws IOException, GeneralSecurityException {
        byte[] fileData;
        byte[] salt;
        byte[] nonce;
        byte[] encryptedData;
        byte[] decryptedData;

        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(saveMode, "saveMode");

        fileData = readInput();

        if (fileData.length < 44){
            throw new IOException("Encrypted file is too short.");
        }

        salt = Arrays.copyOfRange(fileData, 0, 16);
        nonce = Arrays.copyOfRange(fileData, 16, 28);
        encryptedData = Arrays.copyOfRange(fileData, 28, fileData.length);

        SecretKey key = KeyDerivation.deriveKey(password, salt);
        decryptedData = decryptData(encryptedData, key, nonce);

        saveOutput(decryptedData, saveMode);
    }
}