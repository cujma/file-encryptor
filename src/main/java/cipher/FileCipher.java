package cipher;

import java.util.Objects;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardOpenOption;

public abstract class FileCipher {
    protected final Path inputPath;
    protected final Path outputPath;

    public FileCipher(Path inputPath, Path outputPath) {
        this.inputPath = Objects.requireNonNull(inputPath, "inputPath").toAbsolutePath().normalize();
        this.outputPath = Objects.requireNonNull(outputPath, "outputPath").toAbsolutePath().normalize();
    }

    public FileCipher(Path inputPath) {
        this.inputPath = Objects.requireNonNull(inputPath, "inputPath").toAbsolutePath().normalize();
        this.outputPath = null;
    }

    protected byte[] readInput() throws IOException {
        if(!Files.exists(inputPath)){
            throw new IOException("Input file does not exist.");
        }

        if(!Files.isRegularFile(inputPath)){
            throw new IOException("Input path is not a regular file.");
        }

        return Files.readAllBytes(inputPath);
    }

    protected void writeOutput(byte[] data) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        Objects.requireNonNull(data, "data");

        if (inputPath.equals(outputPath)) {
            throw new IOException("Input and output paths must be different.");
        }

        Files.write(outputPath, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    protected void replaceOriginal(byte[] data) throws IOException{
        Path parent;
        Path tempPath;

        Objects.requireNonNull(data, "data");

        if(Files.isSymbolicLink(inputPath)){
            throw new IOException("Symbolic links are not supported.");
        }

        if(!Files.isRegularFile(inputPath)){
            throw new IOException("Input path is not a regular file.");
        }

        parent = inputPath.getParent();

        if (parent == null) {
            throw new IOException("Input file has no parent directory.");
        }

        tempPath = Files.createTempFile(parent, "cipher-", ".tmp");

        try {
            Files.write(tempPath, data, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            try{
                Files.move(tempPath, inputPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch(AtomicMoveNotSupportedException e){
                throw new IOException("Atomic replacement is not supported by this filesystem.", e);
            }
        }finally {
            Files.deleteIfExists(tempPath);
        }
    }

    protected void saveOutput(byte[] data, SaveMode saveMode) throws IOException {
        Objects.requireNonNull(saveMode, "saveMode");

        if(saveMode == SaveMode.SEPARATE_FILE && outputPath == null){
            throw new IllegalStateException("Output path must be specified when using SEPARATE_FILE save mode.");
        }

        if(saveMode == SaveMode.REPLACE_ORIGINAL && outputPath != null){
            throw new IllegalStateException("Output path must be null when using REPLACE_ORIGINAL save mode.");
        }

        switch(saveMode){
            case SEPARATE_FILE -> writeOutput(data);
            case REPLACE_ORIGINAL -> replaceOriginal(data);
        }
    }

}
