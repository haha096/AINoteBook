package com.example.ai_notebook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// (1) WebClient 관련 임포트 추가
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map; // (Map 임포트 추가)
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class YouTubeDataService {

    // (정규식 부분은 동일)
    private static final String VIDEO_ID_REGEX =
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com/(?:watch\\?v=|embed/|v/)|youtu\\.be/)([^&?#]+)";
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(VIDEO_ID_REGEX);

    // (2) WebClient 필드 추가
    private final WebClient webClient;

    // (3) 생성자에서 WebClient 초기화 (파이썬 API 주소)
    public YouTubeDataService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:5001") // Python Flask 서버 주소
                .build();
    }

    /**
     * 전체 YouTube URL에서 Video ID만 추출합니다. (동일)
     */
    public String extractVideoId(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL이 비어있습니다.");
        }
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            String videoId = matcher.group(1);
            log.info("추출된 Video ID: {}", videoId);
            return videoId;
        } else {
            log.warn("URL에서 Video ID를 추출하지 못했습니다: {}", url);
            throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다.");
        }
    }

    /**
     * (제목은 우선 임시 하드코딩 유지)
     */
    public String fetchTitle(String videoId) {
        log.info("{} ID로 제목을 가져옵니다. (임시)", videoId);
        return "임시 제목 - " + videoId;
    }

    /**
     * (4) 🔽🔽🔽 가장 중요! 🔽🔽🔽
     * Whisper API를 호출하여 '진짜' 자막을 가져옵니다.
     * @param url (주의!) videoId가 아닌 '전체 URL'을 받습니다.
     * @return 추출된 자막 텍스트
     */
    public String fetchTranscript(String url) { // (시그니처가 videoId -> url로 변경됨)
        log.info("Whisper API 호출 시작. URL: {}", url);

        // Python API에 POST로 보낼 JSON 본문 ({"url": "..."})
        Map<String, String> body = Map.of("url", url);

        try {
            // POST http://localhost:5001/transcribe
            // (Map.class 대신 Map.class를 사용)
            Map response = webClient.post()
                    .uri("/transcribe") // Python 서버의 엔드포인트
                    .bodyValue(body)    // JSON 본문 설정
                    .retrieve()         // 요청 실행
                    .bodyToMono(Map.class) // 응답을 Map으로 받음
                    .block();           // 동기식으로 대기

            if (response == null || response.get("text") == null) {
                log.warn("Whisper API 응답이 비어있거나 'text' 필드가 없습니다.");
                throw new RuntimeException("자막을 가져오지 못했습니다. (Whisper 응답 없음)");
            }

            String transcript = (String) response.get("text");
            log.info("Whisper API 자막 추출 성공. (첫 50자: {})", transcript.substring(0, Math.min(transcript.length(), 50)));
            return transcript;

        } catch (WebClientResponseException e) {
            log.error("Whisper API 호출 실패 status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("자막 생성 실패 (Whisper API): " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Whisper API 호출 중 예외", e);
            throw new RuntimeException("자막 생성 중 예외: " + e.getMessage());
        }
    }
}