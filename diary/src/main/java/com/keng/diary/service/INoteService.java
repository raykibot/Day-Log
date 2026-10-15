package com.keng.diary.service;

import com.keng.diary.model.DTO.NoteDto;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.VO.NoteVo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface INoteService {


    Boolean createNote(NoteDto noteDto, List<MultipartFile> imageFile);

    NoteImage findImage(long noteId, String name);

    Resource readImage(NoteImage image);

    List<NoteVo> getNotesList(String category);

    NoteVo getNote(long id);

}
