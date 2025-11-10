package com.example.ai_notebook.controller;

import com.example.ai_notebook.entity.NoteEntity;
import com.example.ai_notebook.entity.NoteSourceEntity;
import com.example.ai_notebook.entity.SourceType;
import com.example.ai_notebook.repository.NoteRepository;
import com.example.ai_notebook.repository.NoteSourceRepository;
import com.example.ai_notebook.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

//@RestController
//@RequiredArgsConstructor
public class NoteApiController {

//    private final NoteRepository noteRepo;
//    private final NoteSourceRepository srcRepo;
//    private final OpenAiService openAiService;
//
//    @Value("${app.upload.base:${user.home}/ai-notebook/uploads}")
//    private String uploadBase;
//
//    @GetMapping("/api/notes/{id}")
//    public Map<String, Object> getNote(@PathVariable Long id) {
//        var dto = new HashMap<String, Object>();
//        dto.put("id", id);
//        dto.put("title", "노트");
//        return dto;
//    }
//
//    @PostMapping("/api/notes/{id}/content")
//    public Map<String, Object> saveContent(@PathVariable Long id, @RequestBody Map<String, Object> body) {
//        System.out.println("[SAVE] note " + id + " :: title=" + body.get("title"));
//        return Map.of("ok", true);
//    }
//
//    /** ✅ 파일 업로드 (DB 저장 + OpenAI Files 업로드) */
//    @PostMapping(value = "/api/notes/{id}/sources/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public NoteSourceEntity uploadSource(@PathVariable Long id,
//                                         @RequestParam("file") MultipartFile file) throws Exception {
//        NoteEntity note = noteRepo.findById(id).orElseThrow();
//
//        // 노트별 폴더
//        Path dir = Paths.get(uploadBase, String.valueOf(id));
//        Files.createDirectories(dir);
//
//        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
//        String safeName = UUID.randomUUID() + "_" + original;
//        Path saveTo = dir.resolve(safeName);
//        Files.copy(file.getInputStream(), saveTo, StandardCopyOption.REPLACE_EXISTING);
//
//        // OpenAI Files 업로드 → file-xxxx
//        String openAiFileId = openAiService.uploadFile(saveTo);
//
//        // DB 저장
//        NoteSourceEntity src = new NoteSourceEntity();
//        src.setNote(note);
//        src.setType(SourceType.FILE);
//        src.setName(original);
//        src.setValue(saveTo.toString());     // 로컬 경로
//        src.setOpenaiFileId(openAiFileId);   // 중요
//        return srcRepo.save(src);
//    }
//
//    /** ✅ 소스 목록 조회 (DB 기준) */
//    @GetMapping("/api/notes/{id}/sources")
//    public List<NoteSourceEntity> listSources(@PathVariable Long id) {
//        return srcRepo.findByNoteId(id);
//    }
}
