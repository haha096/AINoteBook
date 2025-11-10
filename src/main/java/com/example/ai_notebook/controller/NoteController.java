package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.NoteDto;
import com.example.ai_notebook.entity.*;
import com.example.ai_notebook.repository.NoteRepository;
import com.example.ai_notebook.repository.NoteSectionRepository;
import com.example.ai_notebook.repository.NoteSourceRepository;
import com.example.ai_notebook.service.NoteService;
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
//@CrossOrigin(origins = "http://localhost:5174")
public class NoteController {

    private final NoteRepository noteRepo;
    private final NoteSourceRepository srcRepo;
    private final NoteSectionRepository secRepo;
    private final NoteService noteService;
    private final SummarizeService summarizeService;
    private final OpenAiService openAiService;

    @Value("${app.upload.base:${user.home}/ai-notebook/uploads}")
    private String uploadBase;

    // 노트 기본 정보
    @GetMapping("/{id}")
    public Map<String,Object> getNote(@PathVariable Long id){
        var n = noteRepo.findById(id).orElseThrow();
        return Map.of("id", n.getId(), "title", n.getTitle(), "color", n.getColor());
    }

    // 노트 생성/조회
    @PostMapping("/create")
    public NoteEntity create(@RequestBody NoteDto dto){ return noteService.createNote(dto); }
    @GetMapping("/user/{userId}")
    public List<NoteEntity> listByUser(@PathVariable Long userId){ return noteService.getNotesByUser(userId); }

    // 소스 목록 (DB 기준)
    @GetMapping("/{id}/sources")
    public List<Map<String, String>> listSources(@PathVariable Long id) {
        return srcRepo.findByNoteId(id).stream()
                .map(s -> Map.of(
                        "name", s.getName(),
                        "path", s.getValue()   // web 경로(/uploads/..)
                ))
                .toList();
    }

    // URL/NOTION 소스 등록 (NOTION 당장은 안 쓰면 URL만 사용)
    public record AddSourceReq(String type, String name, String value){}
    @PostMapping("/{id}/sources")
    public NoteSourceEntity addSource(@PathVariable Long id, @RequestBody AddSourceReq req){
        var note = noteRepo.findById(id).orElseThrow();
        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.valueOf(req.type())); // "URL" | "FILE" | "NOTION"
        src.setName(req.name());
        src.setValue(req.value());
        return srcRepo.save(src);
    }

    // 파일 업로드 + OpenAI Files 업로드 + DB 저장
    @PostMapping(value = "/{id}/sources/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NoteSourceEntity upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws Exception {
        var note = noteRepo.findById(id).orElseThrow();

        var dir = java.nio.file.Paths.get(uploadBase, String.valueOf(id));
        java.nio.file.Files.createDirectories(dir);

        var original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        //var safeName = java.util.UUID.randomUUID() + "_" + original;
        var safeName = original;
        java.nio.file.Path path = dir.resolve(safeName);
        java.nio.file.Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // ★ OpenAI Files 업로드 → file-xxxx 받기
        String openAiFileId = null;
        try {
            openAiFileId = openAiService.uploadFile(path);
        } catch (Exception e) {
            // 실패해도 로컬 경로는 남겨둠 (원하면 throw로 바꿔도 됨)
            openAiFileId = null;
        }

        var webPath = "/uploads/" + id + "/" + safeName;

        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.FILE);
        src.setName(original);
        src.setValue(webPath);           // 프론트가 열 수 있는 경로
        src.setOpenaiFileId(openAiFileId); // ★ 저장

        return srcRepo.save(src);
    }


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
        var answer = openAiService.askWithContext(q, ctx.toString());   // ← 이거!!!
        return Map.of("answer", answer, "fileCount", sources.size());
    }

    @PostMapping("/{id}/sources/link-existing")
    public NoteSourceEntity linkExisting(
            @PathVariable Long id,
            @RequestParam("safeName") String safeName // 예: f3fbc910-..._lec02.pdf
    ) {
        var note = noteRepo.findById(id).orElseThrow();
        var local = java.nio.file.Paths.get(uploadBase, String.valueOf(id), safeName);
        if (!java.nio.file.Files.exists(local)) {
            throw new IllegalArgumentException("파일이 없습니다: " + local);
        }
        var webPath = "/uploads/" + id + "/" + safeName;

        // 이미 같은 경로가 있으면 재사용
        var existing = srcRepo.findByNoteId(id).stream()
                .filter(s -> webPath.equals(s.getValue()))
                .findFirst();
        if (existing.isPresent()) return existing.get();

        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.FILE);
        src.setName(safeName.substring(safeName.indexOf('_')+1)); // 원본명 추출
        src.setValue(webPath);
        src.setOpenaiFileId(null); // B안은 필요 없음
        return srcRepo.save(src);
    }

    // 요약(파일명/URL을 합쳐 간단 요약 → 나중에 파일 파서로 교체)
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
