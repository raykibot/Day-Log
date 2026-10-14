package com.keng.diary.service.impl;

import com.keng.diary.mapper.ImageMapper;
import com.keng.diary.mapper.NoteMapper;
import com.keng.diary.model.Note;
import com.keng.diary.model.NoteImage;
import com.keng.diary.model.NotePage;
import com.keng.diary.service.IFileService;
import com.keng.diary.service.INoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class NoteServiceImpl implements INoteService {
    private static final long MAX_MD_BYTES = 1024 * 1024;
    private static final Pattern IMAGE_LINK = Pattern.compile("!\\[[^]]*]\\(([^)]+)\\)");
    private final NoteMapper notes;
    private final ImageMapper images;
    private final IFileService files;
    private final TransactionTemplate transaction;

    public NoteServiceImpl(NoteMapper notes, ImageMapper images, IFileService files, TransactionTemplate transaction) {
        this.notes = notes;
        this.images = images;
        this.files = files;
        this.transaction = transaction;
    }

    @Override
    public Note create(String title, String category, MultipartFile document, List<MultipartFile> files) {
        String cleanTitle = title(title);
        String cleanCategory = category(category);
        String mdName = mdName(document);
        String content = markdown(document);
        List<NoteImage> added = new ArrayList<>();
        try {
            return transaction.execute(status -> {
                Note note = new Note();
                note.setTitle(cleanTitle);
                note.setCategory(cleanCategory);
                note.setMdName(mdName);
                note.setContent(content);
                note.setCreateTime(now());
                notes.insert(note);
                addFiles(note.getId(), files, added);
                checkImages(content, added);
                for (int i = 0; i < added.size(); i++) {
                    NoteImage image = added.get(i);
                    image.setSortOrder(i);
                    images.insert(image);
                }
                return get(note.getId());
            });
        } catch (RuntimeException e) {
            cleanup(added);
            throw e;
        }
    }

    @Override
    public Note update(long id, String title, String category, MultipartFile document,
                       List<Long> keepImageIds, List<MultipartFile> files) {
        String cleanTitle = title(title);
        String cleanCategory = category(category);
        String newName = document == null ? null : mdName(document);
        String newContent = document == null ? null : markdown(document);
        List<NoteImage> added = new ArrayList<>();
        List<NoteImage> removed = new ArrayList<>();
        try {
            Note saved = transaction.execute(status -> {
                Note note = get(id);
                List<NoteImage> current = note.getImages();
                Set<Long> keep = new HashSet<>(keepImageIds == null ? List.of() : keepImageIds);
                Set<Long> available = new HashSet<>();
                current.forEach(image -> available.add(image.getId()));
                if (!available.containsAll(keep)) {
                    throw badRequest("包含不属于这条记录的图片ID");
                }
                List<NoteImage> finalImages = new ArrayList<>();
                for (NoteImage image : current) {
                    if (keep.contains(image.getId())) finalImages.add(image);
                    else removed.add(image);
                }
                addFiles(id, files, added);
                finalImages.addAll(added);
                String content = newContent == null ? note.getContent() : newContent;
                checkImages(content, finalImages);
                note.setTitle(cleanTitle);
                note.setCategory(cleanCategory);
                if (newName != null) note.setMdName(newName);
                note.setContent(content);
                note.setUpdateTime(now());
                if (notes.update(note) == 0) throw notFound();
                removed.forEach(image -> images.delete(image.getId()));
                int order = current.stream().mapToInt(NoteImage::getSortOrder).max().orElse(-1) + 1;
                for (NoteImage image : added) {
                    image.setSortOrder(order++);
                    images.insert(image);
                }
                return get(id);
            });
            cleanup(removed);
            return saved;
        } catch (RuntimeException e) {
            cleanup(added);
            throw e;
        }
    }

    @Override
    public NotePage list(int page, int size, String category) {
        if (page < 1 || size < 1 || size > 50) throw badRequest("page从1开始，size须在1到50之间");
        String filter = category == null ? null : category(category);
        List<Note> items = notes.list(filter, size + 1, (long) (page - 1) * size);
        boolean hasMore = items.size() > size;
        if (hasMore) items.remove(size);
        items.forEach(note -> note.setImages(images.list(note.getId())));
        return new NotePage(items, page, size, hasMore);
    }

    @Override
    public Note get(long id) {
        Note note = notes.find(id);
        if (note == null) throw notFound();
        note.setImages(images.list(id));
        return note;
    }

    @Override
    public void delete(long id) {
        if (notes.softDelete(id, now()) == 0) throw notFound();
    }

    @Override
    public NoteImage image(long noteId, String name) {
        files.fileName(name);
        if (notes.find(noteId) == null) throw notFound();
        NoteImage image = images.findByName(noteId, name);
        if (image == null) throw notFound();
        return image;
    }

    @Override
    public Resource readImage(NoteImage image) {
        return files.readImage(image);
    }

    private void addFiles(long noteId, List<MultipartFile> uploads, List<NoteImage> added) {
        if (uploads == null) return;
        for (MultipartFile file : uploads) added.add(files.uploadImage(noteId, file));
    }

    private void cleanup(List<NoteImage> list) {
        for (NoteImage image : list) {
            try {
                files.deleteImage(image);
            } catch (RuntimeException e) {
                log.warn("OSS图片清理失败: {}", image.getStorageKey(), e);
            }
        }
    }

    private void checkImages(String content, List<NoteImage> list) {
        Set<String> names = new HashSet<>();
        for (NoteImage image : list) {
            if (!names.add(image.getOriginalName().toLowerCase(Locale.ROOT))) {
                throw badRequest("同一条记录不能有重名图片");
            }
        }
        Matcher links = IMAGE_LINK.matcher(content);
        while (links.find()) {
            String name = links.group(1).trim();
            files.fileName(name);
            if (!names.contains(name.toLowerCase(Locale.ROOT))) {
                throw badRequest("Markdown引用了未上传的图片：" + name);
            }
        }
    }

    private String mdName(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_MD_BYTES) {
            throw badRequest("必须上传不超过1MB的Markdown文件");
        }
        String name = files.fileName(file.getOriginalFilename());
        if (!name.toLowerCase(Locale.ROOT).endsWith(".md")) throw badRequest("文档必须是.md文件");
        return name;
    }

    private String markdown(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw badRequest("Markdown文件必须使用UTF-8编码");
        } catch (IOException e) {
            throw badRequest("无法读取Markdown文件");
        }
    }

    private String title(String title) {
        if (title == null || title.isBlank()) return null;
        String value = title.trim();
        if (value.length() > 200) throw badRequest("标题不能超过200字");
        return value;
    }

    private String category(String category) {
        if (!"WORK".equals(category) && !"LIFE".equals(category)) {
            throw badRequest("分类只能是WORK或LIFE");
        }
        return category;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
    }
}
