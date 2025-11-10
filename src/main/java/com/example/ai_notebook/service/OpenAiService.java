package com.example.ai_notebook.service;

import com.example.ai_notebook.config.OpenAiProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final WebClient client;
    private final OpenAiProperties props;

    public OpenAiService(WebClient openAiWebClient, OpenAiProperties props) {
        this.client = openAiWebClient;
        this.props = props;
    }

    /** 1) 간단 채팅(빠른 동작 확인용) - /chat/completions */
    public String chat(String userMessage) {
        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        Map<String, Object> res = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        try {
            // choices[0].message.content
            List<Map<String, Object>> choices = (List<Map<String, Object>>) res.get("choices");
            Map<String, Object> first = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) first.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            return "OpenAI error: " + e.getMessage();
        }
    }

    /** 2) 파일 업로드 - /files (purpose: assistants 또는 responses) */
    public String uploadFile(Path filePath) {
        // 보통 purpose는 "assistants" 사용 (responses도 가능)
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("purpose", "assistants");
        form.add("file", new FileSystemResource(filePath.toFile()));

        Map<String, Object> res = client.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 반환: file.id (예: "file_abc123")
        return (String) res.get("id");
    }

    /** 3) 파일 삭제 - /files/{id} */
    public boolean deleteFile(String fileId) {
        Map<String, Object> res = client.delete()
                .uri("/files/{id}", fileId)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> Mono.just(Map.of("deleted", false)))
                .block();

        Object deleted = res.get("deleted");
        return deleted instanceof Boolean && (Boolean) deleted;
    }

    /** 4) 업로드한 파일을 첨부해서 답변 받기 - /responses (file_search 사용) */
    public String askWithFiles(String prompt, List<String> fileIds) {
        // attachments + tools=file_search 를 선언
        var attachments = fileIds.stream()
                .map(id -> Map.of(
                        "file_id", id,
                        "tools", List.of(Map.of("type", "file_search"))
                ))
                .toList();

        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "input", List.of(
                        Map.of("role", "user", "content", prompt,
                                "attachments", attachments)
                ),
                "tools", List.of(Map.of("type", "file_search"))
        );

        Map<String, Object> res = client.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // responses API: output_text 추출
        // (형식은 버전에 따라 다소 달라질 수 있어, 가장 보편적인 output_text 우선 시도)
        try {
            Map<String, Object> out = (Map<String, Object>) res.get("output");
            if (out != null && "output_text".equals(out.get("type"))) {
                return (String) out.get("text");
            }
            // fallback: 전체 JSON 반환
            return res.toString();
        } catch (Exception e) {
            return res != null ? res.toString() : "No response";
        }
    }
}

