package com.keng.diary.mapper;

import com.keng.diary.model.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface INoteMapper {

    int insert(Note note);

    Note find(long id);

    List<Note> list(@Param("category") String category, @Param("limit") int limit,
                    @Param("offset") long offset);

    int update(Note note);

    int softDelete(@Param("id") long id, @Param("deleteTime") LocalDateTime deleteTime);

    List<Note> getNoteListByCategory(@Param("category") String category);
}
