package com.keng.diary.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import com.keng.diary.config.OSSConfig;
import com.keng.diary.model.NoteImage;
import com.keng.diary.service.IFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements IFileService {
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter KEY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    private final OSSConfig config;

    public FileServiceImpl(OSSConfig config) {
        this.config = config;
    }

    @Override
    public NoteImage uploadImage(long noteId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片不能为空且不能超过20MB");
        }
        String name = fileName(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取图片", e);
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片不能超过20MB");
        }
        String mime = imageType(bytes);
        String extension = mime.equals("image/jpeg") ? ".jpg" : mime.equals("image/png") ? ".png" : ".webp";
        LocalDateTime time = LocalDateTime.now(BEIJING);
        String key = "notes/" + noteId + "/" + KEY_TIME.format(time) + "_"
                + UUID.randomUUID().toString().replace("-", "") + extension;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(mime);
        metadata.setContentLength(bytes.length);
        OSS client = client();
        try {
            client.putObject(config.getBucketName(), key, new ByteArrayInputStream(bytes), metadata);
        } catch (RuntimeException e) {
            try { client.deleteObject(config.getBucketName(), key); } catch (RuntimeException ignored) { }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "上传图片到OSS失败", e);
        } finally {
            client.shutdown();
        }
        NoteImage image = new NoteImage();
        image.setNoteId(noteId);
        image.setOriginalName(name);
        image.setStorageKey(key);
        image.setMimeType(mime);
        image.setSizeBytes(bytes.length);
        image.setCreateTime(time);
        return image;
    }

    @Override
    public Resource readImage(NoteImage image) {
        OSS client = client();
        try {
            OSSObject object = client.getObject(config.getBucketName(), image.getStorageKey());
            try (InputStream input = object.getObjectContent()) {
                return new ByteArrayResource(input.readAllBytes());
            }
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片文件不存在", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "读取OSS图片失败", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "读取OSS图片失败", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "读取OSS图片失败", e);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void deleteImage(NoteImage image) {
        OSS client = client();
        try {
            client.deleteObject(config.getBucketName(), image.getStorageKey());
        } finally {
            client.shutdown();
        }
    }

    @Override
    public String fileName(String name) {
        if (name == null || name.isBlank() || name.length() > 255 || name.equals(".") || name.equals("..")
                || name.chars().anyMatch(c -> c < 32 || "\\/<>:\"|?*".indexOf(c) >= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件名无效");
        }
        return name;
    }

    @Override
    public String uploadFile(MultipartFile file) {

        String endPoint = config.getEndPoint();
        String accessKeyId = config.getAccessKeyId();
        String accessKeySecret = config.getAccessKeySecret();
        String bucketName = config.getBucketName();

        //创建OSS对象
        OSS ossClient = new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);

        //获取原生文件名
        String originalFilename = file.getOriginalFilename();
        log.info("原始文件名：{}", originalFilename);
        //JDK8的日期格式
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter dft = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        //拼装OSS上存储的路径
        String folder = dft.format(time);
        //String fileName = generateUUID();
        // 获取不带后缀的文件名
//        String fileName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
//        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //在OSS上bucket下的文件名
        String uploadFileName = "user/" + folder + "/" + originalFilename;
        try {
            PutObjectResult result = ossClient.putObject(bucketName, uploadFileName, file.getInputStream());
            //拼装返回路径
            if (result != null) {
                return "https://"+bucketName+"."+endPoint+"/"+uploadFileName;
            }
        } catch (IOException e) {
            log.error("文件上传失败:{}",e.getMessage());
        } finally {
            //OSS关闭服务，不然会造成OOM
            ossClient.shutdown();
        }
        return null;
    }

    private OSS client() {
        return new OSSClientBuilder().build(config.getEndPoint(), config.getAccessKeyId(), config.getAccessKeySecret());
    }

    private String imageType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78
                && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 82 && bytes[1] == 73 && bytes[2] == 70 && bytes[3] == 70
                && bytes[8] == 87 && bytes[9] == 69 && bytes[10] == 66 && bytes[11] == 80) return "image/webp";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只支持JPEG、PNG和WebP图片");
    }


    /**
     * 获取随机字符串
     * @return
     */
    private String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
    }

}


