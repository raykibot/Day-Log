package com.keng.diary.controller;

import com.keng.diary.model.DTO.NoteDto;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.Result;
import com.keng.diary.model.VO.NoteVo;
import com.keng.diary.service.IFileService;
import com.keng.diary.service.INoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@Slf4j
public class NoteController {

    @Autowired
    private IFileService fileService;

    @Autowired
    private INoteService noteService;

    /**
     * 图片上传
     *
     * @param imageFile 图片文件
     * @return 阿里云图片下载路径
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("fileName") MultipartFile imageFile) {

        log.info("图片上传");

        String imageURL = fileService.uploadFile(imageFile);
        return Result.success(imageURL);
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Boolean> createNote(@RequestPart("noteDto") NoteDto noteDto,
                                      @RequestPart(value = "imageFile", required = false) List<MultipartFile> imageFiles) {

        log.info("创建记录");

        Boolean created = noteService.createNote(noteDto, imageFiles == null ? List.of() : imageFiles);
        if (!Boolean.TRUE.equals(created)) {
            return Result.error();
        }
        log.info("创建成功");
        return Result.created(true);
    }


    @GetMapping("/noteList")
    public Result<List<NoteVo>> getNotes(@RequestParam(value = "category", required = false) String category) {

        log.info("获取记录--分类类型：{}", category);

        List<NoteVo> notesList = noteService.getNotesList(category);
        return Result.success(notesList);
    }

    @GetMapping("/{id}")
    public Result<NoteVo> getNote(@PathVariable long id) {
        log.info("获取记录详情--ID：{}", id);
        NoteVo note = noteService.getNote(id);
        return Result.success(note);
    }



    @GetMapping("/{id}/images/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable long id, @PathVariable String filename) {
        NoteImage image = noteService.findImage(id, filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .body(noteService.readImage(image));
    }
}
