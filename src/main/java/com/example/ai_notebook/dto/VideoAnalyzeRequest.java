package com.example.ai_notebook.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoAnalyzeRequest {
    private String videoId;
    private String title;
    private String description;
    private String transcript;   // 나중에 자막 자동 붙일 때 필요
}
