package com.example.ai_notebook.service;

import com.example.ai_notebook.dto.VideoAnalyzeRequest;
import com.example.ai_notebook.dto.VideoAnalyzeResponse;
import com.example.ai_notebook.dto.VideoDto;
import com.example.ai_notebook.entity.VideoEntity;
import com.example.ai_notebook.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VideoService {

    @Value("${gemini.apiKey}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;        // gemini-1.5-flash-latest
    @Value("${gemini.endpoint}")
    private String endpoint;     // https://generativelanguage.googleapis.com/v1beta

    private WebClient webClient;

    // @PostConstruct에서 endpoint 값 세팅된 뒤 WebClient 생성
    @jakarta.annotation.PostConstruct
    public void initClient() {
        this.webClient = WebClient.builder()
                .baseUrl(endpoint)    // ==> https://generativelanguage.googleapis.com/v1beta
                .build();
        log.info("Gemini WebClient initialized. endpoint={}", endpoint);
    }

    /**
     * @param videoTitle     (수정) 유튜브 영상 제목
     * @param fullTranscript (수정) 유튜브 영상의 전체 자막 텍스트
     * @return Gemini API가 생성한 요약 텍스트
     */
    public String analyzeVideo(String videoTitle, String fullTranscript) { // (1) 시그니처 변경
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("gemini.apiKey (GEMINI_API_KEY)가 설정되지 않았습니다.");
        }

        // (2) 프롬프트 포맷팅 수정
        String prompt = """
당신은 전문 요약 어시턴트입니다.
다음은 "%s" 유튜브 영상의 전체 자막입니다.

---[자막 시작]---
%s
---[자막 끝]---

이 영상의 전체 내용을 명확하고 간결하게 요약해 주세요.
다음 구조를 반드시 사용하고, 자막에 제공된 내용에만 충실하게 초점을 맞춰주세요:

1.  **영상의 핵심 주제:**
    (이 영상이 무엇에 대해 이야기하는지 1-2줄로 명확하게 요약)

2.  **주요 내용:**
    * (자막 내용을 바탕으로 영상의 주요 포인트를 3-5개 항목으로 나누어 요약)
    * (각 항목은 구체적인 사실이나 영상 속 발언을 기반으로 작성)

3.  **영상의 결론 또는 핵심 메시지:**
    (영상이 최종적으로 전달하고자 하는 결론, 요점, 또는 가장 중요한 메시지)
""".formatted(videoTitle, fullTranscript); // <-- videoTitle과 fullTranscript를 전달

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            // WebClient 호출 로직
            Map response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            // baseUrl: https://generativelanguage.googleapis.com/v1beta
                            // 최종 URL: /v1beta/models/{model}:generateContent?key=...
                            .path("/models/" + model + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(body)
                    .retrieve()          // <-- 1. 요청을 "실행"합니다.
                    .bodyToMono(Map.class) // <-- 2. 응답 본문을 Mono<Map>으로 변환합니다.
                    .block();            // <-- 3. Mono가 완료될 때까지 "대기"합니다.

            return extractText(response);

        } catch (WebClientResponseException e) {
            log.error("Gemini 호출 실패 status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Gemini 호출 실패: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Gemini 호출 중 예외", e);
            throw new RuntimeException("Gemini 호출 중 예외: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map response) {
        if (response == null) return "Gemini 응답이 없습니다.";

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "Gemini candidates가 비어 있습니다.";
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> p : parts) {
            Object t = p.get("text");
            if (t != null) sb.append(t.toString()).append("\n");
        }
        return sb.toString().trim();
    }
}