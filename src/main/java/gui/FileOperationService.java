package gui;

import cipher.Decryptor;
import cipher.DirectoryArchive;
import cipher.Encryptor;
import cipher.PayloadType;
import cipher.SaveMode;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

public final class FileOperationService implements FileOperationHandler {
    private enum Operation {
        ENCRYPT,
        DECRYPT
    }

    @Override
    public void encrypt(Path input, Path output, char[] password, SaveMode saveMode,
                        OperationCallbacks callbacks) {
        start(input, output, password, saveMode, Operation.ENCRYPT, callbacks);
    }

    @Override
    public void decrypt(Path input, Path output, char[] password, SaveMode saveMode,
                        OperationCallbacks callbacks) {
        start(input, output, password, saveMode, Operation.DECRYPT, callbacks);
    }

    private void start(Path input, Path output, char[] password, SaveMode saveMode,
                       Operation operation, OperationCallbacks callbacks) {
        Objects.requireNonNull(input, "Input path cannot be null");
        Objects.requireNonNull(output, "Output path cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(saveMode, "Save mode cannot be null");
        Objects.requireNonNull(callbacks, "Callbacks cannot be null");

        char[] passwordChars = password.clone();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage(startMessage(operation));

                    runCipher(input, output, passwordChars, saveMode, operation,
                            progress -> updateProgress(progress, 1.0));
                    return null;
                } finally {
                    Arrays.fill(passwordChars, '\0');
                }
            }
        };

        task.messageProperty().addListener((observable, oldMessage, newMessage) -> {
            if (newMessage != null && !newMessage.isBlank()) {
                callbacks.onStatus().accept(newMessage);
            }
        });
        task.progressProperty().addListener((observable, oldProgress, newProgress) ->
                callbacks.onProgress().accept(newProgress.doubleValue()));

        task.setOnSucceeded(event -> {
            callbacks.onStatus().accept(completedMessage(operation));
            callbacks.onFinished().run();
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();
            callbacks.onError().accept(error == null
                    ? "An unknown error occurred."
                    : error.getMessage() == null
                    ? error.getClass().getSimpleName()
                    : error.getMessage());
            callbacks.onFinished().run();
        });

        Thread thread = new Thread(task, operation == Operation.ENCRYPT
                ? "file-encryption-task"
                : "file-decryption-task");
        thread.setDaemon(true);
        thread.start();
    }

    private String startMessage(Operation operation) {
        return operation == Operation.ENCRYPT
                ? "Encrypting file..."
                : "Decrypting file...";
    }

    private String completedMessage(Operation operation) {
        return operation == Operation.ENCRYPT
                ? "Encryption completed successfully."
                : "Decryption completed successfully.";
    }

    private void runCipher(Path input, Path output, char[] password, SaveMode saveMode,
                           Operation operation, java.util.function.DoubleConsumer progress)
            throws IOException, GeneralSecurityException {
        if (operation == Operation.DECRYPT && !Files.isRegularFile(input)) {
            throw new IOException("To decrypt a directory, select its encrypted .enc file.");
        }

        if (operation == Operation.ENCRYPT && Files.isDirectory(input)) {
            Path archive = Files.createTempFile(output.getParent(), ".file-archive-", ".zip");
            try {
                DirectoryArchive.create(input, archive);
                new Encryptor(archive, output, PayloadType.DIRECTORY)
                        .encrypt(password, SaveMode.SEPARATE_FILE, progress);
                if (saveMode == SaveMode.REPLACE_ORIGINAL) {
                    deleteDirectory(input);
                }
            } finally {
                Files.deleteIfExists(archive);
            }
            return;
        }

        if (operation == Operation.DECRYPT
                && (saveMode == SaveMode.SEPARATE_FILE
                || input.getFileName().toString().endsWith(".enc"))) {
            Path temporaryDirectory = Files.createTempDirectory(output.getParent(), ".file-archive-");
            Path archive = temporaryDirectory.resolve("content.zip");
            Path restored = Files.createTempDirectory(output.getParent(), ".file-restored-");
            try {
                Decryptor decryptor = new Decryptor(input, archive);
                decryptor.decrypt(password, SaveMode.SEPARATE_FILE, progress);
                if (decryptor.payloadType() == PayloadType.DIRECTORY) {
                    DirectoryArchive.extract(archive, restored);
                    Path directoryOutput = saveMode == SaveMode.SEPARATE_FILE
                            ? output
                            : directoryOutput(input);
                    if (Files.exists(directoryOutput)) {
                        throw new IOException("Output directory already exists: " + directoryOutput);
                    }
                    moveDirectory(restored, directoryOutput);
                    if (saveMode == SaveMode.REPLACE_ORIGINAL) {
                        Files.delete(input);
                    }
                } else {
                    if (saveMode == SaveMode.SEPARATE_FILE) {
                        if (Files.exists(output)) {
                            throw new IOException("Output file already exists: " + output);
                        }
                        Files.move(archive, output);
                    } else {
                        Files.move(archive, input, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } finally {
                Files.deleteIfExists(archive);
                deleteDirectory(temporaryDirectory);
                deleteDirectory(restored);
            }

            return;
        }

        if (operation == Operation.ENCRYPT) {
            new Encryptor(input, output).encrypt(password, saveMode, progress);
        } else {
            new Decryptor(input, output).decrypt(password, saveMode, progress);
        }
    }

    private Path directoryOutput(Path encryptedFile) throws IOException {
        String name = encryptedFile.getFileName().toString();
        if (!name.endsWith(".enc")) {
            throw new IOException("Encrypted directory must have the .enc extension.");
        }
        String directoryName = name.substring(0, name.length() - ".enc".length());
        if (directoryName.isBlank()) {
            throw new IOException("Encrypted directory has an invalid name.");
        }
        return encryptedFile.resolveSibling(directoryName);
    }

    private void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
        } catch (java.io.UncheckedIOException exception) {
            throw exception.getCause();
        }
    }
}
