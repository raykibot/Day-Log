package com.keng.diary.service.impl;

import com.keng.diary.mapper.ImageMapper;
import com.keng.diary.mapper.INoteMapper;
import com.keng.diary.model.DTO.NoteDto;
import com.keng.diary.model.Note;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.VO.NoteVo;
import com.keng.diary.service.IFileService;
import com.keng.diary.service.INoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class NoteServiceImpl implements INoteService {
    private static final long MAX_IMAGE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_MD_SIZE = 1024 * 1024;

    private final INoteMapper noteMapper;
    private final ImageMapper imageMapper;
    private final IFileService fileService;
    private final TransactionTemplate transaction;

    public NoteServiceImpl(INoteMapper noteMapper, ImageMapper imageMapper,
                           IFileService fileService, TransactionTemplate transaction) {
        this.noteMapper = noteMapper;
        this.imageMapper = imageMapper;
        this.fileService = fileService;
        this.transaction = transaction;
    }

    @Override
    public Boolean createNote(NoteDto noteDto, List<MultipartFile> imageFiles) {
        validateNote(noteDto);
        List<MultipartFile> files = imageFiles == null ? List.of() : imageFiles;
        Set<String> names = new HashSet<>();
        for (MultipartFile file : files) {
            String name = file == null ? null : file.getOriginalFilename();
            if (name == null || name.isBlank() || name.length() > 255
                    || name.contains("/") || name.contains("\\")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片文件名无效");
            }
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片文件名不能重复");
            }
            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片不能为空");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "单张图片不能超过20MB");
            }
            if (!List.of("image/jpeg", "image/png", "image/webp").contains(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只支持JPEG、PNG、WebP图片");
            }
        }

        List<String> uploadedKeys = new ArrayList<>();
        try {
            return transaction.execute(status -> {
                Note note = new Note();
                note.setTitle(noteDto.getTitle());
                note.setMdName(noteDto.getMdName());
                note.setContent(noteDto.getContent()); // Markdown 原文直接保存，不改写图片路径
                note.setCategory(noteDto.getCategory());
                note.setCreateTime(new Date());
                if (noteMapper.insert(note) != 1 || note.getId() == null) {
                    throw new IllegalStateException("记录保存失败");
                }

                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    String key = fileService.uploadImage(note.getId(), file);
                    uploadedKeys.add(key);

                    NoteImage image = new NoteImage();
                    image.setNoteId(note.getId());
                    image.setOriginalName(file.getOriginalFilename());
                    image.setStorageKey(key);
                    image.setMimeType(file.getContentType());
                    image.setSizeBytes(file.getSize());
                    image.setSortOrder(i);
                    image.setCreateTime(new Date());
                    imageMapper.insert(image);
                }
                return true;
            });
        } catch (RuntimeException ex) {
            for (String key : uploadedKeys) {
                try {
                    fileService.deleteImage(key);
                } catch (RuntimeException cleanupError) {
                    log.error("OSS 图片清理失败：{}", key, cleanupError);
                }
            }
            throw ex;
        }
    }

    @Override
    public NoteImage findImage(long noteId, String name) {
        if (noteMapper.find(noteId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        }
        NoteImage image = imageMapper.findByName(noteId, name);
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
        }
        return image;
    }

    @Override
    public Resource readImage(NoteImage image) {
        return fileService.readImage(image.getStorageKey());
    }

    @Override
    public List<NoteVo> getNotesList(String category) {

        List<NoteVo> noteVoList = new ArrayList<>();

        List<Note> noteList = noteMapper.getNoteListByCategory(category);
        for (Note note : noteList) {
            noteVoList.add(toNoteVo(note));
        }
        return noteVoList;
    }

    @Override
    public NoteVo getNote(long id) {
        Note note = noteMapper.find(id);
        if (note == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        }
        return toNoteVo(note);
    }

    private NoteVo toNoteVo(Note note) {
        NoteVo vo = new NoteVo();
        vo.setId(note.getId());
        vo.setTitle(note.getTitle());
        vo.setMdName(note.getMdName());
        vo.setContent(note.getContent());
        vo.setCategory(note.getCategory());
        vo.setCreateTime(note.getCreateTime());
        vo.setUpdateTime(note.getUpdateTime());
        List<NoteImage> images = imageMapper.list(note.getId());
        if (images != null) {
            vo.setImages(images);
        }
        return vo;
    }

    private void validateNote(NoteDto note) {
        if (note == null || note.getMdName() == null || note.getMdName().isBlank()
                || note.getMdName().length() > 255 || !note.getMdName().toLowerCase(Locale.ROOT).endsWith(".md")
                || note.getMdName().contains("/") || note.getMdName().contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需要有效的.md文件名");
        }
        if (note.getTitle() != null && note.getTitle().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能超过200字");
        }
        if (note.getContent() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Markdown正文不能为空");
        }
        if (note.getContent().getBytes(StandardCharsets.UTF_8).length > MAX_MD_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Markdown正文不能超过1MB");
        }
        if (!"WORK".equals(note.getCategory()) && !"LIFE".equals(note.getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分类只能是WORK或LIFE");
        }
    }
}
