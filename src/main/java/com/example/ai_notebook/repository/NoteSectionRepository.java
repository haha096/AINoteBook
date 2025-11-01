package com.example.ai_notebook.repository;

import com.example.ai_notebook.entity.NoteSectionEntity; // 이미 있다면 그대로
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteSectionRepository extends JpaRepository<NoteSectionEntity, Long> {
    List<NoteSectionEntity> findByNoteIdOrderByOrdIdxAsc(Long noteId);
}
