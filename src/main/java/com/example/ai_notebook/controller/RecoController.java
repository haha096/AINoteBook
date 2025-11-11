package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.RecoItemDTO;
import com.example.ai_notebook.service.NoteContentService;
import com.example.ai_notebook.service.YoutubeSearchService;
import com.example.ai_notebook.service.GeminiRerankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reco")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173","http://localhost:5174"}, allowCredentials = "true")
public class RecoController {

    private final NoteContentService noteContentService;
    private final YoutubeSearchService youtubeSearchService;
    private final GeminiRerankService geminiRerankService;

    @PostMapping("/videos/{noteId}")
    public List<RecoItemDTO> recommendVideos(@PathVariable Long noteId) {
        var pieces = noteContentService.buildPieces(noteId);

        // 1) 압축 질의로 유튜브 후보 찾기(Shorts/초장편 배제)
        var candidates = youtubeSearchService.searchTopSmart(
                pieces.title(), pieces.filenamesCsv(), pieces.body(), 12
        );

        // 2) Gemini 재랭킹(규칙 강화)
        return geminiRerankService.rerank(pieces.prettyContext(), candidates, 4);
    }

    @GetMapping("/videos/{noteId}")
    public List<RecoItemDTO> recommendVideosGet(@PathVariable Long noteId) {
        return recommendVideos(noteId);
    }
}
