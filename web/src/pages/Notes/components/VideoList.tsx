// 🔴 VideoList.tsx (수정)

import youtubeIcon from "../../../assets/icons/youtube.png";
import "../../../css/Notes/components/VideoList.css";

export type VideoItem = {
    id: string;
    title: string;
    url: string; // YouTube URL
};

// ▼ Props 확장
type VideoListProps = {
    videos: VideoItem[];
    onClickAdd?: () => void;
    recommending?: boolean;        // [기능 2] 추천 로딩 상태
    onAnalyze?: (video: VideoItem) => void; // [기능 3] 분석 핸들러
    analyzingVideoId?: string | null; // [기능 3] 분석 중인 영상 ID
};

export default function VideoList({
                                      videos,
                                      onClickAdd,
                                      recommending = false,
                                      onAnalyze,
                                      analyzingVideoId,
                                  }: VideoListProps) {
    return (
        <div className="nd-panel">
            <div className="nd-panel-title">
                <span>영상</span>
                {onClickAdd && (
                    // ▼ [기능 2] 버튼 수정
                    <button
                        className="nd-add-btn recommend" // 클래스 추가
                        onClick={onClickAdd}
                        title="소스 기반 영상 추천받기"
                        disabled={recommending} // 로딩 중 비활성화
                    >
                        {recommending ? "..." : "추천"}
                    </button>
                )}
            </div>

            {/* ▼ 목록만 스크롤 */}
            <div className="nd-panel-body">
                {videos.length === 0 ? (
                    <div className="nd-video-empty" style={{display:'grid',placeItems:'center',gap:8,padding:'10px 0',color:'#6b7280'}}>
                        <img src={youtubeIcon} alt="youtube" style={{ width:28, height:28, objectFit:'contain' }} />
                        {/* ▼ 비었을 때 메시지 수정 */}
                        <div>추천된 영상 없음</div>
                    </div>
                ) : (
                    <ul className="nd-list">
                        {videos.map(v => {
                            const isAnalyzing = analyzingVideoId === v.id;
                            return (
                                <li key={v.id} className="nd-list-item video"> {/* video 클래스 추가 */}
                                    <img className="nd-list-icon" src={youtubeIcon} alt="" />

                                    {/* ▼ 제목 클릭 시 새 탭에서 열기 */}
                                    <span
                                        className="nd-list-text"
                                        onClick={() => window.open(v.url, "_blank")}
                                        title="영상 시청하기"
                                    >
                                        {v.title}
                                    </span>

                                    {/* ▼ [기능 3] 분석 버튼 추가 */}
                                    {onAnalyze && (
                                        <button
                                            className="nd-analyze-btn"
                                            onClick={() => onAnalyze(v)}
                                            disabled={isAnalyzing || !!analyzingVideoId} // 분석 중이거나 다른 영상 분석 중일 때 비활성화
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
