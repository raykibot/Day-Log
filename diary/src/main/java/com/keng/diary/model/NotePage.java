package com.keng.diary.model;

import lombok.Data;

import java.util.List;

@Data
public class NotePage {
    private List<Note> items;
    private int page;
    private int size;
    private boolean hasMore;

    public NotePage(List<Note> items, int page, int size, boolean hasMore) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.hasMore = hasMore;
    }
}
