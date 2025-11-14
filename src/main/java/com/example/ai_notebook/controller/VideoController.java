package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.VideoAnalyzeRequest;
import com.example.ai_notebook.dto.VideoAnalyzeResponse;
import com.example.ai_notebook.service.VideoService;
import com.example.ai_notebook.service.YouTubeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    // (1) YouTubeDataService 의존성 주입
    private final YouTubeDataService youTubeDataService;

    @PostMapping("/analyze")
    public ResponseEntity<VideoAnalyzeResponse> analyze(@RequestParam("url") String url) {

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // (2) 1단계: URL로 YouTube API에서 정보 가져오기
            // (이 메서드들은 YouTubeDataService에 구현해야 합니다)
            String videoId = youTubeDataService.extractVideoId(url); // URL에서 ID 추출
            String title = youTubeDataService.fetchTitle(videoId);
            String transcript = youTubeDataService.fetchTranscript(videoId);

            // (3) 2단계: 가져온 정보로 Gemini API에 요약 요청
            String summary = videoService.analyzeVideo(title, transcript);

            // (4) 3단계: 요약 결과 반환
            return ResponseEntity.ok(new VideoAnalyzeResponse(summary));

        } catch (Exception e) {
            // (예: 자막이 없는 영상, 잘못된 URL 등 모든 예외 처리)
            // log.error("분석 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new VideoAnalyzeResponse("영상 분석에 실패했습니다: " + e.getMessage()));
        }
    }
}
