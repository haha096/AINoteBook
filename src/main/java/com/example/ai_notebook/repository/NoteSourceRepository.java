package com.example.ai_notebook.repository;

import com.example.ai_notebook.entity.NoteSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteSourceRepository extends JpaRepository<NoteSourceEntity, Long> {
    List<NoteSourceEntity> findByNoteId(Long noteId);
}
