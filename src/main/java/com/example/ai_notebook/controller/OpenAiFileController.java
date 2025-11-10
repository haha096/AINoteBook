package com.example.ai_notebook.controller;

import com.example.ai_notebook.repository.NoteSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/openai")
@RequiredArgsConstructor
public class OpenAiFileController {
//    private final OpenAiService openAiService;
//    private final NoteSourceRepository noteSourceRepository;
//
//    @PostMapping("/upload/{sourceId}")
//    public ResponseEntity<?> uploadToOpenAI(@PathVariable Long sourceId) throws IOException {
//        NoteSource src = noteSourceRepository.findById(sourceId)
//                .orElseThrow(() -> new RuntimeException("파일이 존재하지 않습니다."));
//
//        File file = new File(src.getLocalPath());
//        UploadFileResponse res = openAiService.uploadFile("assistants", file);
//
//        src.setOpenaiFileId(res.getId());
//        noteSourceRepository.save(src);
//
//        return ResponseEntity.ok(Map.of("openai_file_id", res.getId()));
//    }
}
