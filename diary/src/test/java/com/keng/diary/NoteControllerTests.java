package com.keng.diary;

import com.keng.diary.controller.ApiError;
import com.keng.diary.controller.NoteController;
import com.keng.diary.model.Note;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.NotePage;
import com.keng.diary.service.INoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class NoteControllerTests {
    private INoteService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(INoteService.class);
        mvc = standaloneSetup(new NoteController(service)).setControllerAdvice(new ApiError()).build();
    }

    @Test
    void listUsesCommonJson() throws Exception {
        when(service.list(1, 10, null)).thenReturn(new NotePage(List.of(), 1, 10, false));

        mvc.perform(get("/api/v1/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void createKeepsCreatedStatusAndLocation() throws Exception {
        Note note = new Note();
        note.setId(7L);
        note.setMdName("entry.md");
        when(service.create(any(), any(), any(), any())).thenReturn(note);
        MockMultipartFile document = new MockMultipartFile("document", "entry.md", "text/markdown", "# hello".getBytes());

        mvc.perform(multipart("/api/v1/notes").file(document).param("category", "WORK"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/notes/7"))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void deleteReturnsJsonBody() throws Exception {
        mvc.perform(delete("/api/v1/notes/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void errorsUseCommonJsonAndHttpStatus() throws Exception {
        when(service.get(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));

        mvc.perform(get("/api/v1/notes/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("记录不存在"));
    }

    @Test
    void imageRouteReturnsOssImageBytes() throws Exception {
        NoteImage image = new NoteImage();
        image.setMimeType("image/png");
        when(service.image(7L, "photo.png")).thenReturn(image);
        when(service.readImage(image)).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        mvc.perform(get("/api/v1/notes/7/images/photo.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }
}
