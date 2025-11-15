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
import java.util.regex.Pattern; // ★★★ import 추가
import java.util.stream.Collectors; // ★★★ import 추가

@Service
@RequiredArgsConstructor
public class GeminiRerankService {

    @Value("${gemini.apiKey}")   private String apiKey;
    @Value("${gemini.model}")    private String model;
    @Value("${gemini.endpoint}") private String endpoint;

    private final ObjectMapper om = new ObjectMapper();

    // (1) ★★★ QueryUtil의 키워드 추출 로직을 여기로 가져옴 ★★★
//    private static final Pattern TOKENIZER = Pattern.compile("[^0-9A-Za-z가-힣]+");
//    private static final Set<String> STOP = Set.of(
//            "the","and","for","with","from","this","that","you","your",
//            "are","can","how","use","file","files","data","pdf","ppt",
//            "doc","docs","docx","pptx","내용","섹션","요약","분석","정리",
//            "자료","예제","수업","강의","과제","프로젝트","노트","소스",
//            // --- lec02.pdf 노이즈 총정리 ---
//            "e", "w1", "w2", "x1", "x2", "head", "model", "diff",
//            "large", "예측치", "정답", "를곱하여예측치를계산", "초기랜덤값",
//            "content", "test", "test1", "test2", "test3", "test4", "test5",
//            "forward", "가중치업데이트", "을활용하여", "데이터에가중치"
//    );


    private String normalizeToQuery(String text) {
        if (text == null) return "";
        text = text.trim();

        if (text.startsWith("```")) {
            int first = text.indexOf('\n');
            int last = text.lastIndexOf("```");
            if (first >= 0 && last > first) {
                text = text.substring(first + 1, last).trim();
            }
        }
        text = text.replace("\"", "").replace("'", "");
        if (text.contains(":")) {
            text = text.substring(text.indexOf(":") + 1).trim();
        }
        text = text.replace("\n", " ").trim();

        if (text.isBlank()) return "";
        return text;
    }

    /**
     * (수정됨) 긴 본문 내용을 바탕으로 YouTube 검색 키워드 1~2개를 생성
     * (Gemini 호출 X)
     */
    public String generateQuery(String noteBody) {
        if (noteBody == null || noteBody.isBlank()) return "학습 튜토리얼";

        // ★★★ [수정됨] 특정 도메인 예시를 제거한 범용 프롬프트 ★★★
        String prompt = """
        [역할] 너는 입력된 문서를 분석하여 유튜브 검색 키워드를 생성하는 '키워드 추출 엔진'이다.
        
        [임무]
        다음 [입력 내용]을 읽고, 이 문서의 주제를 가장 잘 나타내는 **핵심 주제어 3~4개**를 조합하여 유튜브 검색어를 만들어라.
        
        [가이드]
        - 문서 전체의 주제를 관통하는, 가장 중요하고 **고유한 단어**에 집중해야 한다.
        - '문제', '방법', '이해', '학습', '파일', 'Chapter', 'Test', 'Note' 처럼,
          너무 일반적이거나 모든 문서에 등장할 수 있는 단어는 **반드시 제외**해야 한다.
        
        [규칙]
        - 절대 설명하지 마라.
        - 따옴표도 붙이지 마라.
        - 키워드(검색어)만 텍스트로 출력해라.
        
        [입력 내용]
        %s
        """.formatted(noteBody);

        // (이하 로직은 동일)
        String raw = callGemini(prompt);
        String query = normalizeToQuery(raw);

        if (query.isBlank()) {
            return "IT 학습 튜토리얼";
        }

        System.out.println("[Gemini Query Gen]: " + query); // ★ 생성된 쿼리 확인용 로그
        return query;
    }


    /** * 후보들을 Gemini로 재랭킹하고 상위 N개 반환
     * (기존 코드 유지 - 404 에러가 나도 안전장치가 동작할 것임)
     */
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

        // 2) ★★★ [수정] 올바른 '재랭킹' 프롬프트로 복원 ★★★
        String prompt = """
[역할] 너는 학습용 큐레이션 엔진이다. 소스 내용와 '직결'되는 교육 영상을 골라라.

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
""".formatted(noteContext, compactJson); // ★ compactJson을 다시 사용합니다.

        // 3) Gemini 호출 (404 에러가 나도 catch에서 처리됨)
        String raw = callGemini(prompt);
        String normalized = normalizeToJson(raw); // ★ "[]" 배열을 기대합니다.

        // 4) 파싱 실패 시 안전 디폴트 (404 에러 시 여기가 동작함)
        List<Map<String, Object>> ranks;
        try {
            ranks = om.readValue(normalized, new TypeReference<>() {});
        } catch (Exception e) {
            ranks = new ArrayList<>();
            for (var v : candidates) {
                ranks.add(Map.of("id", v.getId(), "score", 0.5, "reason", "", "targetLevel", ""));
            }
        }

        // ... (이하 5) score/사유 반영 후 정렬... 코드는 모두 동일합니다) ...
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

    /** * Gemini 호출 (v1beta generateContent)
     * (기존 코드 유지 - rerank에서만 사용되며, 404 에러 시 안전장치가 처리함)
     */
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
