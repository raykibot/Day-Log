package com.keng.diary.controller;

import com.keng.diary.model.Note;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.NotePage;
import com.keng.diary.model.ResponseEntity;
import com.keng.diary.service.NoteService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {
    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Note> create(@RequestParam(required = false) String title,
                                       @RequestParam String category,
                                       @RequestParam MultipartFile document,
                                       @RequestParam(required = false) List<MultipartFile> images,
                                       HttpServletResponse response) {
        Note note = service.create(title, category, document, images);
        response.setHeader("Location", "/api/v1/notes/" + note.getId());
        return ResponseEntity.created(note);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Note> update(@PathVariable long id, @RequestParam(required = false) String title,
                                       @RequestParam String category,
                                       @RequestParam(required = false) MultipartFile document,
                                       @RequestParam(required = false) List<Long> keepImageIds,
                                       @RequestParam(required = false) List<MultipartFile> images) {
        return ResponseEntity.success(service.update(id, title, category, document, keepImageIds, images));
    }

    @GetMapping
    public ResponseEntity<NotePage> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String category) {
        return ResponseEntity.success(service.list(page, size, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> get(@PathVariable long id) {
        return ResponseEntity.success(service.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.success(null);
    }

    @GetMapping("/{id}/images/{filename:.+}")
    public org.springframework.http.ResponseEntity<Resource> image(@PathVariable long id, @PathVariable String filename) {
        NoteImage image = service.image(id, filename);
        return org.springframework.http.ResponseEntity.ok().contentType(MediaType.parseMediaType(image.getMimeType()))
                .body(service.readImage(image));
    }

}
