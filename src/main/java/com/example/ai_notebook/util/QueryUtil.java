package com.example.ai_notebook.util;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryUtil {
    // 한글/영문/숫자 단어만 남기고, 자주 나오는 의미없는 단어 제거
    private static final Pattern TOKENIZER = Pattern.compile("[^0-9A-Za-z가-힣]+");
    private static final Set<String> STOP = Set.of(
            "the","and","for","with","from","this","that","you","your","are","can","how",
            "use","file","files","data","pdf","ppt","doc","docs","docx","pptx","내용","섹션",
            "요약","분석","정리","자료","예제","수업","강의","과제","프로젝트","노트","소스"
    );

    /** 파일명/본문에서 상위 키워드 3~6개 뽑기 */
    public static String extractCompactQuery(String title, String filenamesCsv, String body, int maxTerms) {
        String base = String.join(" ",
                nullToEmpty(title),
                nullToEmpty(filenamesCsv).replaceAll("\\.[A-Za-z0-9]{2,4}", " "),
                nullToEmpty(body)
        );

        Map<String, Integer> freq = new HashMap<>();
        for (String raw : TOKENIZER.split(base.toLowerCase())) {
            if (raw.length() < 2) continue;
            if (STOP.contains(raw)) continue;
            // 숫자만/한 글자 무시
            if (raw.chars().allMatch(Character::isDigit)) continue;
            freq.merge(raw, 1, Integer::sum);
        }
        // 길이/빈도 가중치로 상위 N개
        var top = freq.entrySet().stream()
                .sorted((a,b)->{
                    int f = Integer.compare(b.getValue(), a.getValue());
                    if (f!=0) return f;
                    return Integer.compare(b.getKey().length(), a.getKey().length());
                })
                .limit(Math.max(3, Math.min(maxTerms, 6)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return String.join(" ", top);
    }

    private static String nullToEmpty(String s){ return s==null? "" : s; }
}
