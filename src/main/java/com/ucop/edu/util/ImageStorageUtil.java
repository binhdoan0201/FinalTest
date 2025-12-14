package com.ucop.edu.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ImageStorageUtil {

    private static final String BASE_DIR = "FinalTest/src/main/resourses/images";

    public static String saveCourseImage(File sourceFile) {
        if (sourceFile == null) return null;

        try {
            File dir = new File(BASE_DIR);
            if (!dir.exists()) dir.mkdirs();

            String ext = getExt(sourceFile.getName()); // ".jpg"
            String newName = "course_" + System.currentTimeMillis() + ext;

            File dest = new File(dir, newName);

            Files.copy(sourceFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Lưu path dạng tương đối (portable hơn)
            return dest.getPath().replace("\\", "/");
        } catch (Exception e) {
            throw new RuntimeException("Không lưu ảnh được: " + e.getMessage(), e);
        }
    }

    public static String toImageUri(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;

        // nếu user nhập URL online
        if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) return storedPath;

        // nếu đã là file:/...
        if (storedPath.startsWith("file:/")) return storedPath;

        // path local -> uri
        File f = new File(storedPath);
        return f.toURI().toString();
    }

    private static String getExt(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i) : "";
    }
}