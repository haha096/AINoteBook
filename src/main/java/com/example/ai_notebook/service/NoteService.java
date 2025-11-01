package com.example.ai_notebook.service;


import com.example.ai_notebook.dto.NoteDto;
import com.example.ai_notebook.entity.*;
import com.example.ai_notebook.repository.NoteRepository;
import com.example.ai_notebook.repository.NoteSourceRepository;
import com.example.ai_notebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteSourceRepository noteSourceRepository;
    private final UserRepository userRepository;

    public NoteEntity createNote(NoteDto dto) {
        UserEntity user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        NoteEntity note = new NoteEntity();
        note.setTitle(dto.getTitle());
        note.setColor(dto.getColor());
        note.setUser(user);

        // 소스 추가
        List<NoteSourceEntity> sources = new ArrayList<>();
        if (dto.getSources() != null) {
            for (String src : dto.getSources()) {
                NoteSourceEntity source = new NoteSourceEntity();
                source.setType(SourceType.FILE);
                source.setName(src);
                source.setValue(src);
                source.setNote(note);
                sources.add(source);
            }
        }

        note.setSources(sources);
        return noteRepository.save(note);
    }

    public List<NoteEntity> getNotesByUser(Long userId) {
        return noteRepository.findByUserId(userId);
    }
}
