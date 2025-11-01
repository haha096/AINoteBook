package com.example.ai_notebook.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="note_sections")
@Getter
@Setter
public class NoteSectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int ordIdx;              // 정렬
    private String h2;

    @Column(columnDefinition="TEXT")
    private String body;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="note_id")
    @JsonIgnore
    private NoteEntity note;
}
