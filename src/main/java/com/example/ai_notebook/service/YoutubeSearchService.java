package com.example.ai_notebook.service;


import com.example.ai_notebook.dto.RecoItemDTO;
import com.example.ai_notebook.util.QueryUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeSearchService {

    @Value("${youtube.apiKey}")         private String apiKey;
    @Value("${youtube.regionCode:KR}")  private String regionCode;
    @Value("${youtube.relevanceLanguage:ko}") private String lang;

    private final ObjectMapper om = new ObjectMapper();

    // ★★★ QueryUtil 대신 Gemini가 만든 smartQuery를 받도록 수정 ★★★
    public List<RecoItemDTO> searchTopSmart(String smartQuery, int max) {

        String q = smartQuery;
        log.info("[Debug] YouTube Search Query (from AI): [{}]", q);
        log.info("[Debug] YouTube Search Query (from Gemini): [{}]", q);

        try {
            // 1) search.list
            URI uri = UriComponentsBuilder.fromUriString("https://www.googleapis.com/youtube/v3/search")
                    .queryParam("key", apiKey)
                    .queryParam("part", "snippet")
                    .queryParam("type", "video")
                    .queryParam("order", "relevance")
                    .queryParam("regionCode", regionCode)
                    .queryParam("relevanceLanguage", lang)
                    .queryParam("videoDuration", "medium")
                    //.queryParam("videoCaption", "closedCaption") // (주석 처리 유지)
                    .queryParam("maxResults", Math.min(max, 15))
                    .queryParam("q", q)
                    .build().toUri();

            HttpClient hc = HttpClient.newHttpClient();
            HttpResponse<String> res = hc.send(
                    HttpRequest.newBuilder(uri).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            JsonNode root = om.readTree(res.body());
            List<String> ids = new ArrayList<>();
            for (JsonNode it : root.path("items")) {
                String id = it.path("id").path("videoId").asText("");
                if (!id.isBlank()) ids.add(id);
            }

            log.info("[Debug] YouTube API Raw Result Count: {}", ids.size());

            if (ids.isEmpty()) return List.of();

            // 2) videos.list — 상세 조회
            URI vUri = UriComponentsBuilder.fromUriString("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("key", apiKey)
                    .queryParam("part", "snippet,contentDetails,statistics")
                    .queryParam("id", String.join(",", ids))
                    .build().toUri();
            HttpResponse<String> vRes = hc.send(
                    HttpRequest.newBuilder(vUri).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            JsonNode vRoot = om.readTree(vRes.body());

            List<RecoItemDTO> all = new ArrayList<>();
            for (JsonNode v : vRoot.path("items")) {
                String id = v.path("id").asText("");
                JsonNode sn = v.path("snippet");
                JsonNode cd = v.path("contentDetails");
                JsonNode st = v.path("statistics");

                int dur = isoDurationToSec(cd.path("duration").asText("PT0S"));
                if (dur < 60 || dur > 7200) continue;

                String titleStr   = sn.path("title").asText("");
                String channelStr = sn.path("channelTitle").asText("");
                String audioLang  = sn.path("defaultAudioLanguage").asText("");
                boolean looksKorean = isKorean(titleStr) || isKorean(channelStr) || audioLang.startsWith("ko");

                all.add(RecoItemDTO.builder()
                        .id(id)
                        .title(titleStr)
                        .url("https://www.youtube.com/watch?v="+id)
                        .channelTitle(channelStr)
                        .thumbnailUrl(
                                sn.path("thumbnails").path("medium").path("url").asText(
                                        sn.path("thumbnails").path("default").path("url").asText("")
                                )
                        )
                        .publishedAt(sn.path("publishedAt").asText(""))
                        .durationSec(dur)
                        .viewCount(st.has("viewCount") ? st.path("viewCount").asLong() : null)
                        .score(looksKorean ? 0.1 : 0.0)
                        .build());
            }

            log.info("[Debug] YouTube Filtered (by duration) Result Count: {}", all.size());

            // 3) 한국어 우선 필터
            List<RecoItemDTO> krOnly = all.stream().filter(v ->
                    isKorean(v.getTitle()) || isKorean(v.getChannelTitle())
            ).toList();

            List<RecoItemDTO> picked = (krOnly.size() >= 3) ? krOnly : all;
            return picked.stream().limit(max).toList();

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static boolean isKorean(String s) {
        if (s == null) return false;
        for (int i=0;i<s.length();i++){
            char c = s.charAt(i);
            if ((c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x1100 && c <= 0x11FF)) return true;
        }
        return false;
    }

    private static Integer isoDurationToSec(String iso) {
        int h=0,m=0,s=0; String num="";
        for (char c : iso.replace("PT","").toCharArray()){
            if (Character.isDigit(c)) num+=c;
            else { int v = num.isEmpty()?0:Integer.parseInt(num); num="";
                if (c=='H') h=v; else if (c=='M') m=v; else if (c=='S') s=v; }
        }
        return h*3600+m*60+s;
    }
}
