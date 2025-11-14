package com.example.ai_notebook.dto;

import com.example.ai_notebook.entity.VideoEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VideoDto {

    private Long id;
    private String youtubeId;
    private String title;
    private String fullUrl;
    private String summary;
    private LocalDateTime createdAt; // 프론트엔드에 '등록일'로 표시할 수 있음

    // --- 생성자 ---
    public VideoDto() {
        // 기본 생성자
    }

    // (엔티티 -> DTO 변환을 위한 생성자)
    public VideoDto(VideoEntity entity) {
        this.id = entity.getId();
        this.youtubeId = entity.getYoutubeId();
        this.title = entity.getTitle();
        this.fullUrl = entity.getFullUrl();
        this.summary = entity.getSummary();
        this.createdAt = entity.getCreatedAt(); // 엔티티의 createdAt 필드 필요
    }
}
