package com.medicare.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FileStorageUtil {

    private static final Path BASE_UPLOAD_DIR = Path.of("uploads");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private FileStorageUtil() {
    }

    public static String copyToUploads(Path sourceFile, String folderName) throws IOException {
        if (sourceFile == null) {
            return null;
        }

        Path targetDir = BASE_UPLOAD_DIR.resolve(folderName);
        Files.createDirectories(targetDir);

        String originalName = sourceFile.getFileName().toString();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String safeName = "file_" + FORMATTER.format(LocalDateTime.now()) + extension.toLowerCase();
        Path targetFile = targetDir.resolve(safeName);
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        return targetFile.toAbsolutePath().toString();
    }
}
