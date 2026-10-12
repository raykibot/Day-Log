package com.keng.diary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Note {
    private Long id;
    private String title;
    private String mdName;
    private String content;
    private String category;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @JsonIgnore
    private LocalDateTime deleteTime;
    private List<NoteImage> images = new ArrayList<>();

    public String getDisplayTitle() {
        return title == null || title.isBlank() ? mdName.replaceFirst("(?i)\\.md$", "") : title;
    }
}
