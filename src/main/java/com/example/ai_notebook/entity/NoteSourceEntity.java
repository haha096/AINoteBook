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

    @Lob // 대용량 텍스트(CLOB) 지정을 위해
    @Column(name = "processed_text_content", columnDefinition = "LONGTEXT")
    private String processedTextContent;

    @Column(name = "openai_file_id", length = 120)
    private String openaiFileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    @JsonIgnore
    private NoteEntity note;
}
