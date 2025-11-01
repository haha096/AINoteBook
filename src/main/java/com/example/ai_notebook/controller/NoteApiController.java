package com.example.ai_notebook.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class NoteApiController {

    @GetMapping("/api/notes/{id}")
    public Map<String, Object> getNote(@PathVariable Long id) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", id);
        dto.put("title", "노트");     // 초기 제목
        return dto;                   // 200 OK
    }

    /** 본문/제목 임시 저장 (프론트 Ctrl+S가 호출) */
    @PostMapping("/api/notes/{id}/content")
    public Map<String, Object> saveContent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        // TODO: DB 저장 로직으로 교체
        System.out.println("[SAVE] note " + id + " :: title=" + body.get("title"));
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        return res;
    }

    /** 파일 업로드 (소스 패널 + 버튼) */
    @PostMapping(value = "/api/notes/{id}/sources/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadSource(@PathVariable Long id,
                                            @RequestPart("file") MultipartFile file) throws Exception {
        Path dir = Paths.get("uploads").resolve(String.valueOf(id));
        Files.createDirectories(dir);

        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        // 확장자 분리
        int dot = original.lastIndexOf('.');
        String base = (dot >= 0) ? original.substring(0, dot) : original;
        String ext  = (dot >= 0) ? original.substring(dot) : "";

        // 기본은 원래 이름
        Path saveTo = dir.resolve(original);

        // 중복이면 base (1).ext, base (2).ext … 로 찾기
        int i = 1;
        while (Files.exists(saveTo)) {
            saveTo = dir.resolve(base + " (" + i + ")" + ext);
            i++;
        }

        Files.copy(file.getInputStream(), saveTo);

        String storedName = saveTo.getFileName().toString();
        Map<String, Object> res = new HashMap<>();
        res.put("name", storedName); // 프론트에 보이는 이름
        res.put("path", "/uploads/" + id + "/" + storedName);
        return res;
    }

    @GetMapping("/api/notes/{id}/sources")
    public List<Map<String, Object>> listSources(@PathVariable Long id) throws Exception {
        Path dir = Paths.get("uploads").resolve(String.valueOf(id));
        if (!Files.exists(dir)) return List.of();

        try (var s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        Map<String,Object> m = new HashMap<>();
                        m.put("name", fileName);
                        // 프론트에서 클릭해 열 수 있는 경로 (아래 1-B 설정과 세트)
                        m.put("path", "/uploads/" + id + "/" + fileName);
                        return m;
                    })
                    .toList();
        }
    }
}
