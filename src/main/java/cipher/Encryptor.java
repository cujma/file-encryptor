package cipher;

import java.nio.file.Path;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;


public class Encryptor extends FileCipher{

    public Encryptor(Path inputPath, Path outputPath) {
        super(inputPath, outputPath);
    }

    public Encryptor(Path inputPath) {
        super(inputPath);
    }

    private byte[] generateNonce(){
        byte[] nonce = new byte[12];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);

        return nonce;
    }

    private byte[] encryptData(byte[] data, SecretKey key, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        return cipher.doFinal(data);
    }

    private byte[] combineData(byte[] nonce, byte[] encryptedData){
        byte[] outputData = new byte[nonce.length + encryptedData.length];

        System.arraycopy(nonce, 0, outputData, 0, nonce.length);
        System.arraycopy(encryptedData, 0, outputData, nonce.length, encryptedData.length);

        return outputData;
    }

    public void encrypt(SecretKey key, SaveMode saveMode) throws IOException, GeneralSecurityException {
        byte[] data = readInput();
        byte[] nonce = generateNonce();
        byte[] encryptedData = encryptData(data, key, nonce);
        byte[] outputData = combineData(nonce, encryptedData);

        saveOutput(outputData, saveMode);
    }
}
