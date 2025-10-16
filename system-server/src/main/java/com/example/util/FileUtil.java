package com.example.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUtil {

    public static Path ensureUploadsDir(String... parts) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir"));
        Path p = root;
        for (String s : parts) p = p.resolve(s);
        if (!Files.exists(p)) Files.createDirectories(p);
        return p;
    }

    public static String saveFile(MultipartFile file, String subdir) throws IOException {
        Path dir = ensureUploadsDir("uploads", subdir);
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : ".jpg";
        String filename = UUID.randomUUID().toString() + ext;
        Path dest = dir.resolve(filename).normalize();
        file.transferTo(dest.toFile());
        return filename;
    }

    public static boolean deleteFile(String filename, String subdir) {
        if (filename == null || filename.trim().isEmpty()) return false;
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "uploads", subdir);
            Path f = dir.resolve(filename).normalize();
            if (!f.startsWith(dir)) return false;
            return Files.deleteIfExists(f);
        } catch (IOException e) {
            return false;
        }
    }
}
