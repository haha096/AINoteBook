package com.example.ai_notebook.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RecoItemDTO {
    private String id;            // YouTube videoId
    private String title;
    private String url;           // https://www.youtube.com/watch?v=...
    private String channelTitle;
    private String thumbnailUrl;
    private String publishedAt;   // ISO
    private Integer durationSec;
    private Long viewCount;
    private Double score;         // 0~1
    private String reason;        // Gemini 재랭킹 사유
    private String targetLevel;   // BEGINNER|INTERMEDIATE|ADVANCED
}
