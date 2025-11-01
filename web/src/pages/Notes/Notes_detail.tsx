import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import "../../css/Notes/Notes_detail.css";
import NoteEditor from "./NoteEditor";

//type Section = { h2: string; body: string };

const API_BASE = "http://localhost:8080";

export default function NoteDetail() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    // 제목 / 본문 HTML / 섹션(요약은 일단 유지하지만 버튼 제거)
    const [title] = useState<string>("");
    const [html, setHtml] = useState<string>("");
    //const [sections, setSections] = useState<Section[]>([]);
    const [saving, setSaving] = useState<"idle" | "saving" | "saved">("idle");
    const lastSavedAtRef = useRef<string>("");

    // ───────────────────────────────────────────────────────────────
    // 초기 로드: 백엔드에서 노트 단건을 받아오고(가능하면) 없으면 로컬스토리지 fallback
    // ───────────────────────────────────────────────────────────────
    useEffect(() => {
        if (!id) return;
        (async () => {
            try {
                const r = await fetch(`${API_BASE}/api/notes/${id}/sources`);
                if (r.ok) {
                    const list = await r.json(); // [{name, path}]
                    setSources(list);
                } else {
                    setSources([]); // 없으면 빈 목록
                }
            } catch {
                setSources([]);
            }
        })();
    }, [id]);

    // const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = async (e) => {
    //     const input = e.currentTarget;
    //     const files = Array.from(input.files ?? []);
    //     if (!files.length || !id) return;
    //
    //     try {
    //         const form = new FormData();
    //         form.append("file", files[0]);
    //         const res = await fetch(`${API_BASE}/api/notes/${id}/sources/file`, { method: "POST", body: form });
    //         if (!res.ok) throw new Error();
    //         const data: Uploaded = await res.json(); // {name, path}
    //         setSources(prev => [data, ...prev]);     // 서버 저장된 경로 반영
    //     } catch {
    //         alert("파일 업로드 실패");
    //     } finally {
    //         input.value = "";
    //     }
    // };

    // ───────────────────────────────────────────────────────────────
    // Ctrl/Cmd + S 저장 (버튼 없이)
    // ───────────────────────────────────────────────────────────────
    useEffect(() => {
        const onKeyDown = (e: KeyboardEvent) => {
            const isSave = (e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "s";
            if (!isSave) return;
            e.preventDefault();
            void handleSave();
        };
        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);
    }, [id, title, html]);

    const handleSave = async () => {
        if (!id) return;
        setSaving("saving");

        // 1) 로컬 초안 저장(항상)
        localStorage.setItem(`note:${id}:title`, title.trim());
        localStorage.setItem(`note:${id}:content`, html);

        // 2) 백엔드가 있으면 시도(실패해도 무시)
        try {
            await fetch(`${API_BASE}/api/notes/${id}/content`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ title: title.trim(), html }),
            });
        } catch {
            /* 서버 없으면 로컬만 유지 */
        }

        setSaving("saved");
        lastSavedAtRef.current = new Date().toLocaleTimeString();
        setTimeout(() => setSaving("idle"), 1200);
    };

    type Uploaded = {
        name: string;
        path?: string;     // 로컬 미리보기/가짜 경로
        ext?: string;
    };

    const [sources, setSources] = useState<Uploaded[]>([]);
    const fileRef = useRef<HTMLInputElement>(null);
    const openPicker = () => fileRef.current?.click();

// 허용 확장자(필요시 추가)
//     const ALLOW = [
//         "pdf","ppt","pptx","doc","docx","xls","xlsx","csv","txt","md"
//     ];

    // const iconFor = (ext: string) => {
    //     const e = ext.toLowerCase();
    //     if (["ppt","pptx"].includes(e)) return "📊";
    //     if (["doc","docx"].includes(e)) return "📝";
    //     if (["xls","xlsx","csv"].includes(e)) return "📈";
    //     if (e === "pdf") return "📄";
    //     if (["txt","md"].includes(e)) return "📃";
    //     return "📁";
    // };

    const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = async (e) => {
        const input = e.currentTarget;
        const files = Array.from(input.files ?? []);
        if (!files.length || !id) return;

        try {
            const form = new FormData();
            form.append("file", files[0]);

            const res = await fetch(`${API_BASE}/api/notes/${id}/sources/file`, {
                method: "POST",
                body: form,
            });
            if (!res.ok) throw new Error(`업로드 실패: ${res.status}`);

            const data = await res.json().catch(() => ({}));
            setSources((prev) => [{ name: files[0].name, path: data.path }, ...prev]);
        } catch (err) {
            console.error(err);
            alert("파일 업로드 중 오류가 발생했습니다.");
        } finally {
            input.value = "";
        }
    };

    return (
        <div className="note-detail">
            <header className="nd-header">
                <button className="nd-back" onClick={() => navigate("/notes")}>← 목록</button>
                <h2 className="nd-logo">AI NoteBook</h2>

                <div className="nd-actions">
                    {/* 저장 상태 미니 인디케이터 */}
                    {saving === "saving" && <span className="nd-badge">저장중…</span>}
                    {saving === "saved" && (
                        <span className="nd-badge nd-badge-ok">
              저장됨 {lastSavedAtRef.current && `(${lastSavedAtRef.current})`}
            </span>
                    )}
                    {saving === "idle" && <span className="nd-badge nd-badge-dim">Ctrl+S 저장</span>}
                </div>
            </header>

            <div className="nd-grid">
                {/* 좌측: 소스/영상 (기존 그대로) */}
                <aside className="nd-side">
                    <div className="nd-panel">
                        <div className="nd-panel-title">
                            <span>소스</span>
                            <button className="nd-add-btn" onClick={openPicker} title="파일 추가">+</button>
                            <input
                                ref={fileRef}
                                type="file"
                                hidden
                                multiple
                                accept="
          application/pdf,
          application/vnd.openxmlformats-officedocument.presentationml.presentation,
          application/vnd.ms-powerpoint,
          application/vnd.openxmlformats-officedocument.wordprocessingml.document,
          application/msword,
          application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,
          application/vnd.ms-excel,
          text/csv,
          text/plain,
          text/markdown
        "
                                onChange={onPickFiles}
                            />
                        </div>

                        {sources.length === 0 ? (
                            <div className="nd-source-empty">소스 없음</div>
                        ) : (
                            <ul className="nd-list">
                                {sources.map((s) => (
                                    <li key={(s.path ?? s.name) + Math.random()} className="nd-list-item">
                                        {s.name}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>

                    <br />

                    <div className="nd-panel">
                        <div className="nd-panel-title">영상</div>
                        <div className="nd-video-empty">영상 소스 없음</div>
                    </div>
                </aside>

                {/* 가운데: 본문 (제목 h1 + TipTap 에디터) */}
                <main className="nd-main">
                    <div className="nd-main-panel">

                        <div className="nd-section">
                            {/* 에디터 변경사항은 html state로 올라옴 → Ctrl+S로 저장 */}
                            {id && (
                                <NoteEditor
                                    noteId={id}
                                    initial={html}
                                    onChange={(h) => setHtml(h)}
                                />
                            )}
                        </div>
                    </div>
                </main>

                {/* 우측: AI 채팅 패널 (그대로) */}
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