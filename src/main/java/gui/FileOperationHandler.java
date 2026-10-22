
package gui;

import cipher.SaveMode;

import java.nio.file.Path;

public interface FileOperationHandler {

    void encrypt(Path input, Path output, char[] password, SaveMode saveMode, OperationCallbacks callbacks);

    void decrypt(Path input, Path output, char[] password, SaveMode saveMode, OperationCallbacks callbacks);
}
