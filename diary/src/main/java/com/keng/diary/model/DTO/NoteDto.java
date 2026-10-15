package com.keng.diary.model.DTO;

import lombok.Data;

@Data
public class NoteDto {

    private Long id;
    private String title;
    private String mdName;
    private String content;
    private String category;

}
