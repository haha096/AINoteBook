package com.example.ai_notebook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotionService {

    @Value("${notion.token}") String token;

    public String fetchPlainText(String pageId) {
        // blocks children 텍스트를 이어붙이는 방식(실서비스는 재귀/문단 처리)
        var url = "https://api.notion.com/v1/blocks/" + pageId + "/children?page_size=100";
        // HttpClient로 GET, 헤더: Authorization: Bearer <token>, Notion-Version: 2022-06-28
        // 결과 JSON에서 rich_text/plain_text를 모아 String으로 반환
        return "...";
    }
}
