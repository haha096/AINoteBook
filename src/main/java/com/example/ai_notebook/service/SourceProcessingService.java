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
import java.nio.file.Path;
import java.nio.file.Paths;

import com.example.ai_notebook.service.OpenAiService;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceProcessingService {

    private final NoteSourceRepository sourceRepo;

    // ▼▼▼ 2. OpenAiService 주입 ▼▼▼
    private final OpenAiService openAiService;

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
                textContent = cleanExtractedText(textContent);

            } else if (source.getType() == SourceType.FILE) {
                String webPath = source.getValue();
                String noteIdStr = source.getNote().getId().toString();
                String fileName = Paths.get(webPath).getFileName().toString();

                Path localPath = Paths.get(uploadBase, noteIdStr, fileName); // (수정) Path 타입으로 받기

                if (Files.exists(localPath)) {
                    // --- 1. 텍스트 추출 (기존 로직) ---
                    if (fileName.toLowerCase().endsWith(".pdf")) {
                        try (var doc = PDDocument.load(localPath.toFile())) {
                            var stripper = new PDFTextStripper();
                            textContent = stripper.getText(doc);
                        }
                    } else {
                        textContent = Files.readString(localPath);
                    }
                    textContent = cleanExtractedText(textContent);

                    // --- ▼▼▼ 3. OpenAI 파일 업로드 (추가된 로직) ▼▼▼ ---
                    try {
                        String fileId = openAiService.uploadFile(localPath);
                        source.setOpenaiFileId(fileId);
                        log.info("[SourceProcessing] Uploaded to OpenAI, fileId: {} for source: {}", fileId, sourceId);
                    } catch (Exception e) {
                        log.warn("[SourceProcessing] OpenAI file upload failed for source: {}. Error: {}", sourceId, e.getMessage());
                        // (참고) 업로드에 실패해도 텍스트 추출은 성공했으므로,
                        // 트랜잭션을 롤백하지 않고 계속 진행합니다.
                    }
                    // --- ▲▲▲ (추가된 로직) ▲▲▲ ---

                } else {
                    textContent = "[파일을 찾을 수 없음: " + localPath + "]";
                    log.warn("[SourceProcessing] File not found at path: {}", localPath);
                }
            }

            if (textContent.length() > 5000) {
                textContent = textContent.substring(0, 5000);
            }

            source.setProcessedTextContent(textContent);
            log.info("[SourceProcessing] Successfully processed text for source: {}", sourceId);

        } catch (Exception e) {
            log.error("[SourceProcessing] Failed to process source: " + sourceId, e);
            source.setProcessedTextContent("[텍스트 추출 실패: " + e.getMessage() + "]");
        }
        // @Transactional이 종료되면서 source 객체가 DB에 자동 저장 (save 호출 불필요)
    }

    private String cleanExtractedText(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("[^a-zA-Z0-9가-힣\\s]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
}
