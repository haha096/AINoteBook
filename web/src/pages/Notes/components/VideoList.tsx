import youtubeIcon from "../../../assets/icons/youtube.png";

export type VideoItem = {
    id: string;
    title: string;
    url: string; // YouTube URL
};

export default function VideoList({ videos, onClickAdd }: { videos: VideoItem[]; onClickAdd?: () => void; }) {
    return (
        <div className="nd-panel">
            <div className="nd-panel-title">
                <span>영상</span>
                {onClickAdd && <button className="nd-add-btn" onClick={onClickAdd} title="영상 추가">+</button>}
            </div>

            {/* ▼ 목록만 스크롤 */}
            <div className="nd-panel-body">
                {videos.length === 0 ? (
                    <div className="nd-video-empty" style={{display:'grid',placeItems:'center',gap:8,padding:'10px 0',color:'#6b7280'}}>
                        <img src={youtubeIcon} alt="youtube" style={{ width:28, height:28, objectFit:'contain' }} />
                        <div>영상 소스 없음</div>
                    </div>
                ) : (
                    <ul className="nd-list">
                        {videos.map(v => (
                            <li key={v.id} className="nd-list-item" onClick={() => window.open(v.url, "_blank")} style={{ cursor:'pointer' }}>
                                <img className="nd-list-icon" src={youtubeIcon} alt="" />
                                <span className="nd-list-text">{v.title}</span>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}