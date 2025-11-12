// VideoList.tsx (전체 코드)

import youtubeIcon from "../../../assets/icons/youtube.png";
import "../../../css/Notes/components/VideoList.css";

// ▼ [개선] VideoItem 타입을 백엔드 DTO(RecoItemDTO)에 맞게 확장
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
                    <button
                        className="nd-add-btn recommend"
                        onClick={onClickAdd}
                        title="소스 기반 영상 추천받기"
                        disabled={recommending}
                    >
                        {recommending ? "..." : "추천"}
                    </button>
                )}
            </div>

            <div className="nd-panel-body">
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

                                    {/* ▼ [개선] 제목과 추천 사유를 묶음 */}
                                    <div className="nd-list-main" title={tooltip}>
                                        <span
                                            className="nd-list-text"
                                            onClick={() => window.open(v.url, "_blank")}
                                        >
                                            {v.title}
                                        </span>

                                        {/* ▼ [개선] 추천 사유 및 배지 표시 */}
                                        {v.reason && (
                                            <div className="nd-video-reason">
                                                {v.targetLevel && v.targetLevel !== "N/A" && (
                                                    <span className="nd-video-badge">{v.targetLevel}</span>
                                                )}
                                                <span>{v.reason}</span>
                                            </div>
                                        )}
                                    </div>

                                    {/* ▼ [기능 3] 분석 버튼 (기존과 동일) */}
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

// // 🔴 VideoList.tsx (수정)
//
// import youtubeIcon from "../../../assets/icons/youtube.png";
// import "../../../css/Notes/components/VideoList.css";
//
// export type VideoItem = {
//     id: string;
//     title: string;
//     url: string; // YouTube URL
// };
//
// // ▼ Props 확장
// type VideoListProps = {
//     videos: VideoItem[];
//     onClickAdd?: () => void;
//     recommending?: boolean;        // [기능 2] 추천 로딩 상태
//     onAnalyze?: (video: VideoItem) => void; // [기능 3] 분석 핸들러
//     analyzingVideoId?: string | null; // [기능 3] 분석 중인 영상 ID
// };
//
// export default function VideoList({
//                                       videos,
//                                       onClickAdd,
//                                       recommending = false,
//                                       onAnalyze,
//                                       analyzingVideoId,
//                                   }: VideoListProps) {
//     return (
//         <div className="nd-panel">
//             <div className="nd-panel-title">
//                 <span>영상</span>
//                 {onClickAdd && (
//                     // ▼ [기능 2] 버튼 수정
//                     <button
//                         className="nd-add-btn recommend" // 클래스 추가
//                         onClick={onClickAdd}
//                         title="소스 기반 영상 추천받기"
//                         disabled={recommending} // 로딩 중 비활성화
//                     >
//                         {recommending ? "..." : "추천"}
//                     </button>
//                 )}
//             </div>
//
//             {/* ▼ 목록만 스크롤 */}
//             <div className="nd-panel-body">
//                 {videos.length === 0 ? (
//                     <div className="nd-video-empty" style={{display:'grid',placeItems:'center',gap:8,padding:'10px 0',color:'#6b7280'}}>
//                         <img src={youtubeIcon} alt="youtube" style={{ width:28, height:28, objectFit:'contain' }} />
//                         {/* ▼ 비었을 때 메시지 수정 */}
//                         <div>추천된 영상 없음</div>
//                     </div>
//                 ) : (
//                     <ul className="nd-list">
//                         {videos.map(v => {
//                             const isAnalyzing = analyzingVideoId === v.id;
//                             return (
//                                 <li key={v.id} className="nd-list-item video"> {/* video 클래스 추가 */}
//                                     <img className="nd-list-icon" src={youtubeIcon} alt="" />
//
//                                     {/* ▼ 제목 클릭 시 새 탭에서 열기 */}
//                                     <span
//                                         className="nd-list-text"
//                                         onClick={() => window.open(v.url, "_blank")}
//                                         title="영상 시청하기"
//                                     >
//                                         {v.title}
//                                     </span>
//
//                                     {/* ▼ [기능 3] 분석 버튼 추가 */}
//                                     {onAnalyze && (
//                                         <button
//                                             className="nd-analyze-btn"
//                                             onClick={() => onAnalyze(v)}
//                                             disabled={isAnalyzing || !!analyzingVideoId} // 분석 중이거나 다른 영상 분석 중일 때 비활성화
//                                             title="영상 분석/요약하기"
//                                         >
//                                             {isAnalyzing ? "..." : "분석"}
//                                         </button>
//                                     )}
//                                 </li>
//                             );
//                         })}
//                     </ul>
//                 )}
//             </div>
//         </div>
//     );
// }
