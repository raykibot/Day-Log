package com.keng.diary.model.VO;

import lombok.Data;

import com.keng.diary.model.NoteImage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class NoteVo {

    private Long id;
    private String title;
    private String mdName;
    private String content;
    private String category;
    private Date createTime;
    private Date updateTime;
    private List<NoteImage> images = new ArrayList<>();
}
