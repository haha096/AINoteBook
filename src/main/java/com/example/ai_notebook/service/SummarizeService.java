package com.example.ai_notebook.service;

import com.example.ai_notebook.entity.NoteEntity;
import com.example.ai_notebook.entity.NoteSectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummarizeService {

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    public List<NoteSectionEntity> summarize(NoteEntity note, String fullText) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("⚠️ [SummarizeService] OpenAI API 키 없음 → 더미 요약 사용");
            var dummy = new NoteSectionEntity();
            dummy.setH2("요약 (임시)");
            dummy.setBody("OpenAI API 키가 없어 요약을 생략했습니다. (" + fullText.length() + "자)");
            return List.of(dummy);
        }

        // TODO: 실제 OpenAI 호출 로직 (나중에)
        return List.of();
    }
}