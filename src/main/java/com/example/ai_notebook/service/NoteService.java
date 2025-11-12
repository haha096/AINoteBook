package com.example.ai_notebook.service;


import com.example.ai_notebook.dto.NoteDto;
import com.example.ai_notebook.entity.*;
import com.example.ai_notebook.repository.NoteRepository;
import com.example.ai_notebook.repository.NoteSourceRepository;
import com.example.ai_notebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteSourceRepository noteSourceRepository;
    private final UserRepository userRepository;
    private final SourceProcessingService processingService; // (이전에 추가했는지 확인)

    @Transactional
    public NoteEntity createNote(NoteDto dto) {
        UserEntity user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        NoteEntity note = new NoteEntity();
        note.setTitle(dto.getTitle());
        note.setColor(dto.getColor());
        note.setUser(user);

        List<NoteSourceEntity> sources = new ArrayList<>();
        if (dto.getSources() != null) {
            for (String srcValue : dto.getSources()) {
                NoteSourceEntity source = new NoteSourceEntity();

                if (srcValue.startsWith("http://") || srcValue.startsWith("https://")) {
                    source.setType(SourceType.URL);
                    source.setName(srcValue);
                } else {
                    source.setType(SourceType.FILE);
                    source.setName(extractFileName(srcValue));
                }

                source.setValue(srcValue);
                source.setNote(note);
                sources.add(source);
            }
        }
        note.setSources(sources);

        NoteEntity savedNote = noteRepository.save(note);

        // (3) ★★★ 여기에 로그 추가 ★★★
        log.info("[Debug] Note created: {}. Source count: {}", savedNote.getId(), savedNote.getSources().size());

        for (NoteSourceEntity savedSource : savedNote.getSources()) {

            // (4) ★★★ 여기에 로그 추가 ★★★
            log.info("[Debug] Calling SourceProcessingService for source ID: {}", savedSource.getId());

            processingService.processAndSaveContent(savedSource.getId());
        }

        return savedNote;
    }

    public List<NoteEntity> getNotesByUser(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    private String extractFileName(String filePath) {
        try {
            int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            return (lastSlash >= 0) ? filePath.substring(lastSlash + 1) : filePath;
        } catch (Exception e) {
            return filePath;
        }
    }
}
