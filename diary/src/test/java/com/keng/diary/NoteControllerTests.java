package com.keng.diary;

import com.keng.diary.controller.NoteController;
import com.keng.diary.exception.GlobalException;
import com.keng.diary.model.DTO.NoteDto;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.VO.NoteVo;
import com.keng.diary.service.IFileService;
import com.keng.diary.service.INoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class NoteControllerTests {
    private INoteService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(INoteService.class);
        NoteController controller = new NoteController();
        ReflectionTestUtils.setField(controller, "noteService", service);
        ReflectionTestUtils.setField(controller, "fileService", mock(IFileService.class));
        mvc = standaloneSetup(controller).setControllerAdvice(new GlobalException()).build();
    }

    @Test
    void createsNoteFromJsonPartAndMultipleImageParts() throws Exception {
        when(service.createNote(any(), any())).thenReturn(true);
        MockMultipartFile dto = new MockMultipartFile("noteDto", "", "application/json",
                "{\"mdName\":\"today.md\",\"content\":\"# 今天\",\"category\":\"WORK\"}"
                        .getBytes(StandardCharsets.UTF_8));
        MockMultipartFile first = new MockMultipartFile("imageFile", "a.png", "image/png", new byte[]{1});
        MockMultipartFile second = new MockMultipartFile("imageFile", "b.png", "image/png", new byte[]{2});

        mvc.perform(multipart("/api/v1/notes/create").file(dto).file(first).file(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(service).createNote(argThat(note -> "today.md".equals(note.getMdName())),
                argThat(images -> images.size() == 2
                        && "a.png".equals(images.get(0).getOriginalFilename())
                        && "b.png".equals(images.get(1).getOriginalFilename())));
    }

    @Test
    void textPartCannotBindAsNoteDto() throws Exception {
        MockMultipartFile dto = new MockMultipartFile("noteDto", "", "text/plain",
                "{}".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/v1/notes/create").file(dto))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(service);
    }

    @Test
    void readsAssociatedImage() throws Exception {
        NoteImage image = new NoteImage();
        image.setMimeType("image/png");
        image.setStorageKey("notes/7/a.png");
        when(service.findImage(7L, "a.png")).thenReturn(image);
        when(service.readImage(image)).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        mvc.perform(get("/api/v1/notes/7/images/a.png"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Type", "image/png"));
    }

    @Test
    void listsNotesForSelectedCategory() throws Exception {
        NoteVo note = new NoteVo();
        note.setId(7L);
        note.setCategory("WORK");
        note.setMdName("today.md");
        NoteImage image = new NoteImage();
        image.setId(3L);
        image.setNoteId(7L);
        image.setOriginalName("a.png");
        image.setStorageKey("notes/7/internal.png");
        note.setImages(List.of(image));
        when(service.getNotesList("WORK")).thenReturn(List.of(note));

        mvc.perform(get("/api/v1/notes/noteList").param("category", "WORK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].category").value("WORK"))
                .andExpect(jsonPath("$.data[0].images[0].url")
                        .value("/api/v1/notes/7/images/a.png"))
                .andExpect(jsonPath("$.data[0].images[0].storageKey").doesNotExist());
        verify(service).getNotesList("WORK");
    }

    @Test
    void getsFullNoteForDetailPage() throws Exception {
        NoteVo note = new NoteVo();
        note.setId(7L);
        note.setContent("# 完整正文");
        when(service.getNote(7L)).thenReturn(note);

        mvc.perform(get("/api/v1/notes/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.content").value("# 完整正文"))
                .andExpect(jsonPath("$.data.images").isArray());
    }

    @Test
    void missingDetailReturns404() throws Exception {
        when(service.getNote(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));

        mvc.perform(get("/api/v1/notes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("记录不存在"));
    }
}
