package cipher;

import java.util.Objects;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardOpenOption;
import java.nio.file.LinkOption;


public abstract class FileCipher {
    protected final Path inputPath;
    protected final Path outputPath;

    public FileCipher(Path inputPath, Path outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public FileCipher(Path inputPath) {
        this.inputPath = inputPath;
        this.outputPath = null;
    }

    protected byte[] readInput() throws IOException {
        return Files.readAllBytes(inputPath);
    }

    protected void writeOutput(byte[] data) throws IOException {
        Files.write(outputPath, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    protected void replaceOriginal(byte[] data) throws IOException{
        Path parent = inputPath.getParent();
        Path tempPath;

        if(Files.isSymbolicLink(inputPath)){
            throw new IOException("Symbolic links are not supported.");
        }

        if(!Files.isRegularFile(inputPath)){
            throw new IOException("Input path is not a regular file.");
        }

        if (parent == null) {
            throw new IOException("Input file has no parent directory.");
        }

        tempPath = Files.createTempFile(parent, "cipher-", ".tmp");

        try {
            Files.write(tempPath, data);

            try{
                Files.move(tempPath, inputPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch(AtomicMoveNotSupportedException e){
                throw new IOException("Atomic replacement is not supported by this filesystem." + e);
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
