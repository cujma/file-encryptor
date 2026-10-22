package gui;

import cipher.Decryptor;
import cipher.Encryptor;
import cipher.SaveMode;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Path;
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

                    runCipher(input, output, passwordChars, saveMode, operation);
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
                           Operation operation) throws IOException, GeneralSecurityException {
        if (operation == Operation.ENCRYPT) {
            new Encryptor(input, output).encrypt(password, saveMode);
        } else {
            new Decryptor(input, output).decrypt(password, saveMode);
        }
    }
}
