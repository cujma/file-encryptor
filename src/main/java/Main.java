
import cipher.Decryptor;
import cipher.Encryptor;
import cipher.SaveMode;

import javax.crypto.AEADBadTagException;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        while (true){
            clearConsole();

            System.out.println("=== File Encryptor ===");
            System.out.println("1. Encrypt file");
            System.out.println("2. Decrypt file");
            System.out.println("3. Exit");

            String choice = readLine("Choose operation:").trim();

            if (choice.equals("3")){
                break;
            }

            try {
                if (!choice.equals("1") && !choice.equals("2")) {
                    throw new IllegalArgumentException("Choose 1, 2 or 3.");
                }

                runOperation(choice.equals("1"));

                System.out.println();
                System.out.println("Operation finished.");
                Thread.sleep(2000);

            } catch (AEADBadTagException e) {
                showError("Wrong password or corrupted file.");

            } catch (IOException e) {
                showError("File operation failed: " + e.getMessage());

            } catch (GeneralSecurityException e) {
                showError("Cryptographic operation failed.");

            } catch (IllegalArgumentException | IllegalStateException e) {
                showError("Invalid input: " + e.getMessage());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        SCANNER.close();
    }

    private static void runOperation(boolean encrypting) throws IOException, GeneralSecurityException {
        System.out.println();
        Path inputPath = readPath("Input file path:");

        validateInputFile(inputPath);

        if (!encrypting && !inputPath.getFileName().toString().endsWith(".enc")) {
            throw new IllegalArgumentException("For decryption, the filename must end with .enc.");
        }

        Path outputPath = createOutputPath(inputPath, encrypting);

        System.out.println("Input file: " + inputPath);
        System.out.println("Output file: " + outputPath);

        System.out.println();
        System.out.println("Choose save mode");
        System.out.println("1. Save to a new file");
        System.out.println("2. Replace the original file");

        String modeChoice = readLine("Your choice:").trim();

        SaveMode saveMode;

        if (modeChoice.equals("1")) {
            saveMode = SaveMode.SEPARATE_FILE;

        } else if (modeChoice.equals("2")) {
            saveMode = SaveMode.REPLACE_ORIGINAL;

            System.out.println();
            System.out.println("Warning: the original file will be replaced.");

            String confirmation = readLine("Type YES to continue: ");

            if (!confirmation.equals("YES")) {
                System.out.println();
                System.out.println("Operation cancelled.");
                return;
            }

        }else{
            throw new IllegalArgumentException("Choose save mode 1 or 2.");
        }

        validateOutputPath(inputPath, outputPath, saveMode);

        System.out.println();
        char[] password = readPassword();

        try{
            if (encrypting) {
                Encryptor encryptor = new Encryptor(inputPath, outputPath);
                encryptor.encrypt(password, saveMode);
            }else{
                Decryptor decryptor = new Decryptor(inputPath, outputPath);
                decryptor.decrypt(password, saveMode);
            }

            System.out.println();
            System.out.println("Operation completed successfully.");
            System.out.println("Result: " + outputPath);

        }finally{
            Arrays.fill(password, '\0');
        }
    }

    private static void validateInputFile(Path inputPath) throws IOException {
        if(!Files.exists(inputPath)){
            throw new IOException("Input file does not exist.");
        }

        if(Files.isSymbolicLink(inputPath)){
            throw new IOException("Symbolic links are not supported.");
        }

        if(!Files.isRegularFile(inputPath)){
            throw new IOException("Input path is not a regular file.");
        }

        if(!Files.isReadable(inputPath)){
            throw new IOException("Input file is not readable.");
        }
    }

    private static void validateOutputPath(Path inputPath, Path outputPath, SaveMode saveMode) throws IOException {
        if(inputPath.equals(outputPath)) {
            throw new IOException("Input and output paths must be different.");
        }

        Path parent = outputPath.getParent();

        if(parent == null || !Files.isDirectory(parent)){
            throw new IOException("Output directory does not exist.");
        }

        if(Files.exists(outputPath)) {
            throw new IOException("Output file already exists: " + outputPath);
        }

        if(saveMode == SaveMode.REPLACE_ORIGINAL && !Files.isWritable(inputPath.getParent())) {
            throw new IOException("Input directory is not writable.");
        }
    }

    private static Path createOutputPath(Path inputPath, boolean encrypting){
        String fileName = inputPath.getFileName().toString();

        if(encrypting) {
            return inputPath.resolveSibling(fileName + ".enc");
        }

        if(!fileName.endsWith(".enc")) {
            throw new IllegalArgumentException("Encrypted input file must end with .enc.");
        }

        String outputName = fileName.substring(0, fileName.length() - ".enc".length());

        if(outputName.isEmpty()) {
            throw new IllegalArgumentException("Invalid encrypted filename.");
        }
        return inputPath.resolveSibling(outputName);
    }

    private static Path readPath(String prompt) {
        String value = readLine(prompt).trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty.");
        }

        try{
            return Path.of(value).toAbsolutePath().normalize();

        }catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid file path.", e);
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine();
    }

    private static char[] readPassword() {
        Console console = System.console();

        if(console != null){
            char[] password = console.readPassword("Password:");

            if(password == null){
                throw new IllegalStateException("Could not read password.");
            }
            return password;
        }

        System.out.println("Warning: password may be visible while typing.");

        return readLine("Password: ").toCharArray();
    }

    private static void showError(String message) {
        System.out.println();
        System.out.println("Error: " + message);

        try{
            Thread.sleep(2000);
        }catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
