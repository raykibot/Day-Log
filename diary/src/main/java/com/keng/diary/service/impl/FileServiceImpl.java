package com.keng.diary.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.keng.diary.config.OSSConfig;
import com.keng.diary.service.IFileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileServiceImpl implements IFileService {
    private final OSSConfig config;

    public FileServiceImpl(OSSConfig config) {
        this.config = config;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String key = uploadImage(0, file);
        String endpoint = config.getEndPoint().replaceFirst("^https?://", "");
        return "https://" + config.getBucketName() + "." + endpoint + "/" + key;
    }

    @Override
    public String uploadImage(long noteId, MultipartFile file) {
        String suffix;
        switch (String.valueOf(file.getContentType())) {
            case "image/jpeg": suffix = ".jpg"; break;
            case "image/png": suffix = ".png"; break;
            case "image/webp": suffix = ".webp"; break;
            default: suffix = ".bin";
        }
        String time = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        String key = "notes/" + noteId + "/" + time + "_" + UUID.randomUUID().toString().replace("-", "") + suffix;
        OSS client = client();
        try (InputStream input = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            client.putObject(config.getBucketName(), key, input, metadata);
            return key;
        } catch (IOException e) {
            try {
                client.deleteObject(config.getBucketName(), key);
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw new IllegalStateException("图片读取失败", e);
        } catch (RuntimeException e) {
            try {
                client.deleteObject(config.getBucketName(), key);
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void deleteImage(String storageKey) {
        OSS client = client();
        try {
            client.deleteObject(config.getBucketName(), storageKey);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public Resource readImage(String storageKey) {
        OSS client = client();
        try {
            OSSObject object = client.getObject(config.getBucketName(), storageKey);
            try (InputStream input = object.getObjectContent()) {
                return new ByteArrayResource(input.readAllBytes());
            }
        } catch (OSSException ex) {
            if ("NoSuchKey".equals(ex.getErrorCode())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在", ex);
            }
            throw ex;
        } catch (IOException ex) {
            throw new IllegalStateException("图片读取失败", ex);
        } finally {
            client.shutdown();
        }
    }

    private OSS client() {
        return new OSSClientBuilder().build(config.getEndPoint(), config.getAccessKeyId(), config.getAccessKeySecret());
    }

}


