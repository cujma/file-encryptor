# File Encryptor

A small desktop application for encrypting and decrypting individual files. It is written in Java with JavaFX and works locally.

## Running the application

Java 21 is required. The Gradle Wrapper is included in the repository, so Gradle does not need to be installed separately.

Linux/macOS:

```bash
./gradlew run
```

Windows:

```bat
gradlew.bat run
```

On the first run, Gradle downloads JavaFX and the other dependencies. The application is operated through a graphical user interface.

<br>

## Usage

1. Choose `Encrypt` or `Decrypt`.
2. Select the input file.
3. Choose how the output should be saved.
4. Enter the password and start the operation.

When encrypting, the password must be entered twice for confirmation. The application displays the operation progress and keeps the interface responsive while the file is being processed.

There are two output modes:
- `Save as a new file` saves the result in the selected directory,
- `Replace original file` replaces the input file only after the operation finishes successfully.

<br>

## File names

When encrypting to a separate file, `.enc` is added to the file name:

```text
notes.txt -> notes.txt.enc
```

When decrypting, `.enc` is removed:

```text
notes.txt.enc -> notes.txt
```

If the input file does not have the `.enc` extension, the output receives the `.dec` extension. The output file name is generated automatically. The output directory defaults to the input file's directory, but it can be changed in the application.

<br>

## Encryption

File contents are encrypted using AES-256-GCM. The key is derived from the password with PBKDF2-HMAC-SHA256 using 600,000 iterations. New random cryptographic parameters are generated for every file.

Files are processed in 64 KiB chunks, so the entire file does not have to be loaded into memory at once. During decryption, the integrity of every chunk is verified. An incorrect password, a modified file, or data appended to the end of the file causes the operation to stop.

Encrypted files use a custom binary format created for this application. The format is not compatible with other encryption programs.

<br>

## Limitations

- the application handles individual files, not entire directories,
- there is no batch mode or command-line interface,
- the password is not stored and cannot be recovered,
- the strength of the protection depends partly on the quality of the password.

<br>

## Tests

Run the tests with:

```bash
./gradlew test
```

They cover successful encryption and decryption, empty and larger files, incorrect passwords, and replacing the original file.
