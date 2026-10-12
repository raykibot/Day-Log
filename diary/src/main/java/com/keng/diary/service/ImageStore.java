package com.keng.diary.service;

import com.keng.diary.model.NoteImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ImageStore {
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private final Path directory;

    public ImageStore(@Value("${diary.upload-dir}") String uploadDir) {
        directory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public NoteImage save(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片不能为空且不能超过20MB");
        }
        String name = fileName(file.getOriginalFilename());
        String mime;
        try {
            mime = imageType(file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取图片", e);
        }
        String key = UUID.randomUUID().toString();
        Path path = directory.resolve(key);
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, path);
            }
        } catch (IOException e) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "保存图片失败", e);
        }
        NoteImage image = new NoteImage();
        image.setOriginalName(name);
        image.setStorageKey(key);
        image.setMimeType(mime);
        image.setSizeBytes(file.getSize());
        image.setCreateTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        return image;
    }

    public Resource read(NoteImage image) {
        Path path = directory.resolve(image.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片文件不存在");
        }
        return new FileSystemResource(path);
    }

    public void delete(NoteImage image) {
        try {
            Files.deleteIfExists(directory.resolve(image.getStorageKey()));
        } catch (IOException ignored) {
            // 数据库已提交时，保留孤立文件比误删其他文件安全。
        }
    }

    public static String fileName(String name) {
        if (name == null || name.isBlank() || name.length() > 255 || name.equals(".") || name.equals("..")
                || name.chars().anyMatch(c -> c < 32 || "\\/<>:\"|?*".indexOf(c) >= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件名无效");
        }
        return name;
    }

    private String imageType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) {
            return "image/jpeg";
        }
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78
                && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
            return "image/png";
        }
        if (bytes.length >= 12 && bytes[0] == 82 && bytes[1] == 73 && bytes[2] == 70 && bytes[3] == 70
                && bytes[8] == 87 && bytes[9] == 69 && bytes[10] == 66 && bytes[11] == 80) {
            return "image/webp";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只支持JPEG、PNG和WebP图片");
    }
}
