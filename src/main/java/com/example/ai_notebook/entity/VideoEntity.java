package com.example.ai_notebook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "videos") // 'videos' 테이블과 매핑
public class VideoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "youtube_id", nullable = false, unique = true, length = 20)
    private String youtubeId; // YouTube 영상 고유 ID

    @Column(name = "title")
    private String title; // 영상 제목

    @Column(name = "full_url", nullable = false, length = 512)
    private String fullUrl; // 사용자가 입력한 원본 URL

    @Lob // 긴 텍스트 (TEXT 타입)
    @Column(name = "summary")
    private String summary; // 요약문

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- 생성자 ---
    public VideoEntity() {
    }

    // (필요시) 서비스에서 사용할 생성자
    public VideoEntity(String youtubeId, String title, String fullUrl) {
        this.youtubeId = youtubeId;
        this.title = title;
        this.fullUrl = fullUrl;
    }
}
