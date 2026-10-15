package com.keng.diary.model.VO;

import lombok.Data;

import java.util.Date;

@Data
public class NoteVo {

    private Long id;
    private String title;
    private String mdName;
    private String content;
    private String category;
    private Date createTime;
    private Date updateTime;
}
