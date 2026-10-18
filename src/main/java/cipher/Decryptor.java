package cipher;

import java.util.Arrays;
import java.util.Objects;
import java.io.IOException;
import java.nio.file.Path;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.GeneralSecurityException;

public class Decryptor extends FileCipher {

    private static final int SALT_LENGTH = 16;
    private static final int NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int HEADER_LENGTH = SALT_LENGTH + NONCE_LENGTH;
    private static final int MIN_ENCRYPTED_FILE_LENGTH = HEADER_LENGTH + GCM_TAG_LENGTH;

    public Decryptor(Path inputPath, Path outputPath){
        super(inputPath, outputPath);
    }

    public Decryptor(Path inputPath){
        super(inputPath);
    }

    private byte[] decryptData(byte[] encryptedData, SecretKey key, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        return cipher.doFinal(encryptedData);
    }

    public void decrypt(char[] password, SaveMode saveMode) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(saveMode, "saveMode");

        if (password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        byte[] fileData = readInput();
        EncryptedFileData encryptedFile = parseEncryptedFile(fileData);

        SecretKey key = KeyDerivation.deriveKey(password, encryptedFile.salt());
        byte[] decryptedData = decryptData(encryptedFile.encryptedData(), key, encryptedFile.nonce());

        saveOutput(decryptedData, saveMode);
    }

    private EncryptedFileData parseEncryptedFile(byte[] fileData) throws IOException {
        Objects.requireNonNull(fileData, "fileData");

        if (fileData.length < MIN_ENCRYPTED_FILE_LENGTH){
            throw new IOException("Encrypted file is too short or invalid.");
        }

        byte[] salt = Arrays.copyOfRange(fileData, 0, SALT_LENGTH);
        byte[] nonce = Arrays.copyOfRange(fileData, SALT_LENGTH, HEADER_LENGTH);
        byte[] encryptedData = Arrays.copyOfRange(fileData, HEADER_LENGTH, fileData.length);

        return new EncryptedFileData(salt, nonce, encryptedData);
    }

    private record EncryptedFileData(byte[] salt, byte[] nonce, byte[] encryptedData){}

}