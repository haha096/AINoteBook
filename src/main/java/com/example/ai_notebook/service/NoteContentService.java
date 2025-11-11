package com.example.ai_notebook.service;

import com.example.ai_notebook.entity.NoteEntity;
import com.example.ai_notebook.entity.NoteSectionEntity;
import com.example.ai_notebook.entity.NoteSourceEntity;
import com.example.ai_notebook.repository.NoteRepository;
import com.example.ai_notebook.repository.NoteSectionRepository;
import com.example.ai_notebook.repository.NoteSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** 영상 추천용으로 가벼운 문맥 문자열을 만들어줌 */
@Service
@RequiredArgsConstructor
public class NoteContentService {

    private final NoteRepository noteRepo;
    private final NoteSectionRepository sectionRepo;
    private final NoteSourceRepository sourceRepo;

    /** 영상 추천용 간단 컨텍스트 생성 */
    public String buildContext(Long noteId) {
        NoteEntity note = noteRepo.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("note not found: " + noteId));

        String title = safe(note.getTitle());

        // 1) 섹션 본문(body) 취합
        List<NoteSectionEntity> sections = sectionRepo.findByNoteIdOrderByOrdIdxAsc(noteId);
        String body = sections.stream()
                .map(s -> safe(s.getBody()))   // ← 섹션의 body 사용
                .map(this::stripHtml)
                .collect(Collectors.joining("\n"));
        if (body.length() > 2000) body = body.substring(0, 2000);

        // 2) 첨부 파일명 취합 (최신순)
        List<NoteSourceEntity> srcs = sourceRepo.findByNoteIdOrderByIdDesc(noteId);
        String filenames = srcs.stream()
                .map(s -> safe(s.getName()))
                .limit(10)
                .collect(Collectors.joining(", "));

        return """
               [제목] %s
               [파일] %s
               [내용 일부]
               %s
               """.formatted(title, filenames, body);
    }

    private static String safe(String s){ return s==null? "" : s; }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;"," ")
                .replaceAll("\\s+"," ")
                .trim();
    }


    public record NoteContextPieces(String title, String filenamesCsv, String body, String prettyContext) {}

    public NoteContextPieces buildPieces(Long noteId){
        var note = noteRepo.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("note not found: "+noteId));
        String title = safe(note.getTitle());

        var sections = sectionRepo.findByNoteIdOrderByOrdIdxAsc(noteId);
        String body = sections.stream()
                .map(s -> safe(s.getBody()))
                .map(this::stripHtml)
                .collect(Collectors.joining("\n"));
        if (body.length()>2000) body = body.substring(0,2000);

        var srcs = sourceRepo.findByNoteIdOrderByIdDesc(noteId); // ← 앞서 추가했던 메서드
        String filenames = srcs.stream()
                .map(s -> safe(s.getName()))
                .limit(10)
                .collect(Collectors.joining(", "));

        String pretty = """
        [제목] %s
        [파일] %s
        [내용 일부]
        %s
    """.formatted(title, filenames, body);

        return new NoteContextPieces(title, filenames, body, pretty);
    }
}
