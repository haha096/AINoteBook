package com.example.ai_notebook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_sources")
@Getter
@Setter
public class NoteSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SourceType type;  // FILE, URL, NOTION

    private String name;   // 표시용 이름
    private String value;  // 파일경로 / URL / Notion pageId

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    @JsonIgnore
    private NoteEntity note;
}
