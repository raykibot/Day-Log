package com.keng.diary;

import com.keng.diary.service.ImageStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStoreTests {
    @TempDir
    Path directory;

    @Test
    void savesAndReadsPng() {
        ImageStore store = new ImageStore(directory.toString());
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        var image = store.save(new MockMultipartFile("images", "photo.png", "image/png", png));

        assertEquals("image/png", image.getMimeType());
        assertTrue(store.read(image).exists());
        store.delete(image);
        assertThrows(ResponseStatusException.class, () -> store.read(image));
    }

    @Test
    void rejectsOtherFilesAndPaths() {
        ImageStore store = new ImageStore(directory.toString());
        assertThrows(ResponseStatusException.class,
                () -> store.save(new MockMultipartFile("images", "fake.png", "image/png", "text".getBytes())));
        assertThrows(ResponseStatusException.class, () -> ImageStore.fileName("../photo.png"));
    }
}
