package cipher;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

public class Encryptor extends FileCipher {
    private final SecureRandom secureRandom = new SecureRandom();

    public Encryptor(Path inputPath, Path outputPath) {
        super(inputPath, outputPath);
    }

    public void encrypt(char[] password, SaveMode saveMode) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(saveMode, "Save mode cannot be null");

        byte[] salt = new byte[SALT_LENGTH];
        byte[] noncePrefix = new byte[NONCE_PREFIX_LENGTH];

        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(noncePrefix);

        SecretKey key = KeyDerivation.deriveKey(password, salt);
        byte[] header = createHeader(salt, noncePrefix);
        Path tempFile = createTempOutputFile(saveMode);
        byte[] buffer = new byte[CHUNK_SIZE];

        try {
            try (InputStream input = new BufferedInputStream(Files.newInputStream(inputPath));
                 DataOutputStream output = new DataOutputStream(
                         new BufferedOutputStream(Files.newOutputStream(tempFile)))) {

                output.write(header);

                long counter = 0;

                while (true) {
                    int length = readChunk(input, buffer);

                    if (length == 0) {
                        break;
                    }

                    if (counter > 0xFFFF_FFFFL) {
                        throw new IOException("Maximum number of encrypted chunks exceeded.");
                    }

                    byte[] plaintext = Arrays.copyOf(buffer, length);
                    byte[] encrypted = null;
                    try {
                        encrypted = encryptChunk(plaintext, key, noncePrefix, header, counter, length);

                        output.writeInt(length);
                        output.write(encrypted);
                    } finally {
                        Arrays.fill(plaintext, (byte) 0);
                        if (encrypted != null) {
                            Arrays.fill(encrypted, (byte) 0);
                        }
                    }

                    counter++;
                }

                if (counter > 0xFFFF_FFFFL) {
                    throw new IOException("Maximum number of encrypted chunks exceeded.");
                }

                byte[] finalTag = null;
                try {
                    finalTag = encryptChunk(new byte[0], key, noncePrefix, header, counter, 0);

                    output.writeInt(0);
                    output.write(finalTag);
                } finally {
                    if (finalTag != null) {
                        Arrays.fill(finalTag, (byte) 0);
                    }
                }
            }

            commitOutput(tempFile, saveMode);

        } finally {
            Files.deleteIfExists(tempFile);
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(noncePrefix, (byte) 0);
            Arrays.fill(header, (byte) 0);
            Arrays.fill(buffer, (byte) 0);
        }
    }

    private byte[] createHeader(byte[] salt, byte[] noncePrefix) {
        byte[] header = new byte[MAGIC.length + 1 + SALT_LENGTH + NONCE_PREFIX_LENGTH];

        int offset = 0;

        System.arraycopy(MAGIC, 0, header, offset, MAGIC.length);
        offset += MAGIC.length;

        header[offset++] = VERSION;

        System.arraycopy(salt, 0, header, offset, salt.length);
        offset += salt.length;

        System.arraycopy(noncePrefix, 0, header, offset, noncePrefix.length);

        return header;
    }

    private byte[] encryptChunk(byte[] plaintext, SecretKey key, byte[] noncePrefix, byte[] header, long counter, int length) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] nonce = createNonce(noncePrefix, counter);

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            cipher.updateAAD(header);
            cipher.updateAAD(intToBytes((int) counter));
            cipher.updateAAD(intToBytes(length));

            return cipher.doFinal(plaintext);
        } finally {
            Arrays.fill(nonce, (byte) 0);
        }
    }

    private int readChunk(InputStream input, byte[] buffer) throws IOException {
        int total = 0;

        while (total < buffer.length) {
            int read = input.read(buffer, total, buffer.length - total);

            if (read == -1) {
                break;
            }

            if (read == 0) {
                int singleByte = input.read();
                if (singleByte == -1) {
                    break;
                }
                buffer[total++] = (byte) singleByte;
                continue;
            }

            total += read;
        }

        return total;
    }
}
