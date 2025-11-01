package com.example.ai_notebook.repository;

import com.example.ai_notebook.entity.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRepository extends JpaRepository<NoteEntity, Long> {
    List<NoteEntity> findByUserId(Long userId);
}
