// VideoList.tsx (전체 코드)

// ▼▼▼ [추가됨] useState 임포트 ▼▼▼
import { useState } from "react";
import youtubeIcon from "../../../assets/icons/youtube.png";
import "../../../css/Notes/components/VideoList.css";

export type VideoItem = {
    id: string;
    title: string;
    url: string;
    score?: number;
    reason?: string;
    targetLevel?: string;
};

type VideoListProps = {
    videos: VideoItem[];
    onClickAdd?: () => void;
    recommending?: boolean;
    onAnalyze?: (video: VideoItem) => void;
    analyzingVideoId?: string | null;

    // ▼▼▼ [추가됨] 핵심단어 검색용 props ▼▼▼
    onClickKeywordAdd?: (keyword: string) => void;
    recommendingKeyword?: boolean;
    // ▲▲▲ [추가됨] ▲▲▲
};

export default function VideoList({
                                      videos,
                                      onClickAdd,
                                      recommending = false,
                                      onAnalyze,
                                      analyzingVideoId,

                                      // ▼▼▼ [추가됨] props 받기 ▼▼▼
                                      onClickKeywordAdd,
                                      recommendingKeyword = false,
                                      // ▲▲▲ [추가됨] ▲▲▲
                                  }: VideoListProps) {

    // ▼▼▼ [추가됨] 핵심단어 입력창을 위한 state ▼▼▼
    const [keywordInput, setKeywordInput] = useState("");

    const handleKeywordAdd = () => {
        if (onClickKeywordAdd && keywordInput.trim()) {
            onClickKeywordAdd(keywordInput.trim());
            setKeywordInput(""); // 검색 후 입력창 비우기
        }
    };
    // ▲▲▲ [추가됨] ▲▲▲

    return (
        <div className="nd-panel">
            <div className="nd-panel-title">
                <span>영상</span>

                {/* ▼▼▼ [수정됨] 핵심단어 검색 UI 추가 ▼▼▼ */}
                <div className="nd-keyword-search">
                    <input
                        type="text"
                        placeholder="핵심단어 입력..."
                        value={keywordInput}
                        onChange={(e) => setKeywordInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') handleKeywordAdd(); }}
                        disabled={recommendingKeyword}
                    />
                    <button
                        className="nd-add-btn keyword" // 새 클래스
                        onClick={handleKeywordAdd}
                        title="핵심단어로 영상 추천"
                        disabled={recommendingKeyword || !keywordInput.trim()}
                    >
                        {recommendingKeyword ? "..." : "검색"}
                    </button>
                </div>
                {/* ▲▲▲ [수정됨] ▲▲▲ */}

                {/* (기존) 소스 기반 추천 버튼 */}
                {onClickAdd && (
                    <button
                        className="nd-add-btn recommend"
                        onClick={onClickAdd}
                        title="소스 기반 영상 추천받기"
                        disabled={recommending || recommendingKeyword} // ★ 키워드 검색 중에도 비활성화
                    >
                        {recommending ? "..." : "추천"}
                    </button>
                )}
            </div>

            <div className="nd-panel-body">
                {/* ... (이하 동일) ... */}
                {videos.length === 0 ? (
                    <div className="nd-video-empty" style={{display:'grid',placeItems:'center',gap:8,padding:'10px 0',color:'#6b7280'}}>
                        <img src={youtubeIcon} alt="youtube" style={{ width:28, height:28, objectFit:'contain' }} />
                        <div>추천된 영상 없음</div>
                    </div>
                ) : (
                    <ul className="nd-list">
                        {videos.map(v => {
                            const isAnalyzing = analyzingVideoId === v.id;
                            const tooltip = `[추천 점수: ${v.score?.toFixed(2) ?? 'N/A'}]\n${v.reason ?? 'N/A'}`;

                            return (
                                <li key={v.id} className="nd-list-item video">
                                    <img className="nd-list-icon" src={youtubeIcon} alt="" />
                                    <div className="nd-list-main" title={tooltip}>
                                        <span
                                            className="nd-list-text"
                                            onClick={() => window.open(v.url, "_blank")}
                                        >
                                            {v.title}
                                        </span>
                                        {v.reason && (
                                            <div className="nd-video-reason">
                                                {v.targetLevel && v.targetLevel !== "N/A" && (
                                                    <span className="nd-video-badge">{v.targetLevel}</span>
                                                )}
                                                <span>{v.reason}</span>
                                            </div>
                                        )}
                                    </div>
                                    {onAnalyze && (
                                        <button
                                            className="nd-analyze-btn"
                                            onClick={() => onAnalyze(v)}
                                            disabled={isAnalyzing || !!analyzingVideoId}
                                            title="영상 분석/요약하기"
                                        >
                                            {isAnalyzing ? "..." : "분석"}
                                        </button>
                                    )}
                                </li>
                            );
                        })}
                    </ul>
                )}
            </div>
        </div>
    );
}