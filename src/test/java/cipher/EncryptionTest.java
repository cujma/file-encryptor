package cipher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.AEADBadTagException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptionTest {
    private static final int TEST_CHUNK_SIZE = 64 * 1024;

    @TempDir
    Path tempDir;

    @Test
    void encryptsAndDecryptsEmptyFile() throws IOException, GeneralSecurityException {
        Path input = file("empty.txt");
        Path encrypted = file("empty.txt.enc");
        Path decrypted = file("empty-restored.txt");
        Files.createFile(input);

        encryptAndDecrypt(input, encrypted, decrypted);

        assertArrayEquals(Files.readAllBytes(input), Files.readAllBytes(decrypted));
    }

    @Test
    void rejectsWrongPassword() throws IOException, GeneralSecurityException {
        Path input = file("message.txt");
        Path encrypted = file("message.txt.enc");
        Path decrypted = file("message-restored.txt");
        Files.writeString(input, "secret message");

        encrypt(input, encrypted);

        char[] wrongPassword = "wrong-password".toCharArray();
        try {
            assertThrows(AEADBadTagException.class,
                    () -> new Decryptor(encrypted, decrypted)
                            .decrypt(wrongPassword, SaveMode.SEPARATE_FILE));
        } finally {
            Arrays.fill(wrongPassword, '\0');
        }
        assertFalse(Files.exists(decrypted));
    }

    @Test
    void rejectsDamagedFile() throws IOException, GeneralSecurityException {
        Path input = file("message.txt");
        Path encrypted = file("message.txt.enc");
        Path decrypted = file("message-restored.txt");
        Files.writeString(input, "secret message");

        encrypt(input, encrypted);
        byte[] contents = Files.readAllBytes(encrypted);
        contents[0] = 'X';
        Files.write(encrypted, contents);

        assertDecryptFails(IOException.class, encrypted, decrypted);
    }

    @Test
    void handlesDataLargerThanOneChunk() throws IOException, GeneralSecurityException {
        Path input = file("large.bin");
        Path encrypted = file("large.bin.enc");
        Path decrypted = file("large-restored.bin");
        byte[] data = new byte[TEST_CHUNK_SIZE * 2 + 17];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 251);
        }
        Files.write(input, data);

        encryptAndDecrypt(input, encrypted, decrypted);

        assertArrayEquals(data, Files.readAllBytes(decrypted));
    }

    @Test
    void rejectsTruncatedEncryptedFile() throws IOException, GeneralSecurityException {
        Path input = file("message.txt");
        Path encrypted = file("message.txt.enc");
        Path decrypted = file("message-restored.txt");
        Files.writeString(input, "secret message");
        encrypt(input, encrypted);

        byte[] contents = Files.readAllBytes(encrypted);
        Files.write(encrypted, Arrays.copyOf(contents, contents.length - 1));

        assertDecryptFails(IOException.class, encrypted, decrypted);
    }

    @Test
    void rejectsTrailingData() throws IOException, GeneralSecurityException {
        Path input = file("message.txt");
        Path encrypted = file("message.txt.enc");
        Path decrypted = file("message-restored.txt");
        Files.writeString(input, "secret message");
        encrypt(input, encrypted);

        Files.write(encrypted, new byte[]{1}, StandardOpenOption.APPEND);

        assertDecryptFails(IOException.class, encrypted, decrypted);
    }

    @Test
    void replacesOriginalFile() throws IOException, GeneralSecurityException {
        Path input = file("message.txt");
        Files.writeString(input, "secret message");

        encrypt(input, input, SaveMode.REPLACE_ORIGINAL);

        assertTrue(Files.exists(input));

        Path restored = file("restored.txt");
        decrypt(input, restored);
        assertEquals("secret message", Files.readString(restored));
    }

    @Test
    void decryptsAndReplacesOriginalFile() throws IOException, GeneralSecurityException {
        Path input = file("message.txt");
        Path encrypted = file("message.txt.enc");
        Files.writeString(input, "secret message");
        encrypt(input, encrypted);

        decrypt(encrypted, encrypted, SaveMode.REPLACE_ORIGINAL);

        assertEquals("secret message", Files.readString(encrypted));
    }

    @Test
    void failedReplaceDoesNotChangeEncryptedFile() throws IOException, GeneralSecurityException {
        Path input = file("message.txt");
        Path encrypted = file("message.txt.enc");
        Files.writeString(input, "secret message");
        encrypt(input, encrypted);
        byte[] original = Files.readAllBytes(encrypted);

        char[] wrongPassword = "wrong-password".toCharArray();
        try {
            assertThrows(AEADBadTagException.class,
                    () -> new Decryptor(encrypted, encrypted)
                            .decrypt(wrongPassword, SaveMode.REPLACE_ORIGINAL));
        } finally {
            Arrays.fill(wrongPassword, '\0');
        }

        assertArrayEquals(original, Files.readAllBytes(encrypted));
    }

    @Test
    void rejectsExistingOutputFile() throws IOException {
        Path input = file("message.txt");
        Path output = file("message.enc");
        Files.writeString(input, "secret message");
        Files.createFile(output);

        assertThrows(IOException.class, () -> encrypt(input, output));
    }

    @Test
    void rejectsEmptyPassword() throws IOException {
        Path input = file("message.txt");
        Path output = file("message.enc");
        Files.writeString(input, "secret message");

        assertThrows(IllegalArgumentException.class,
                () -> new Encryptor(input, output).encrypt(new char[0], SaveMode.SEPARATE_FILE));
    }

    private void encryptAndDecrypt(Path input, Path encrypted, Path decrypted)
            throws IOException, GeneralSecurityException {
        encrypt(input, encrypted);
        decrypt(encrypted, decrypted);
    }

    private void encrypt(Path input, Path output) throws IOException, GeneralSecurityException {
        encrypt(input, output, SaveMode.SEPARATE_FILE);
    }

    private void encrypt(Path input, Path output, SaveMode mode)
            throws IOException, GeneralSecurityException {
        char[] password = password();
        try {
            new Encryptor(input, output).encrypt(password, mode);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void decrypt(Path input, Path output) throws IOException, GeneralSecurityException {
        decrypt(input, output, SaveMode.SEPARATE_FILE);
    }

    private void decrypt(Path input, Path output, SaveMode mode)
            throws IOException, GeneralSecurityException {
        char[] password = password();
        try {
            new Decryptor(input, output).decrypt(password, mode);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void assertDecryptFails(Class<? extends Throwable> type, Path input, Path output) {
        char[] password = password();
        try {
            assertThrows(type, () -> new Decryptor(input, output)
                    .decrypt(password, SaveMode.SEPARATE_FILE));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private Path file(String name) {
        return tempDir.resolve(name);
    }

    private char[] password() {
        return "test-password".toCharArray();
    }
}
