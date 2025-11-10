package com.example.ai_notebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
                // ★ file_search는 Responses/Assistants v2 베타 헤더가 필수
                .defaultHeader("OpenAI-Beta", "assistants=v2")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** OpenAI Files 업로드: 반환값은 file-xxxx */
    public String uploadFile(Path path) throws IOException, InterruptedException {
        byte[] fileBytes = Files.readAllBytes(path);
        String boundary = "----GPTBOUNDARY" + UUID.randomUUID();

        String part1 =
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"purpose\"\r\n\r\n" +
                        "assistants\r\n" +
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + path.getFileName() + "\"\r\n" +
                        // 간단하게 MIME 지정 (필요하면 확장자별 조건 분기)
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

    /** 파일 검색(file_search)로 질문하기 */
    public String askWithFiles(String question, List<String> fileIds) {
        // 1) attachments 구성
        List<Map<String, Object>> attachments = new ArrayList<>();
        for (String fid : fileIds) {
            attachments.add(Map.of(
                    "file_id", fid,
                    "tools", List.of(Map.of("type", "file_search"))
            ));
        }

        // 2) 요청 바디
        Map<String, Object> body = Map.of(
                "model", "gpt-4.1-mini", // ← attachments 지원 모델 (gpt-4.1 / gpt-4.1-mini)
                "tools", List.of(Map.of("type", "file_search")),
                "attachments", attachments,                // ★ 최상위에 둬야 함
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of(
                                "type", "input_text",
                                "text", question
                        ))
                )),
                "max_output_tokens", 800
        );

        try {
            Map<?, ?> resp = webClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractOutputText(resp);

        } catch (WebClientResponseException e) {
            return "OpenAI 요청 실패: " + e.getRawStatusCode() + " " + e.getStatusText()
                    + "\n" + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "OpenAI 요청 실패: " + e.getMessage();
        }
    }

    /** (옵션) 순수 텍스트 컨텍스트로 질문 (file_search 미사용) */
    public String askWithContext(String question, String context) {
        String ctx = context != null && context.length() > 15000
                ? context.substring(0, 15000) : (context == null ? "" : context);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of(
                                "type", "input_text",
                                "text", "다음 자료를 바탕으로 질문에 답해줘.\n\n[자료]\n" + ctx + "\n\n[질문]\n" + question
                        ))
                )),
                "max_output_tokens", 800
        );

        try {
            Map<?, ?> resp = webClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractOutputText(resp);

        } catch (WebClientResponseException e) {
            return "OpenAI 요청 실패: " + e.getRawStatusCode() + " " + e.getStatusText()
                    + "\n" + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "OpenAI 요청 실패: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractOutputText(Map<?,?> resp) {
        if (resp == null) return "(빈 응답)";
        Object outText = resp.get("output_text");
        if (outText instanceof String s && !s.isBlank()) return s;

        // fallback: output[0].content[0].text
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
}

