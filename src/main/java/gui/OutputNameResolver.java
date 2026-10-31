package gui;

import java.nio.file.Path;

final class OutputNameResolver {
    private OutputNameResolver() {
    }

    static String forEncryption(String inputName) {
        return inputName.endsWith(".enc") ? inputName : inputName + ".enc";
    }

    static String forDecryption(String inputName) {
        if (!inputName.endsWith(".enc")) {
            return inputName + ".dec";
        }

        String result = inputName.substring(0, inputName.length() - ".enc".length());
        return result.isBlank() ? "decrypted-file" : result;
    }

    static Path nextRestoredDirectory(Path directory) {
        Path parent = directory.getParent();
        String baseName = directory.getFileName() + "-restored";
        Path candidate = parent.resolve(baseName);
        int suffix = 2;

        while (java.nio.file.Files.exists(candidate)) {
            candidate = parent.resolve(baseName + "-" + suffix++);
        }
        return candidate;
    }
}
