package com.keng.diary.mapper;

import com.keng.diary.model.NoteImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {
    void insert(NoteImage image);

    List<NoteImage> list(long noteId);

    NoteImage findByName(@Param("noteId") long noteId, @Param("name") String name);

    void delete(long id);
}
