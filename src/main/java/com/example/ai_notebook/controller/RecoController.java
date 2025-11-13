package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.RecoItemDTO;
import com.example.ai_notebook.service.NoteContentService;
import com.example.ai_notebook.service.OpenaiRerankService;
import com.example.ai_notebook.service.YoutubeSearchService;
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

    // --- 의존성 (사용자님의 새 아키텍처) ---
    private final NoteContentService noteContentService;
    private final YoutubeSearchService youtubeSearchService;
    private final OpenaiRerankService openaiRerankService;

    /**
     * [기존] 노트 ID 기반 영상 추천 (문서 추천)
     * ★★★ POST에서 GET으로 수정 ★★★
     * 프론트엔드(Notes_detail.tsx)에서 GET으로 호출하고 있으므로 @GetMapping이 맞습니다.
     */
    @GetMapping("/videos/{noteId}")
    public List<RecoItemDTO> recommendVideos(@PathVariable Long noteId) {
        // 1. 컨텍스트 생성 (Good)
        var pieces = noteContentService.buildPieces(noteId);
        log.info("[Debug] Note ID [{}]: Context Generated:\n{}", noteId, pieces.prettyContext());

        // 2. AI 검색어 생성 (Good)
        String smartQuery = openaiRerankService.generateQuery(pieces.body());
        log.info("[Debug] Note ID [{}]: OpenAI Generated Query: [{}]", noteId, smartQuery);

        // 3. 유튜브 검색 (Good)
        // (YoutubeSearchService.searchTopSmart는 RecoItemDTO를 반환합니다)
        var candidates = youtubeSearchService.searchTopSmart(smartQuery, 12);
        log.info("[Debug] Note ID [{}]: YouTube Search Candidates Count: {}", noteId, candidates.size());

        // 4. AI 재랭킹 (Good)
        var result = openaiRerankService.rerank(pieces.prettyContext(), candidates, 4);
        log.info("[Debug] Note ID [{}]: OpenAI Reranked Result Count: {}", noteId, result.size());

        return result;
    }


    /**
     * [신규] 사용자가 입력한 키워드 기반 영상 추천
     * (프론트엔드에서 /api/reco/videos/keyword?q=... 로 호출)
     */
    @GetMapping("/videos/keyword")
    public List<RecoItemDTO> recommendVideosByKeyword(@RequestParam("q") String keyword) {
        log.info("[Debug] Keyword Search: Starting for keyword [{}]", keyword);

        // 1. 사용자가 입력한 키워드로 유튜브 검색 (재사용)
        // (YoutubeSearchService.searchTopSmart는 RecoItemDTO를 반환합니다)
        var candidates = youtubeSearchService.searchTopSmart(keyword, 12);
        log.info("[Debug] Keyword Search [{}]: YouTube Search Candidates Count: {}", keyword, candidates.size());

        // 2. AI를 이용해 후보 영상 리랭킹 (재사용)
        // (참고) 컨텍스트로 '키워드' 자체를 사용합니다.
        var result = openaiRerankService.rerank(keyword, candidates, 4);
        log.info("[Debug] Keyword Search [{}]: OpenAI Reranked Result Count: {}", keyword, result.size());

        return result;
    }

    // (제가 제안했던 코드는 모두 삭제하셔도 됩니다. 이 파일이 맞습니다.)
}