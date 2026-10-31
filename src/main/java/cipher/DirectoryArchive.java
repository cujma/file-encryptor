package cipher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class DirectoryArchive {
    private DirectoryArchive() {
    }

    public static void create(Path directory, Path archive) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) {
            throw new IOException("Input directory is not accessible.");
        }

        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.isSymbolicLink(file) || !attrs.isRegularFile()) {
                        throw new IOException("Symbolic links and special files are not supported.");
                    }
                    addFile(root, file, zip);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                    if (exception != null) {
                        throw exception;
                    }
                    boolean empty;
                    try (var children = Files.list(dir)) {
                        empty = children.findAny().isEmpty();
                    }
                    if (!dir.equals(root) && empty) {
                        String name = root.relativize(dir).toString().replace('\\', '/') + "/";
                        zip.putNextEntry(new ZipEntry(name));
                        zip.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public static void extract(Path archive, Path destination) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        Files.createDirectories(root);
        Set<Path> extracted = new HashSet<>();

        try (InputStream input = Files.newInputStream(archive);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = safeTarget(root, entry.getName());
                if (!extracted.add(target)) {
                    throw new IOException("Archive contains duplicate entries.");
                }

                if (entry.isDirectory()) {
                    if (Files.exists(target) && !Files.isDirectory(target)) {
                        throw new IOException("Archive contains conflicting entries.");
                    }
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent == null || Files.exists(target)) {
                        throw new IOException("Archive contains conflicting entries.");
                    }
                    Files.createDirectories(parent);
                    try (OutputStream output = Files.newOutputStream(target)) {
                        zip.transferTo(output);
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private static Path safeTarget(Path root, String entryName) throws IOException {
        if (entryName == null || entryName.isBlank() || entryName.indexOf('\0') >= 0) {
            throw new IOException("Archive contains an invalid path.");
        }

        Path relative = Path.of(entryName);
        if (relative.isAbsolute()) {
            throw new IOException("Archive contains an unsafe path.");
        }

        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)
                || Files.isSymbolicLink(target)
                || hasSymbolicLinkParent(root, target)) {
            throw new IOException("Archive contains an unsafe path.");
        }
        return target;
    }

    private static boolean hasSymbolicLinkParent(Path root, Path target) {
        Path current = root;
        Path relative = root.relativize(target);
        for (Path part : relative) {
            current = current.resolve(part);
            if (current.equals(target)) {
                break;
            }
            if (Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }

    private static void addFile(Path root, Path file, ZipOutputStream zip) throws IOException {
        String name = root.relativize(file).toString().replace('\\', '/');
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream input = Files.newInputStream(file)) {
            input.transferTo(zip);
        } finally {
            zip.closeEntry();
        }
    }
}
