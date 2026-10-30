package cipher;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoubleConsumer;

public class Decryptor extends FileCipher {
    private static final int HEADER_LENGTH = MAGIC.length + 1 + SALT_LENGTH + NONCE_PREFIX_LENGTH;

    public Decryptor(Path inputPath, Path outputPath) {
        super(inputPath, outputPath);
    }

    public void decrypt(char[] password, SaveMode saveMode) throws IOException, GeneralSecurityException {
        decrypt(password, saveMode, progress -> {
        });
    }

    public void decrypt(char[] password, SaveMode saveMode, DoubleConsumer progress)
            throws IOException, GeneralSecurityException {
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(saveMode, "Save mode cannot be null");
        Objects.requireNonNull(progress, "Progress callback cannot be null");

        Path tempFile = createTempOutputFile(saveMode);
        long totalBytes = Files.size(inputPath);
        long processedBytes = 0;

        byte[] header = new byte[HEADER_LENGTH];
        byte[] salt = null;
        byte[] noncePrefix = null;

        try {
            try (DataInputStream input = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(inputPath)))) {
                try {
                    input.readFully(header);
                } catch (EOFException e) {
                    throw new IOException("Encrypted file is incomplete.", e);
                }

                validateHeader(header);
                processedBytes = HEADER_LENGTH;
                progress.accept(Math.min(1.0, (double) processedBytes / totalBytes));

                int offset = MAGIC.length + 1;

                salt = Arrays.copyOfRange(header, offset, offset + SALT_LENGTH);
                offset += SALT_LENGTH;

                noncePrefix = Arrays.copyOfRange(header, offset, offset + NONCE_PREFIX_LENGTH);

                SecretKey key = KeyDerivation.deriveKey(password, salt);

                try (DataOutputStream output = new DataOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                    long counter = 0;

                    while (true) {
                        int length;

                        try {
                            length = input.readInt();
                        } catch (EOFException e) {
                            throw new IOException("Encrypted file is truncated: missing end marker.", e);
                        }

                        if (length < 0 || length > CHUNK_SIZE) {
                            throw new IOException("Invalid encrypted block length.");
                        }

                        if (counter > 0xFFFF_FFFFL) {
                            throw new IOException("Maximum number of blocks exceeded.");
                        }

                        if (length == 0) {
                            byte[] finalTag = new byte[TAG_LENGTH_BYTES];

                            try {
                                input.readFully(finalTag);
                                processedBytes += Integer.BYTES + finalTag.length;

                                byte[] finalData = decryptChunk(finalTag, key, noncePrefix, header, counter, 0);

                                if (finalData.length != 0) {
                                    throw new IOException("Invalid encrypted file end marker.");
                                }

                                Arrays.fill(finalData, (byte) 0);
                            } catch (EOFException e) {
                                throw new IOException("Encrypted file has an incomplete end marker.", e);
                            } finally {
                                Arrays.fill(finalTag, (byte) 0);
                            }

                            if (input.read() != -1) {
                                throw new IOException("Unexpected data after the end of the encrypted file.");
                            }
                            break;
                        }

                        byte[] encrypted = new byte[length + TAG_LENGTH_BYTES];

                        try {
                            input.readFully(encrypted);

                            byte[] plaintext = decryptChunk(encrypted, key, noncePrefix, header, counter, length);

                            try {
                                if (plaintext.length != length) {
                                    throw new IOException("Decrypted block has an invalid length.");
                                }

                                output.write(plaintext);
                            } finally {
                                Arrays.fill(plaintext, (byte) 0);
                            }
                        } catch (EOFException e) {
                            throw new IOException("Encrypted file contains an incomplete block.", e);
                        } finally {
                            Arrays.fill(encrypted, (byte) 0);
                        }
                        processedBytes += Integer.BYTES + encrypted.length;
                        progress.accept(Math.min(1.0, (double) processedBytes / totalBytes));
                        counter++;
                    }
                }
            }

            commitOutput(tempFile, saveMode);
            progress.accept(1.0);

        } finally {
            Files.deleteIfExists(tempFile);
            Arrays.fill(header, (byte) 0);

            if (salt != null) {
                Arrays.fill(salt, (byte) 0);
            }

            if (noncePrefix != null) {
                Arrays.fill(noncePrefix, (byte) 0);
            }
        }
    }

    private void validateHeader(byte[] header) throws IOException {
        for (int i = 0; i < MAGIC.length; i++) {
            if (header[i] != MAGIC[i]) {
                throw new IOException("Unknown encrypted file format.");
            }
        }

        if (header[MAGIC.length] != VERSION) {
            throw new IOException("Unsupported encrypted file version.");
        }
    }

    private byte[] decryptChunk(byte[] encrypted, SecretKey key, byte[] noncePrefix, byte[] header, long counter, int length) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] nonce = createNonce(noncePrefix, counter);

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

            cipher.updateAAD(header);
            cipher.updateAAD(intToBytes((int) counter));
            cipher.updateAAD(intToBytes(length));

            return cipher.doFinal(encrypted);

        } catch (AEADBadTagException e) {
            throw new AEADBadTagException(
                    "Authentication failed. The password may be incorrect " + "or the encrypted file may be damaged."
            );
        } finally {
            Arrays.fill(nonce, (byte) 0);
        }
    }

}
