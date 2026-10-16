package cipher;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public abstract class FileCipher {
    protected final Path inputPath;
    protected final Path outputPath;

    public FileCipher(Path inputPath, Path outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    protected byte[] readInput() throws IOException {
        return Files.readAllBytes(inputPath);
    }

    protected void writeOutput(byte[] data) throws IOException {
        Files.write(outputPath, data);
    }
}
