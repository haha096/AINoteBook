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

import java.nio.file.*;
import java.util.*;

//NoteApiController를 없애고 깔끔하게 NoteController로 함

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
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
    @PostMapping(
            value = "/{id}/sources/file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public NoteSourceEntity upload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        // 0) 노트 로드 (← 없어서 NPE/컴파일 에러 났던 것)
        var note = noteRepo.findById(id).orElseThrow();

        // 1) 로컬 저장 (노트별 폴더)
        var dir = Paths.get(uploadBase, String.valueOf(id));
        Files.createDirectories(dir);

        var original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        var safeName = UUID.randomUUID() + "_" + original;
        var path = dir.resolve(safeName);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        // 2) (옵션) OpenAI Files 업로드
        String openAiFileId = null;
        try {
            openAiFileId = openAiService.uploadFile(path);   // 구현 전이면 주석처리 가능
        } catch (Exception ignore) {
            // 아직 OpenAI 연동 안했으면 예외 무시하거나 로그만 남겨도 됨
        }

        // 3) DB에는 '웹에서 접근 가능한 경로'를 저장
        var webPath = "/uploads/" + id + "/" + safeName;

        var src = new NoteSourceEntity();
        src.setNote(note);
        src.setType(SourceType.FILE);
        src.setName(original);
        src.setValue(webPath);           // 프론트에서 바로 열 수 있는 경로
        src.setOpenaiFileId(openAiFileId);

        // 4) 저장 후 엔티티 그대로 반환 (프론트의 SourceRow와 일치)
        return srcRepo.save(src);
    }

    // 파일 기반 질문
    @PostMapping("/{id}/ask")
    public Map<String,Object> ask(@PathVariable Long id, @RequestParam("q") String q){
        var fileIds = srcRepo.findByNoteId(id).stream()
                .filter(s -> s.getType()==SourceType.FILE && s.getOpenaiFileId()!=null && !s.getOpenaiFileId().isBlank())
                .map(NoteSourceEntity::getOpenaiFileId)
                .toList();
        if (fileIds.isEmpty()) return Map.of("answer", "이 노트에 첨부된 파일이 없습니다. 먼저 파일을 업로드해주세요.");
        var answer = openAiService.askWithFiles(q, fileIds);
        return Map.of("answer", answer, "fileCount", fileIds.size());
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
