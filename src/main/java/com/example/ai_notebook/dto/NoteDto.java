package com.example.ai_notebook.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class NoteDto {
    private String title;
    private String color;
    private Long userId;                  // 프론트에서 로그인된 userId 보내줌
    private List<String> sources;         // 소스 파일명 목록 (선택)
}
