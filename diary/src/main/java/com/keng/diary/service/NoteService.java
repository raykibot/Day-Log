package com.keng.diary.service;

import com.keng.diary.model.Note;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.NotePage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NoteService {
    Note create(String title, String category, MultipartFile document, List<MultipartFile> images);

    Note update(long id, String title, String category, MultipartFile document,
                List<Long> keepImageIds, List<MultipartFile> images);

    NotePage list(int page, int size, String category);

    Note get(long id);

    void delete(long id);

    NoteImage image(long noteId, String name);

    Resource readImage(NoteImage image);
}
