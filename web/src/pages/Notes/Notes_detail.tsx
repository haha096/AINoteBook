import { useParams, useNavigate } from "react-router-dom";
import {useEffect, useRef, useState} from "react";
import "../../css/Notes/Notes_detail.css";

type Section = { h2: string; body: string };

const API_BASE = "http://localhost:8080";

export default function NoteDetail() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    // ✅ 제목/섹션/소스 — 최초엔 비어두고, 필요 시 채우자
    const [title] = useState("노트");
    const [sections, setSections] = useState<Section[]>([]);
    const [sources, setSources] = useState<string[]>([]);

    // 파일 추가용 input
    const fileInputRef = useRef<HTMLInputElement>(null);
    const openFilePicker = () => fileInputRef.current?.click();

    const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = async (e) => {
        const files = Array.from(e.target.files ?? []);
        if (!files.length || !id) return;

        const form = new FormData();
        form.append("file", files[0]);

        try {
            const res = await fetch(`${API_BASE}/api/notes/${id}/sources/file`, {
                method: "POST",
                body: form,
            });
            if (!res.ok) throw new Error("업로드 실패");
            // 성공하면 UI에만 반영
            setSources((prev) => [...prev, files[0].name]);
            e.target.value = "";
        } catch (err) {
            console.error(err);
            alert("파일 업로드 중 오류가 발생했습니다.");
        }
    };

    // ✅ 요약 버튼 → 서버 summarize 호출 → 섹션 갱신
    const runSummarize = async () => {
        if (!id) return;
        try {
            const res = await fetch(`${API_BASE}/api/notes/${id}/summarize`, {
                method: "POST",
            });
            if (!res.ok) throw new Error("요약 실패");
            const data = await res.json(); // { sections: [{h2, body}, ...] }
            setSections(data.sections ?? []);
        } catch (e) {
            console.error(e);
            alert("요약 생성 중 오류가 발생했습니다.");
        }
    };

    // ✅ (선택) 목록에서 넘어올 때 제목을 전달받았다면 반영
    //    지금은 서버에서 note 단건 조회 API가 없으니 임시로 기본값 유지
    useEffect(() => {
        if (!id) navigate("/notes", { replace: true });
    }, [id, navigate]);

    return (
        <div className="note-detail">
            <header className="nd-header">
                <button className="nd-back" onClick={() => navigate("/notes")}>← 목록</button>
                <h2 className="nd-logo">AI NoteBook</h2>
                <div className="nd-actions" />
            </header>

            <div className="nd-grid">
                {/* 좌측: 소스/영상 */}
                <aside className="nd-side">
                    <div className="nd-panel">
                        <div className="nd-panel-title">
                            <span>소스</span>
                            <button className="nd-add-btn" onClick={openFilePicker} aria-label="소스 추가">+</button>
                            <input
                                ref={fileInputRef}
                                type="file"
                                multiple
                                hidden
                                onChange={onPickFiles}
                            />
                        </div>

                        <ul className="nd-list">
                            {sources.map((s) => (
                                <li key={s} className="nd-list-item" title={s}>{s}</li>
                            ))}
                        </ul>
                    </div>

                    <br />

                    <div className="nd-panel">
                        <div className="nd-panel-title">영상</div>
                        <div className="nd-video-empty">영상 소스 없음</div>
                    </div>
                </aside>

                {/* 가운데: 본문 */}
                <main className="nd-main">
                    <div className="nd-main-panel">
                        <div className="nd-title-row">
                            <h2 className="nd-title">{title}</h2>
                            <button className="nd-run-btn" onClick={runSummarize}>요약 만들기</button>
                        </div>

                        {sections.length === 0 ? (
                            <section className="nd-section">
                                <p className="nd-paragraph">소스를 추가하고 “요약 만들기”를 눌러보세요.</p>
                            </section>
                        ) : (
                            sections.map((sec, idx) => (
                                <section className="nd-section" key={idx}>
                                    <h3 className="nd-h2">{sec.h2}</h3>
                                    <p className="nd-paragraph">{sec.body}</p>
                                </section>
                            ))
                        )}
                    </div>
                </main>

                {/* 우측: AI 채팅 — 그대로 */}
                <aside className="nd-chat">
                    <div className="nd-chat-panel">
                        <div className="nd-chat-scroll">
                            <div className="nd-chat-empty">AI와 노트필기를 시작해보세요</div>
                        </div>
                        <div className="nd-chat-input">
                            <input placeholder="입력을 시작하세요" />
                            <button>보내기</button>
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    );
}