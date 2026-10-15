package com.keng.diary.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {

    String uploadFile(MultipartFile imageFile);

    String uploadImage(long noteId, MultipartFile imageFile);

    void deleteImage(String storageKey);

    Resource readImage(String storageKey);
}
