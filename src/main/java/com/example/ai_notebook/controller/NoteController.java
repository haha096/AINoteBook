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

    private final SourceProcessingService processingService;

    @Value("${app.upload.base:${user.home}/ai-notebook/uploads}")
    private String uploadBase;

    // --- 노트 기본 API ---
    // ( ... getNote, create, listByUser ... 원본과 동일 ... )
    @GetMapping("/{id}")
    public Map<String,Object> getNote(@PathVariable Long id){
        var n = noteRepo.findById(id).orElseThrow();
        return Map.of("id", n.getId(), "title", n.getTitle(), "color", n.getColor());
    }

    @PostMapping("/create")
    public NoteEntity create(@RequestBody NoteDto dto){
        return noteService.createNote(dto);
    }

    @GetMapping("/user/{userId}")
    public List<NoteEntity> listByUser(@PathVariable Long userId){
        return noteService.getNotesByUser(userId);
    }


    // --- 소스 API ---
    // ( ... listSources, addSource, upload ... 원본과 동일 ... )
    @GetMapping("/{id}/sources")
    public List<Map<String, String>> listSources(@PathVariable Long id) {
        // [수정] 프론트엔드 SourceRow 타입에 맞게 openaiFileId도 반환
        return srcRepo.findByNoteId(id).stream()
                .map(s -> Map.of(
                        "name", s.getName(),
                        "path", s.getValue(),
                        "type", s.getType().name(), // FILE, URL 등
                        "openaiFileId", s.getOpenaiFileId() == null ? "" : s.getOpenaiFileId()
                ))
                .toList();
    }

    public record AddSourceReq(String type, String name, String value){}

    @PostMapping("/{id}/sources")
    public NoteSourceEntity addSource(@PathVariable Long id, @RequestBody AddSourceReq req){
        var note = noteRepo.findById(id).orElseThrow();
        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.valueOf(req.type()));
        src.setName(req.name());
        src.setValue(req.value());
        NoteSourceEntity savedSource = srcRepo.save(src);
        processingService.processAndSaveContent(savedSource.getId());
        return savedSource;
    }

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
        // [수정] openAiFileId는 비동기로 처리하므로 여기서 호출 제거
        // try {
        //     openAiFileId = openAiService.uploadFile(path);
        // } catch (Exception e) { openAiFileId = null; }

        var webPath = "/uploads/" + id + "/" + safeName;

        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.FILE);
        src.setName(original);
        src.setValue(webPath);
        src.setOpenaiFileId(null); // ★ 처음엔 null로 저장
        NoteSourceEntity savedSource = srcRepo.save(src);

        // ★ 비동기 텍스트 추출 + OpenAI 파일 업로드
        processingService.processAndSaveContent(savedSource.getId());

        return savedSource;
    }


    // --- AI 질문 API ---

    // ▼▼▼ [추가] 1. '프로 모드' 요청을 받을 DTO (레코드) ▼▼▼
    public record AskStructuredReq(String role, String task, String format) {}

    // ▼▼▼ [추가] 2. '프로 모드'용 새 엔드포인트 ▼▼▼
    /**
     * AI 도우미 (프로 모드): 역할(Role), 작업(Task), 형식(Format)을 지정
     */
    @PostMapping("/{id}/ask-structured")
    public Map<String, Object> askStructured(@PathVariable Long id, @RequestBody AskStructuredReq req) throws Exception {

        // 1. 노트의 파일 컨텍스트를 읽어옵니다. (헬퍼 메소드 사용)
        NoteContext noteCtx = getFileContextForNote(id);

        // 2. 파일이 없으면, 파일 없다는 응답 반환
        if (!noteCtx.hasFiles()) {
            return Map.of(
                    "answer", "(첨부된 파일이 없습니다)",
                    "fileCount", 0
            );
        }

        // 3. OpenAiService의 'askWithPromptTemplate' (새 메소드) 호출
        // (이 메소드는 OpenAiService.java에 만들어야 합니다)
        var answer = openAiService.askWithPromptTemplate(
                req.role(),
                req.task(),
                noteCtx.context(), // 파일에서 읽어온 텍스트
                req.format()
        );

        // 4. 프론트엔드로 응답 반환
        return Map.of("answer", answer, "fileCount", noteCtx.fileCount());
    }


    /**
     * AI 도우미 (일반 모드): 단순 질문
     */
    @PostMapping("/{id}/ask")
    public Map<String, Object> ask(@PathVariable Long id, @RequestParam("q") String q) throws Exception {

        // 1. 노트의 파일 컨텍스트를 읽어옵니다. (헬퍼 메소드 사용)
        NoteContext noteCtx = getFileContextForNote(id);

        // 2. 파일 유무에 따라 분기 처리
        if (!noteCtx.hasFiles()) {
            return Map.of("answer","이 노트에 첨부된 파일이 없습니다. 먼저 파일을 업로드해주세요.", "fileCount", 0);
        }
        if (noteCtx.context().isEmpty()) {
            return Map.of("answer","첨부 파일을 읽을 수 없었습니다(형식 미지원/파일 미존재).", "fileCount", noteCtx.fileCount());
        }

        // 3. OpenAiService의 'askWithContext' (기존 메소드) 호출
        var answer = openAiService.askWithContext(q, noteCtx.context());

        // 4. 프론트엔드로 응답 반환
        return Map.of("answer", answer, "fileCount", noteCtx.fileCount());
    }


    // ▼▼▼ [추가] 3. 파일 컨텍스트 읽기 (중복 로직 헬퍼 메소드) ▼▼▼
    /**
     * [헬퍼] ID에 해당하는 노트의 모든 FILE 소스 내용을 텍스트로 읽어옵니다.
     */
    private NoteContext getFileContextForNote(Long id) throws Exception {
        var sources = srcRepo.findByNoteId(id).stream()
                .filter(s -> s.getType() == SourceType.FILE)
                .toList();

        if (sources.isEmpty()) {
            return new NoteContext("", 0, false); // 파일 없음
        }

        StringBuilder ctx = new StringBuilder();
        int fileCountWithContent = 0;

        for (var s : sources) {
            String content = s.getProcessedTextContent();

            // ★★★ 핵심 수정 ★★★
            // DB에 추출된 텍스트가 있는지 확인
            if (content != null && !content.isBlank()) {
                ctx.append("--- 파일 시작: ").append(s.getName()).append(" ---\n");
                ctx.append(content);
                ctx.append("\n--- 파일 끝: ").append(s.getName()).append(" ---\n\n");
                fileCountWithContent++;
            }
            // (else: 텍스트가 없으면 (아직 처리 중이거나 실패) 컨텍스트에 포함 안 함)
        }

        // ★★★ 핵심 수정 ★★★
        // 파일은 있지만, 아직 텍스트가 추출된 파일이 1개도 없는 경우
        if (fileCountWithContent == 0) {
            return new NoteContext(
                    "(아직 파일 내용을 처리 중이거나 읽을 수 있는 파일이 없습니다. 잠시 후 다시 시도해 주세요.)",
                    sources.size(), // 파일 개수 자체는 맞음
                    true
            );
        }

        return new NoteContext(ctx.toString(), sources.size(), true); // 파일 있음
    }

    /**
     * [헬퍼] 파일 컨텍스트 반환용 내부 레코드
     */
    private record NoteContext(String context, int fileCount, boolean hasFiles) {}


    // --- 기타 유틸 API ---
    // ( ... linkExisting, summarize ... 원본과 동일 ... )
    @PostMapping("/{id}/sources/link-existing")
    public NoteSourceEntity linkExisting(
            @PathVariable Long id,
            @RequestParam("safeName") String safeName
    ) {
        // ... (원본 코드)
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

    @PostMapping("/{id}/summarize")
    public Map<String,Object> summarize(@PathVariable Long id){
        // ... (원본 코드)
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
