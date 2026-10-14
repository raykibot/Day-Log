package com.keng.diary.controller;

import com.keng.diary.model.Note;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.NotePage;
import com.keng.diary.model.Result;
import com.keng.diary.service.IFileService;
import com.keng.diary.service.INoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@Slf4j
public class NoteController {

    @Autowired
    private IFileService fileService;

    private final INoteService service;

    public NoteController(INoteService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Note> create(@RequestParam(required = false) String title,
                                       @RequestParam String category,
                                       @RequestParam MultipartFile document,
                                       @RequestParam(required = false) List<MultipartFile> images,
                                       HttpServletResponse response) {


        log.info("新建文件");
        Note note = service.create(title, category, document, images);
        response.setHeader("Location", "/api/v1/notes/" + note.getId());
        return Result.created(note);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Note> update(@PathVariable long id, @RequestParam(required = false) String title,
                                       @RequestParam String category,
                                       @RequestParam(required = false) MultipartFile document,
                                       @RequestParam(required = false) List<Long> keepImageIds,
                                       @RequestParam(required = false) List<MultipartFile> images) {

        log.info("更新记录");
        return Result.success(service.update(id, title, category, document, keepImageIds, images));
    }

    @GetMapping
    public Result<NotePage> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String category) {
        log.info("查询所有记录列表");
        return Result.success(service.list(page, size, category));
    }

    @GetMapping("/{id}")
    public Result<Note> get(@PathVariable long id) {
        log.info("分类查询");
        return Result.success(service.get(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable long id) {
        log.info("删除文件");
        service.delete(id);
        return Result.success(null);
    }

    @GetMapping("/{id}/images/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable long id, @PathVariable String filename) {
        NoteImage image = service.image(id, filename);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.getMimeType()))
                .body(service.readImage(image));
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("fileName") MultipartFile imageFile){
        log.info("图片上传");
        String imageURL = fileService.uploadFile(imageFile);
        return Result.success(imageURL);
    }

}
