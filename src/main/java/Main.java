import cipher.*;
import java.nio.file.Path;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws GeneralSecurityException, IOException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey key = keyGenerator.generateKey();

        Path input = Path.of("test.txt");
        Path output = Path.of("test.enc");

        System.out.print(Path.of("test.txt").toAbsolutePath());

        Encryptor encryptor = new Encryptor(input, output);
    }
}