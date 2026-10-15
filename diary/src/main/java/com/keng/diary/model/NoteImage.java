package com.keng.diary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.util.UriUtils;

import java.util.Date;

@Data
public class NoteImage {
    private Long id;
    private Long noteId;
    private String originalName;
    @JsonIgnore
    private String storageKey;
    private String mimeType;
    private long sizeBytes;
    private Integer sortOrder;
    private Date createTime;

    public String getUrl() {
        return "/api/v1/notes/" + noteId + "/images/" + UriUtils.encodePathSegment(originalName, "UTF-8");
    }
}
