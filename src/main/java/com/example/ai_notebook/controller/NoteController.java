package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.NoteDto;
import com.example.ai_notebook.entity.*;
import com.example.ai_notebook.repository.NoteRepository;
import com.example.ai_notebook.repository.NoteSectionRepository;
import com.example.ai_notebook.repository.NoteSourceRepository;
import com.example.ai_notebook.service.NoteService;
import com.example.ai_notebook.service.NotionService;
import com.example.ai_notebook.service.SummarizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class NoteController {

    private final NoteRepository noteRepo;
    private final NoteSourceRepository srcRepo;
    private final NoteSectionRepository secRepo;
    private final NoteService noteService;
    private final NotionService notionService;
    private final SummarizeService summarizeService;

    // ✅ URL/NOTION 소스 등록
    @PostMapping("/{id}/sources")
    public NoteSourceEntity addSource(@PathVariable Long id, @RequestBody AddSourceReq req) {
        var note = noteRepo.findById(id).orElseThrow();
        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.valueOf(req.type()));
        src.setName(req.name());
        src.setValue(req.value());
        return srcRepo.save(src);
    }

    public record AddSourceReq(String type, String name, String value) {}

    // ✅ 파일 업로드 소스 등록
    @PostMapping("/{id}/sources/file")
    public NoteSourceEntity addFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        var note = noteRepo.findById(id).orElseThrow();
        Path dir = Paths.get("uploads");
        Files.createDirectories(dir);
        Path path = dir.resolve(UUID.randomUUID() + "_" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.FILE);
        src.setName(file.getOriginalFilename());
        src.setValue(path.toString());
        return srcRepo.save(src);
    }

    // ✅ 요약 생성
    @PostMapping("/{id}/summarize")
    public Map<String, Object> summarize(@PathVariable Long id) {
        var note = noteRepo.findById(id).orElseThrow();
        var sources = srcRepo.findByNoteId(id);

        // 1️⃣ 소스 텍스트 합치기 (지금은 간단히)
        StringBuilder sb = new StringBuilder();
        for (var s : sources) {
            switch (s.getType()) {
                case NOTION -> sb.append(notionService.fetchPlainText(s.getValue()));
                case URL -> sb.append(fetchUrlText(s.getValue()));
                case FILE -> sb.append(extractFileText(Paths.get(s.getValue())));
            }
            sb.append("\n\n");
        }

        // 2️⃣ GPT 요약 → 섹션 저장
        var sections = summarizeService.summarize(note, sb.toString());

        // 기존 섹션 삭제 후 새로 저장
        secRepo.deleteAll(secRepo.findByNoteIdOrderByOrdIdxAsc(id));
        for (int i = 0; i < sections.size(); i++) {
            sections.get(i).setNote(note);
            sections.get(i).setOrdIdx(i);
        }
        secRepo.saveAll(sections);

        // 응답: 프론트에서 바로 렌더링할 수 있게
        var dto = sections.stream()
                .map(s -> Map.of("h2", s.getH2(), "body", s.getBody()))
                .toList();
        return Map.of("sections", dto);
    }

    // --- helpers (초기 단순 버전) ---
    private String fetchUrlText(String url) {
        return "[URL]" + url;
    }

    private String extractFileText(Path path) {
        return "[FILE]" + path.getFileName();
    }

    // ✅ 기존 노트 생성/조회 API
    @PostMapping("/create")
    public NoteEntity createNote(@RequestBody NoteDto dto) {
        return noteService.createNote(dto);
    }

    @GetMapping("/user/{userId}")
    public List<NoteEntity> getNotesByUser(@PathVariable Long userId) {
        return noteService.getNotesByUser(userId);
    }
}
