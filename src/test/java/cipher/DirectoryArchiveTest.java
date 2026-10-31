package cipher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryArchiveTest {
    @TempDir
    Path tempDir;

    @Test
    void archivesNestedFilesAndEmptyDirectories() throws IOException {
        Path source = tempDir.resolve("source");
        Path nested = source.resolve("documents");
        Files.createDirectories(nested);
        Files.writeString(source.resolve("readme.txt"), "hello");
        Files.writeString(nested.resolve("note.txt"), "private");
        Files.createDirectory(source.resolve("empty"));

        Path archive = tempDir.resolve("archive.zip");
        Path restored = tempDir.resolve("restored");
        DirectoryArchive.create(source, archive);
        DirectoryArchive.extract(archive, restored);

        assertEquals("hello", Files.readString(restored.resolve("readme.txt")));
        assertEquals("private", Files.readString(restored.resolve("documents/note.txt")));
        assertTrue(Files.isDirectory(restored.resolve("empty")));
    }

    @Test
    void rejectsUnsafeArchivePaths() throws IOException {
        Path archive = tempDir.resolve("unsafe.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("../outside.txt"));
            output.write("unsafe".getBytes());
            output.closeEntry();
        }

        assertThrows(IOException.class,
                () -> DirectoryArchive.extract(archive, tempDir.resolve("restored")));
    }

    @Test
    void encryptsAndDecryptsDirectoryPayload() throws IOException, java.security.GeneralSecurityException {
        Path source = tempDir.resolve("source");
        Files.createDirectories(source.resolve("empty"));
        Files.writeString(source.resolve("note.txt"), "directory payload");
        Path archive = tempDir.resolve("source.zip");
        Path encrypted = tempDir.resolve("source.enc");
        Path restoredArchive = tempDir.resolve("restored.zip");

        DirectoryArchive.create(source, archive);
        char[] password = "test-password".toCharArray();
        try {
            new Encryptor(archive, encrypted, PayloadType.DIRECTORY)
                    .encrypt(password, SaveMode.SEPARATE_FILE);
            Decryptor decryptor = new Decryptor(encrypted, restoredArchive);
            decryptor.decrypt(password, SaveMode.SEPARATE_FILE);
            assertEquals(PayloadType.DIRECTORY, decryptor.payloadType());
        } finally {
            java.util.Arrays.fill(password, '\0');
        }

        Path restored = tempDir.resolve("restored");
        DirectoryArchive.extract(restoredArchive, restored);
        assertEquals("directory payload", Files.readString(restored.resolve("note.txt")));
        assertTrue(Files.isDirectory(restored.resolve("empty")));
    }
}
