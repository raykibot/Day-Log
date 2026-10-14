package com.keng.diary.service;

import com.keng.diary.model.NoteImage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    NoteImage uploadImage(long noteId, MultipartFile file);

    Resource readImage(NoteImage image);

    void deleteImage(NoteImage image);

    String fileName(String name);

    String uploadFile(MultipartFile imageFile);
}
