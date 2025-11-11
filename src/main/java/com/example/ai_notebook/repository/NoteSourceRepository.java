package com.example.ai_notebook.repository;

import com.example.ai_notebook.entity.NoteSectionEntity;
import com.example.ai_notebook.entity.NoteSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteSourceRepository extends JpaRepository<NoteSourceEntity, Long> {
    List<NoteSourceEntity> findByNoteId(Long noteId);

    @Query("SELECT s FROM NoteSourceEntity s WHERE s.note.id = :noteId ORDER BY s.id DESC")
    List<NoteSourceEntity> findByNoteIdOrderByIdDesc(@Param("noteId") Long noteId);
}
