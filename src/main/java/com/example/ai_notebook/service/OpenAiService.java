package com.example.ai_notebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

// ★★★ 추가됨 ★★★
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class OpenAiService {

    // ★★★ 추가됨 ★★★
    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper om = new ObjectMapper();

    public OpenAiService(
            @Value("${openai.baseUrl:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.apiKey}") String apiKey
    ) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("OpenAI-Beta", "assistants=v2") // file_search용
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** OpenAI Files 업로드: 반환값은 file-xxxx */
    public String uploadFile(Path path) throws IOException, InterruptedException {
        // ... (이 메소드는 원본과 동일)
        byte[] fileBytes = Files.readAllBytes(path);
        String boundary = "----GPTBOUNDARY" + UUID.randomUUID();

        String part1 =
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"purpose\"\r\n\r\n" +
                        "assistants\r\n" +
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + path.getFileName() + "\"\r\n" +
                        "Content-Type: application/octet-stream\r\n\r\n";

        String partEnd = "\r\n--" + boundary + "--";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/files"))
                .header("Authorization", "Bearer " + apiKey)
                .header("OpenAI-Beta", "assistants=v2")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(
                        part1.getBytes(StandardCharsets.UTF_8),
                        fileBytes,
                        partEnd.getBytes(StandardCharsets.UTF_8)
                )))
                .build();

        HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new IOException("File upload failed: " + res.statusCode() + "\n" + res.body());
        }
        JsonNode json = om.readTree(res.body());
        return json.get("id").asText(); // file-xxxx
    }

    /** * [새싹 모드] 순수 텍스트 컨텍스트로 질문
     * ★★★ 여기가 수정되었습니다 ★★★
     */
    public String askWithContext(String question, String context) {
        String ctx = context != null && context.length() > 15000
                ? context.substring(0, 15000) : (context == null ? "" : context);

        String systemPrompt = "너는 첨부된 [자료]를 바탕으로 사용자의 [질문]에 답변하는 친절하고 상세한 AI 도우미야. " +
                "항상 명확하고 이해하기 쉽게 설명해줘.";

        String userContent = "다음 [자료]를 바탕으로 [질문]에 답해줘." +
                "\n\n[자료]\n" + ctx +
                "\n\n[질문]\n" + question;

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", List.of(Map.of("type", "input_text", "text", systemPrompt))
                        ),
                        Map.of(
                                "role", "user",
                                "content", List.of(Map.of("type", "input_text", "text", userContent))
                        )
                ),
                "max_output_tokens", 800
        );

        try {
            Map<?, ?> resp = webClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // ★★★ 추가됨: 토큰 사용량 로깅 ★★★
            logTokenUsage(resp);

            return extractOutputText(resp);

        } catch (WebClientResponseException e) {
            // ★★★ 추가됨: 에러 발생 시에도 로그 레벨을 warn으로 변경 ★★★
            log.warn("OpenAI API 요청 실패 (WebClient): {} {}\n{}", e.getRawStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
            return "OpenAI 요청 실패: " + e.getRawStatusCode() + " " + e.getStatusText()
                    + "\n" + e.getResponseBodyAsString();
        } catch (Exception e) {
            // ★★★ 추가됨: 에러 발생 시에도 로그 레벨을 error로 변경 ★★★
            log.error("OpenAI API 요청 실패 (General): {}", e.getMessage(), e);
            return "OpenAI 요청 실패: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractOutputText(Map<?,?> resp) {
        // ... (이 메소드는 원본과 동일)
        if (resp == null) return "(빈 응답)";
        Object outText = resp.get("output_text");
        if (outText instanceof String s && !s.isBlank()) return s;

        Object output = resp.get("output");
        if (output instanceof List<?> outList && !outList.isEmpty()) {
            Object msg0 = outList.get(0);
            if (msg0 instanceof Map<?,?> m0) {
                Object content = m0.get("content");
                if (content instanceof List<?> cList && !cList.isEmpty()) {
                    Object c0 = cList.get(0);
                    if (c0 instanceof Map<?,?> mC0) {
                        Object text = mC0.get("text");
                        if (text instanceof String s2 && !s2.isBlank()) return s2;
                    }
                }
            }
        }
        return resp.toString(); // 최후 안전망
    }


    /**
     * [프로 모드] 역할, 작업, 맥락, 형식을 기반으로 구조화된 프롬프트를 생성
     * ★★★ "마스터 템플릿" 방식으로 수정됨 ★★★
     */
    public String askWithPromptTemplate(String role, String task, String context, String format) {

        // 1. '프로 모드'용 마스터 템플릿을 조립합니다.

        // 1-1. 시스템 프롬프트 (AI 역할)
        // 사용자가 'role'을 입력하면, "전문가" 템플릿에 끼워넣습니다.
        // 입력 안 하면 '학습 도우미' 기본값을 씁니다.
        String systemPrompt;
        if (role != null && !role.isBlank()) {
            systemPrompt = "너는 '" + role + "' 역할을 맡은 AI 전문가야. " +
                    "주어진 [참고 자료]를 바탕으로 [지시]에 대해 '전문가 수준'으로 상세하고 체계적인 답변을 제공해야 해.";
        } else {
            // '프로 모드'의 기본 역할 (사용자가 '역할'을 안 썼을 때)
            systemPrompt = "너는 'AI 학습 도우미'야. [참고 자료]를 바탕으로 [지시]에 대해 '전문가 수준의 교사'처럼 상세하고 교육적인 답변을 제공해야 해.";
        }

        // 1-2. 유저 프롬프트 (작업, 형식, 자료)
        StringBuilder userContent = new StringBuilder();

        userContent.append("## 지시 (Task)\n");
        userContent.append(task != null ? task : "내 말을 이어서 말해줘.");
        userContent.append("\n\n");

        if (context != null && !context.isBlank()) {
            String safeContext = context.length() > 15000
                    ? context.substring(0, 15000) + "\n... (내용이 너무 길어 생략됨)"
                    : context;
            userContent.append("## 참고 자료 (Context)\n");
            userContent.append(safeContext);
            userContent.append("\n\n");
        }

        // 사용자가 'format'을 입력하면, 템플릿에 끼워넣습니다.
        // 입력 안 하면 '3단계 학습' 기본값을 씁니다. (이게 핵심!)
        userContent.append("## 출력 형식 (Format)\n");
        if (format != null && !format.isBlank()) {
            // 사용자가 입력한 형식 (예: "5단계로 나눠서 설명")
            userContent.append(format);
        } else {
            // ★★★ 사용자가 형식을 지정하지 않았을 때의 '프로 모드' 기본 템플릿 ★★★
            userContent.append("답변은 **반드시** 다음 3단계 구조의 마크다운 형식으로 작성해 줘:\n\n" +
                    "### 1. 핵심 요약 (Core Answer)\n" +
                    "[지시]에 대한 가장 중요하고 직접적인 답변을 1-2문장으로 먼저 제시해.\n\n" +
                    "### 2. 상세 설명 (Detailed Explanation)\n" +
                    "[참고 자료]의 내용을 근거로, 핵심 요약에 대한 구체적인 이유, 배경, 원리, 그리고 예시를 상세하게 설명해 줘.\n\n" +
                    "### 3. 심화 학습 (Further Study)\n" +
                    "사용자가 이 주제를 더 깊이 파고들 수 있도록, [참고 자료]의 내용과 연관된 **새로운 심화 질문 2가지**를 '다음 질문 제안:'이라는 제목으로 제시해 줘.");
        }


        // 2. API 요청 본문 생성
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", List.of(Map.of("type", "input_text", "text", systemPrompt))
        ));
        messages.add(Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "input_text", "text", userContent.toString()))
        ));
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "input", messages,
                "max_output_tokens", 1000 // 프로 모드는 넉넉하게
        );

        // 3. API 호출 (이하 동일)
        try {
            Map<?, ?> resp = webClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            logTokenUsage(resp);
            return extractOutputText(resp);

        } catch (WebClientResponseException e) {
            log.warn("OpenAI API 요청 실패 (WebClient): {} {}\n{}", e.getRawStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
            return "OpenAI 요청 실패: " + e.getRawStatusCode() + " " + e.getStatusText()
                    + "\n" + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("OpenAI API 요청 실패 (General): {}", e.getMessage(), e);
            return "OpenAI 요청 실패: " + e.getMessage();
        }
    }


    /**
     * ★★★ 추가됨 ★★★
     * OpenAI API 응답에서 토큰 사용량을 추출하여 로그로 남깁니다.
     */
    @SuppressWarnings("unchecked")
    private void logTokenUsage(Map<?, ?> resp) {
        if (resp == null) return;

        Object usageObj = resp.get("usage");
        if (usageObj instanceof Map<?, ?> usageMap) {

            // API 엔드포인트마다 필드 이름이 다를 수 있어 두 경우 모두 확인
            // (e.g., 'input_tokens' or 'prompt_tokens')
            Object input = usageMap.get("input_tokens");
            Object output = usageMap.get("output_tokens");
            Object total = usageMap.get("total_tokens");

            if (input == null) input = usageMap.get("prompt_tokens");
            if (output == null) output = usageMap.get("completion_tokens");

            log.info("[OpenAI Usage] Input: {} tokens, Output: {} tokens, Total: {} tokens",
                    input != null ? input : "N/A",
                    output != null ? output : "N/A",
                    total != null ? total : "N/A"
            );
        } else {
            // usage 객체가 없는 경우 (혹시 모를 예외상황 대비)
            log.warn("[OpenAI Usage] 'usage' object not found in API response. Response keys: {}", resp.keySet());
        }
    }
}