package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.RecoItemDTO;
import com.example.ai_notebook.service.NoteContentService;
import com.example.ai_notebook.service.OpenaiRerankService;
import com.example.ai_notebook.service.YoutubeSearchService;
import com.example.ai_notebook.service.GeminiRerankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j // Slf4j 어노테이션
@RestController
@RequestMapping("/api/reco")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173","http://localhost:5174"}, allowCredentials = "true")
public class RecoController {

    private final NoteContentService noteContentService;
    private final YoutubeSearchService youtubeSearchService;
    // private final GeminiRerankService geminiRerankService; // ★★★ 주석 처리
    private final OpenaiRerankService openaiRerankService; // ★★★ OpenAI 서비스로 변경

    @PostMapping("/videos/{noteId}")
    public List<RecoItemDTO> recommendVideos(@PathVariable Long noteId) {
        var pieces = noteContentService.buildPieces(noteId);
        log.info("[Debug] Note ID [{}]: Context Generated:\n{}", noteId, pieces.prettyContext());

        // (1) ★★★ OpenAI에게 "검색어" 생성 요청 ★★★
        String smartQuery = openaiRerankService.generateQuery(pieces.body()); // ★★★ 변경
        log.info("[Debug] Note ID [{}]: OpenAI Generated Query: [{}]", noteId, smartQuery); // ★★★ 로그 변경

        // (2) ★★★ (OpenAI가) 만든 검색어로 YouTube 후보 찾기 ★★★
        var candidates = youtubeSearchService.searchTopSmart(smartQuery, 12);
        log.info("[Debug] Note ID [{}]: YouTube Search Candidates Count: {}", noteId, candidates.size());

        // (3) ★★★ OpenAI에게 "재랭킹" 요청 ★★★
        var result = openaiRerankService.rerank(pieces.prettyContext(), candidates, 4); // ★★★ 변경
        log.info("[Debug] Note ID [{}]: OpenAI Reranked Result Count: {}", noteId, result.size()); // ★★★ 로그 변경

        return result;
    }

    @GetMapping("/videos/{noteId}")
    public List<RecoItemDTO> recommendVideosGet(@PathVariable Long noteId) {
        return recommendVideos(noteId);
    }
}
