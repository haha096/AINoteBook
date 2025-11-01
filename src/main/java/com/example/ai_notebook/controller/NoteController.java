package com.example.ai_notebook.controller;

import com.example.ai_notebook.dto.NoteDto;
import com.example.ai_notebook.entity.NoteEntity;
import com.example.ai_notebook.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class NoteController {

    private final NoteService noteService;

    @PostMapping("/create")
    public NoteEntity createNote(@RequestBody NoteDto dto) {
        return noteService.createNote(dto);
    }

    @GetMapping("/user/{userId}")
    public List<NoteEntity> getNotesByUser(@PathVariable Long userId) {
        return noteService.getNotesByUser(userId);
    }
}
