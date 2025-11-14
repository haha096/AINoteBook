package com.example.ai_notebook.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Caption;
import com.google.api.services.youtube.model.CaptionListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

@Component
public class YoutubeApiClient {

    private final YouTube youtubeService;
    private final String apiKey;

    private static final String APPLICATION_NAME = "AI-Notebook";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public YoutubeApiClient(@Value("${youtube.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.youtubeService = buildYoutubeService();
    }

    private YouTube buildYoutubeService() {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            return new YouTube.Builder(httpTransport, JSON_FACTORY, null)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("YouTube 서비스 초기화에 실패했습니다.", e);
        }
    }

    /**
     * 영상 ID로 영상 제목(Title)을 가져옵니다.
     */
    public String getVideoTitle(String videoId) throws Exception {
        YouTube.Videos.List request = youtubeService.videos()
                .list(Collections.singletonList("snippet")) // <--- 이렇게 수정
                .setId(Collections.singletonList(videoId))
                .setKey(apiKey);

        VideoListResponse response = request.execute();
        List<Video> items = response.getItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("영상을 찾을 수 없습니다: " + videoId);
        }
        return items.get(0).getSnippet().getTitle();
    }

    /**
     * 영상 ID로 스크립트(자막) 텍스트를 가져옵니다.
     */
    public String getTranscript(String videoId) throws Exception {

        // 1. 영상에 사용 가능한 자막 목록(Caption List) 가져오기
        YouTube.Captions.List captionRequest = youtubeService.captions()
                .list(Collections.singletonList("snippet"), videoId)
                .setKey(apiKey);
        CaptionListResponse captionListResponse = captionRequest.execute();
        List<Caption> captions = captionListResponse.getItems();

        if (captions == null || captions.isEmpty()) {
            throw new RuntimeException("이 영상에는 사용 가능한 자막(스크립트)이 없습니다.");
        }

        // 2. 사용 가능한 자막 중 '한국어' 또는 '영어' 자동 생성 자막(asr)이나 표준 자막을 찾습니다.
        String captionTrackId = null;
        for (Caption caption : captions) {
            String lang = caption.getSnippet().getLanguage();
            String kind = caption.getSnippet().getTrackKind();
            // (선호도: 한국어 > 영어 > 기타)
            if (lang.equals("ko") && (kind.equals("standard") || kind.equals("ASR"))) {
                captionTrackId = caption.getId();
                break;
            } else if (lang.equals("en") && (kind.equals("standard") || kind.equals("ASR"))) {
                captionTrackId = caption.getId();
                // (영어를 찾았지만 한국어가 더 있는지 계속 찾아봄)
            }
        }

        if (captionTrackId == null && !captions.isEmpty()) {
            // 정 못찾겠으면 그냥 첫번째 트랙이라도 사용
            captionTrackId = captions.get(0).getId();
        }

        if (captionTrackId == null) {
            throw new RuntimeException("유효한 자막 트랙을 찾지 못했습니다.");
        }

        // 3. 자막 트랙 다운로드 요청 (SBV 포맷으로 요청)
        YouTube.Captions.Download downloadRequest = youtubeService.captions()
                .download(captionTrackId)
                .setTfmt("sbv") // SBV 포맷이 텍스트 파싱하기 가장 좋습니다.
                .setKey(apiKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        downloadRequest.executeMediaAndDownloadTo(outputStream);

        // 4. SBV 포맷을 순수 텍스트로 파싱 (타임스탬프 제거)
        return parseSbvToPlainText(outputStream.toString());
    }

    /**
     * SBV 자막 포맷에서 타임스탬프를 제거하고 순수 텍스트만 반환합니다.
     * (예: "0:00:01.000,0:00:03.000\n안녕하세요" -> "안녕하세요")
     */
    private String parseSbvToPlainText(String sbvContent) {
        StringBuilder plainText = new StringBuilder();
        String[] lines = sbvContent.split("\n");
        for (String line : lines) {
            // 타임스탬프 라인(예: 0:00:01.000,...)이나 빈 줄은 건너뜁니다.
            if (line.matches("^\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}.*") || line.trim().isEmpty()) {
                continue;
            }
            plainText.append(line.trim()).append(" ");
        }
        return plainText.toString();
    }
}
