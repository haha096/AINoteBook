package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.NoteDto;
import com.example.ai_notebook.entity.*;
import com.example.ai_notebook.repository.NoteRepository;
import com.example.ai_notebook.repository.NoteSectionRepository;
import com.example.ai_notebook.repository.NoteSourceRepository;
import com.example.ai_notebook.service.NoteService;
import com.example.ai_notebook.service.SourceProcessingService; // ★ 수정됨: import
import com.example.ai_notebook.service.SummarizeService;
import com.example.ai_notebook.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteRepository noteRepo;
    private final NoteSourceRepository srcRepo;
    private final NoteSectionRepository secRepo;
    private final NoteService noteService;
    private final SummarizeService summarizeService;
    private final OpenAiService openAiService;

    private final SourceProcessingService processingService; // ★ 수정됨: 주입

    @Value("${app.upload.base:${user.home}/ai-notebook/uploads}")
    private String uploadBase;

    // --- 노트 기본 API (빠졌던 부분들) ---

    // 노트 기본 정보
    @GetMapping("/{id}")
    public Map<String,Object> getNote(@PathVariable Long id){
        var n = noteRepo.findById(id).orElseThrow();
        return Map.of("id", n.getId(), "title", n.getTitle(), "color", n.getColor());
    }

    // 노트 생성
    @PostMapping("/create")
    public NoteEntity create(@RequestBody NoteDto dto){
        // NoteService의 createNote를 호출 (여기는 텍스트 추출 로직이 없음)
        return noteService.createNote(dto);
    }

    // 노트 목록 조회 (404 에러났던 부분)
    @GetMapping("/user/{userId}")
    public List<NoteEntity> listByUser(@PathVariable Long userId){
        return noteService.getNotesByUser(userId);
    }

    // --- 소스 API (수정된 부분들) ---

    // 소스 목록 조회 (405 에러났던 부분)
    @GetMapping("/{id}/sources")
    public List<Map<String, String>> listSources(@PathVariable Long id) {
        return srcRepo.findByNoteId(id).stream()
                .map(s -> Map.of(
                        "name", s.getName(),
                        "path", s.getValue()
                ))
                .toList();
    }

    // URL/NOTION 소스 등록용 레코드 (빠졌던 부분)
    public record AddSourceReq(String type, String name, String value){}

    // URL/NOTION 소스 등록
    @PostMapping("/{id}/sources")
    public NoteSourceEntity addSource(@PathVariable Long id, @RequestBody AddSourceReq req){
        var note = noteRepo.findById(id).orElseThrow();
        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.valueOf(req.type()));
        src.setName(req.name());
        src.setValue(req.value());

        NoteSourceEntity savedSource = srcRepo.save(src);

        // ★ 수정됨: 비동기 텍스트 추출 호출
        processingService.processAndSaveContent(savedSource.getId());

        return savedSource;
    }

    // 파일 업로드
    @PostMapping(value = "/{id}/sources/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NoteSourceEntity upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws Exception {
        var note = noteRepo.findById(id).orElseThrow();

        var dir = java.nio.file.Paths.get(uploadBase, String.valueOf(id));
        java.nio.file.Files.createDirectories(dir);
        var original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        var safeName = original;
        java.nio.file.Path path = dir.resolve(safeName);
        java.nio.file.Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String openAiFileId = null;
        try {
            openAiFileId = openAiService.uploadFile(path);
        } catch (Exception e) {
            openAiFileId = null;
        }
        var webPath = "/uploads/" + id + "/" + safeName;

        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.FILE);
        src.setName(original);
        src.setValue(webPath);
        src.setOpenaiFileId(openAiFileId);

        NoteSourceEntity savedSource = srcRepo.save(src);

        // ★ 수정됨: 비동기 텍스트 추출 호출
        processingService.processAndSaveContent(savedSource.getId());

        return savedSource;
    }

    // --- 기타 유틸 API (원본 유지) ---

    // 파일 기반 질문
    @PostMapping("/{id}/ask")
    public Map<String, Object> ask(@PathVariable Long id, @RequestParam("q") String q) throws Exception {
        var sources = srcRepo.findByNoteId(id).stream()
                .filter(s -> s.getType() == SourceType.FILE)
                .toList();
        if (sources.isEmpty()) return Map.of("answer","이 노트에 첨부된 파일이 없습니다. 먼저 파일을 업로드해주세요.");

        StringBuilder ctx = new StringBuilder();
        for (var s : sources) {
            String fileName = java.nio.file.Paths.get(s.getValue()).getFileName().toString();
            var local = java.nio.file.Paths.get(uploadBase, String.valueOf(id), fileName);
            if (java.nio.file.Files.exists(local)) {
                if (fileName.toLowerCase().endsWith(".pdf")) {
                    try (var doc = org.apache.pdfbox.pdmodel.PDDocument.load(local.toFile())) {
                        var stripper = new org.apache.pdfbox.text.PDFTextStripper();
                        ctx.append(stripper.getText(doc)).append("\n\n");
                    }
                } else {
                    ctx.append(java.nio.file.Files.readString(local)).append("\n\n");
                }
            }
        }
        if (ctx.length()==0) return Map.of("answer","첨부 파일을 읽을 수 없었습니다(형식 미지원/파일 미존재).");
        var answer = openAiService.askWithContext(q, ctx.toString());
        return Map.of("answer", answer, "fileCount", sources.size());
    }

    @PostMapping("/{id}/sources/link-existing")
    public NoteSourceEntity linkExisting(
            @PathVariable Long id,
            @RequestParam("safeName") String safeName
    ) {
        var note = noteRepo.findById(id).orElseThrow();
        var local = java.nio.file.Paths.get(uploadBase, String.valueOf(id), safeName);
        if (!java.nio.file.Files.exists(local)) {
            throw new IllegalArgumentException("파일이 없습니다: " + local);
        }
        var webPath = "/uploads/" + id + "/" + safeName;

        var existing = srcRepo.findByNoteId(id).stream()
                .filter(s -> webPath.equals(s.getValue()))
                .findFirst();
        if (existing.isPresent()) return existing.get();

        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.FILE);
        src.setName(safeName.substring(safeName.indexOf('_')+1));
        src.setValue(webPath);
        src.setOpenaiFileId(null);
        return srcRepo.save(src);
    }

    // 요약
    @PostMapping("/{id}/summarize")
    public Map<String,Object> summarize(@PathVariable Long id){
        var note = noteRepo.findById(id).orElseThrow();
        var buf = new StringBuilder();
        srcRepo.findByNoteId(id).forEach(s -> {
            if (s.getType()==SourceType.FILE) buf.append("[FILE]").append(Paths.get(s.getValue()).getFileName());
            else if (s.getType()==SourceType.URL) buf.append("[URL]").append(s.getValue());
            buf.append("\n\n");
        });
        var sections = summarizeService.summarize(note, buf.toString());
        secRepo.deleteAll(secRepo.findByNoteIdOrderByOrdIdxAsc(id));
        for (int i=0;i<sections.size();i++){ sections.get(i).setNote(note); sections.get(i).setOrdIdx(i); }
        secRepo.saveAll(sections);
        var dto = sections.stream().map(s -> Map.of("h2", s.getH2(), "body", s.getBody())).toList();
        return Map.of("sections", dto);
    }
}
