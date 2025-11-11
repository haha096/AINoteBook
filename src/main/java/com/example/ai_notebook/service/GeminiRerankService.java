package com.example.ai_notebook.service;


import com.example.ai_notebook.dto.RecoItemDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GeminiRerankService {

    @Value("${gemini.apiKey}")   private String apiKey;
    @Value("${gemini.model}")    private String model;
    @Value("${gemini.endpoint}") private String endpoint;

    private final ObjectMapper om = new ObjectMapper();

    /** 후보들을 Gemini로 재랭킹하고 상위 N개 반환 */
    public List<RecoItemDTO> rerank(String noteContext, List<RecoItemDTO> candidates, int take) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        // 1) 후보를 가볍게 축약(JSON 문자열)
        List<Map<String, Object>> compact = new ArrayList<>();
        for (var v : candidates) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("title", v.getTitle());
            m.put("channelTitle", v.getChannelTitle());
            m.put("durationSec", v.getDurationSec());
            m.put("viewCount", v.getViewCount());
            compact.add(m);
        }
        String compactJson;
        try { compactJson = om.writeValueAsString(compact); }
        catch (Exception e) { compactJson = "[]"; }

        // 2) 프롬프트
        String prompt = """
[역할] 너는 학습용 큐레이션 엔진이다. 노트 주제와 '직결'되는 교육 영상을 골라라.

[언어 선호]
- 한국어 영상 우선. 동점이면 한국어 선택.
- 한국어 자막(ko) 있는 영어 영상은 차선 선택.

[선호/페널티 규칙]
- 선호: 강의/세미나/개발 데모/튜토리얼/실습 중심 콘텐츠
- 페널티: 60초 이하 쇼츠/팁, 과도한 홍보/썸네일 낚시, 제목-내용 불일치
- 길이 가이드: 4~20분 선호

[노트요약]
%s

[후보영상(JSON)]
%s

다음 JSON 배열만 출력(설명 금지):
[
  {"id":"<videoId>","score":0.0~1.0,"reason":"한 줄","targetLevel":"BEGINNER|INTERMEDIATE|ADVANCED"}
]
""".formatted(noteContext, compactJson);

        // 3) Gemini 호출
        String raw = callGemini(prompt);
        String normalized = normalizeToJson(raw);

        // 4) 파싱 실패 시 안전 디폴트
        List<Map<String, Object>> ranks;
        try {
            ranks = om.readValue(normalized, new TypeReference<>() {});
        } catch (Exception e) {
            ranks = new ArrayList<>();
            for (var v : candidates) {
                ranks.add(Map.of("id", v.getId(), "score", 0.5, "reason", "", "targetLevel", ""));
            }
        }

        // 5) score/사유 반영 후 정렬
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (var r : ranks) byId.put(String.valueOf(r.get("id")), r);

        candidates.forEach(v -> {
            var r = byId.get(v.getId());
            if (r != null) {
                Object sc = r.get("score");
                v.setScore(sc instanceof Number ? ((Number) sc).doubleValue() : tryDouble(sc));
                v.setReason(Objects.toString(r.getOrDefault("reason", "")));
                v.setTargetLevel(Objects.toString(r.getOrDefault("targetLevel", "")));
            } else {
                v.setScore(0.0);
            }
        });

        return candidates.stream()
                .sorted(Comparator.comparing((RecoItemDTO v) -> v.getScore() == null ? 0.0 : v.getScore()).reversed())
                .limit(Math.max(1, take))
                .toList();
    }

    private Double tryDouble(Object o) {
        try { return Double.parseDouble(String.valueOf(o)); }
        catch (Exception e) { return 0.0; }
    }

    /** Gemini 호출 (v1beta generateContent) */
    private String callGemini(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );
        try {
            var resp = WebClient.builder()
                    .baseUrl(endpoint)
                    .build()
                    .post()
                    .uri("/models/" + model + ":generateContent?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (resp == null) return "[]";
            var cands = (List<Map<String, Object>>) resp.getOrDefault("candidates", List.of());
            if (cands.isEmpty()) return "[]";
            var content = (Map<String, Object>) cands.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.getOrDefault("parts", List.of());
            if (parts.isEmpty()) return "[]";
            return Objects.toString(parts.get(0).get("text"), "[]").trim();
        } catch (Exception ex) {
            System.out.println("[Gemini] call failed: " + ex.getMessage());
            return "[]";
        }
    }

    /** 코드블럭/잡텍스트가 섞인 응답을 JSON 배열만 남기도록 정규화 */
    private String normalizeToJson(String text) {
        if (text == null) return "[]";
        text = text.trim();

        // ```json ... ``` 또는 ``` ... ``` 제거
        if (text.startsWith("```")) {
            int first = text.indexOf('\n');
            int last = text.lastIndexOf("```");
            if (first >= 0 && last > first) {
                text = text.substring(first + 1, last).trim();
            }
        }
        // 대괄호 블록만 추출
        int s = text.indexOf('[');
        int e = text.lastIndexOf(']');
        if (s >= 0 && e >= s) {
            text = text.substring(s, e + 1).trim();
        }
        if (text.isBlank()) return "[]";
        return text;
    }
}
