package com.example.ai_notebook.service;

import com.example.ai_notebook.entity.NoteSourceEntity;
import com.example.ai_notebook.entity.SourceType;
import com.example.ai_notebook.repository.NoteSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service; // (1) ★★★ @Service import 확인 ★★★
import org.springframework.transaction.annotation.Transactional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceProcessingService {

    private final NoteSourceRepository sourceRepo;

    @Value("${app.upload.base:${user.home}/ai-notebook/uploads}")
    private String uploadBase;

    @Async
    @Transactional
    public void processAndSaveContent(Long sourceId) {
        NoteSourceEntity source = sourceRepo.findById(sourceId).orElse(null);
        if (source == null) {
            log.warn("[SourceProcessing] Source not found: {}", sourceId);
            return;
        }

        String textContent = "";
        try {
            log.info("[SourceProcessing] Starting processing for source: {}", sourceId);

            if (source.getType() == SourceType.URL) {
                // TODO: Jsoup 등으로 URL 본문 스크래핑
                textContent = "[임시] Jsoup으로 파싱한 URL 텍스트: " + source.getValue();
                // (1) ★★★ URL 스크래핑 후에도 청소 로직 호출 ★★★
                textContent = cleanExtractedText(textContent);

            } else if (source.getType() == SourceType.FILE) {
                String webPath = source.getValue();
                String noteIdStr = source.getNote().getId().toString();
                String fileName = Paths.get(webPath).getFileName().toString();

                var localPath = Paths.get(uploadBase, noteIdStr, fileName);

                if (Files.exists(localPath)) {
                    if (fileName.toLowerCase().endsWith(".pdf")) {
                        try (var doc = PDDocument.load(localPath.toFile())) {
                            var stripper = new PDFTextStripper();
                            textContent = stripper.getText(doc);
                        }
                    } else {
                        textContent = Files.readString(localPath);
                    }

                    // (2) ★★★ 추출된 텍스트 청소 (정확한 위치) ★★★
                    textContent = cleanExtractedText(textContent);

                } else {
                    textContent = "[파일을 찾을 수 없음: " + localPath + "]";
                    log.warn("[SourceProcessing] File not found at path: {}", localPath);
                }
            }

            if (textContent.length() > 5000) { // <-- 청소 후에 길이 제한
                textContent = textContent.substring(0, 5000);
            }

            source.setProcessedTextContent(textContent);
            log.info("[SourceProcessing] Successfully processed source: {}", sourceId);

        } catch (Exception e) {
            log.error("[SourceProcessing] Failed to process source: " + sourceId, e);
            source.setProcessedTextContent("[텍스트 추출 실패: " + e.getMessage() + "]");
        }
    }

    // (3) ★★★ 텍스트 청소 헬퍼 메서드 (정확한 위치) ★★★
    private String cleanExtractedText(String text) {
        if (text == null) return "";

        // 1. 의미 없는 특수 문자, 기호 제거 (알파벳, 숫자, 한글, 공백만 남김)
        String cleaned = text.replaceAll("[^a-zA-Z0-9가-힣\\s]", " ");

        // 2. 여러 개의 공백을 하나로 축소
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }
}
