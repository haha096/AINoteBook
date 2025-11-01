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

    @Value("${openai.apiKey}") String apiKey;

    public List<NoteSectionEntity> summarize(NoteEntity note, String mergedText) {
        String prompt = """
    아래 강의/자료 내용을 바탕으로 한국어 노트필기 섹션을 만들어 주세요.
    - 섹션 4~7개, 각 섹션: h2(짧은 소제목) + 본문(3~6문장)
    - 중요 용어는 **굵게**
    자료:
    """ + mergedText;

        // OpenAI Chat Completions 호출 (gpt-4o-mini 등)
        // 응답을 파싱해 sections[]로 만들고 NoteSectionEntity 목록 생성
        // ordIdx=0..N, h2/body 세팅
        return List.of(/* ... */);
    }
}