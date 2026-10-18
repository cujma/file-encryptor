package cipher;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public abstract class FileCipher{

    protected final Path inputPath;
    protected final Path outputPath;

    protected FileCipher(Path inputPath, Path outputPath) {
        if (inputPath == null || outputPath == null) {
            throw new IllegalArgumentException("Input and output paths cannot be null.");
        }

        this.inputPath = inputPath.toAbsolutePath().normalize();
        this.outputPath = outputPath.toAbsolutePath().normalize();

        if (this.inputPath.equals(this.outputPath)) {
            throw new IllegalArgumentException("Input and output paths must be different.");
        }
    }

    protected FileCipher(Path inputPath) {
        if (inputPath == null) {
            throw new IllegalArgumentException("Input path cannot be null.");
        }

        this.inputPath = inputPath.toAbsolutePath().normalize();
        this.outputPath = null;
    }

    protected byte[] readInput() throws IOException {
        if (!Files.exists(inputPath)) {
            throw new IOException("Input file does not exist.");
        }

        if (Files.isSymbolicLink(inputPath)) {
            throw new IOException("Symbolic links are not supported.");
        }

        if (!Files.isRegularFile(inputPath)) {
            throw new IOException("Input path is not a regular file.");
        }

        if (!Files.isReadable(inputPath)) {
            throw new IOException("Input file is not readable.");
        }

        return Files.readAllBytes(inputPath);
    }

    protected void writeOutput(byte[] data) throws IOException {
        if (outputPath == null){
            throw new IllegalStateException("Output path has not been configured.");
        }

        Path parent = outputPath.getParent();

        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Output directory does not exist.");
        }

        Files.write(outputPath, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    protected void replaceOriginal(byte[] data) throws IOException {
        if (outputPath == null) {
            throw new IllegalStateException("A destination path is required to replace the original.");
        }

        Path parent = inputPath.getParent();

        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Input directory does not exist.");
        }

        if (!Files.isDirectory(outputPath.getParent())) {
            throw new IOException("Output directory does not exist.");
        }

        if (Files.exists(outputPath)) {
            throw new IOException("Destination file already exists: " + outputPath);
        }

        Path tempFile = Files.createTempFile(parent, ".file-cipher-", ".tmp");

        boolean movedToDestination = false;

        try {
            Files.write(tempFile, data, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.move(tempFile, outputPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException("Atomic file move is not supported. " + "The original file was kept.", e);
            }

            movedToDestination = true;

            try{
                Files.delete(inputPath);
            } catch (IOException e) {
                throw new IOException("The result was saved to " + outputPath + ", but the old file could not be deleted: " + inputPath, e);
            }

        }finally {
            if(!movedToDestination || Files.exists(tempFile)){
                Files.deleteIfExists(tempFile);
            }
        }
    }

    protected void saveOutput(byte[] data, SaveMode saveMode) throws IOException {

        if(data == null) {
            throw new IllegalArgumentException("Output data cannot be null.");
        }

        if(saveMode == null){
            throw new IllegalArgumentException("Save mode cannot be null.");
        }

        switch (saveMode) {
            case SEPARATE_FILE -> writeOutput(data);
            case REPLACE_ORIGINAL -> replaceOriginal(data);
        }
    }
}
