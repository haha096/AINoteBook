import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import "../../css/Notes/Notes_detail.css";
import NoteEditor from "./NoteEditor";

const API_BASE = "http://localhost:8080";

//AI와 자료 업로드를 수정한 프로젝트
//upload_11_10_v2보다 나중에 만들었음

/** 서버 반환 모델 (DB 기준) */
type SourceRow = {
    id: number;
    type: "FILE" | "URL" | "NOTION";
    name: string;
    value: string;           // 서버 로컬 경로 문자열
    openaiFileId?: string;   // "file-xxxx" (있으면 질문에 사용)
};


export default function NoteDetail() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    // 제목/HTML 초안 저장(현 UI 유지)
    const [title] = useState<string>("");
    const [html, setHtml] = useState<string>("");
    const [saving, setSaving] = useState<"idle" | "saving" | "saved">("idle");
    const lastSavedAtRef = useRef<string>("");

    // 좌측: 소스 리스트 (UI 그대로)
    const [sources, setSources] = useState<SourceRow[]>([]);
    const fileRef = useRef<HTMLInputElement>(null);
    const openPicker = () => fileRef.current?.click();

    // 우측: 채팅(UI 그대로—빈 영역에 메시지만 채움)
    const [chatInput, setChatInput] = useState("");
    const [messages, setMessages] = useState<{ role: "user" | "assistant"; text: string }[]>([]);


    // ───────────────────────────────────────────────────────────────
    // API helpers
    // ───────────────────────────────────────────────────────────────
    async function fetchSources(noteId: string): Promise<SourceRow[]> {
        const r = await fetch(`${API_BASE}/api/notes/${noteId}/sources`, { credentials: "include" });
        if (!r.ok) return [];
        return r.json();
    }

    async function uploadSourceFile(noteId: string, file: File): Promise<SourceRow> {
        const fd = new FormData();
        fd.append("file", file);
        const r = await fetch(`${API_BASE}/api/notes/${noteId}/sources/file`, {
            method: "POST",
            body: fd,
            credentials: "include",
        });
        if (!r.ok) throw new Error(`업로드 실패: ${r.status}`);
        return r.json();
    }

    async function askWithFiles(noteId: string, q: string): Promise<{ answer: string; fileCount: number }> {
        const r = await fetch(`${API_BASE}/api/notes/${noteId}/ask?q=${encodeURIComponent(q)}`, {
            method: "POST",
            credentials: "include",
        });
        if (!r.ok) throw new Error(`질문 실패: ${r.status}`);
        return r.json();
    }

    // ───────────────────────────────────────────────────────────────
    // 초기 로드: 소스 목록만 DB 기준으로 읽기 (UI 변경 없음)
    // ───────────────────────────────────────────────────────────────
    useEffect(() => {
        if (!id) return;
        (async () => {
            try {
                const list = await fetchSources(id);
                setSources(list);
            } catch {
                setSources([]);
            }
        })();
    }, [id]);

    // ───────────────────────────────────────────────────────────────
    // Ctrl/Cmd + S 저장 (UI 문구 그대로)
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

        // 1) 로컬 초안
        localStorage.setItem(`note:${id}:title`, title.trim());
        localStorage.setItem(`note:${id}:content`, html);

        // 2) 백엔드 저장 시도(없는 API여도 에러 무시)
        try {
            await fetch(`${API_BASE}/api/notes/${id}/content`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ title: title.trim(), html }),
            });
        } catch {}

        setSaving("saved");
        lastSavedAtRef.current = new Date().toLocaleTimeString();
        setTimeout(() => setSaving("idle"), 1200);
    };


    // 업로드 상태
    const [uploading, setUploading] = useState(false);

// 파일 선택 → 업로드 → DB 반영
    const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = async (e) => {
        const input = e.currentTarget;
        const files = Array.from(input.files ?? []);
        if (!files.length || !id) return;

        try {
            setUploading(true);
            const created = await uploadSourceFile(id, files[0]);
            setSources((prev) => [created, ...prev]);
        } catch (err) {
            console.error(err);
            alert("파일 업로드 중 오류가 발생했습니다.");
        } finally {
            setUploading(false);
            input.value = "";
        }
    };

// 질문 상태
    const [asking, setAsking] = useState(false);

// 채팅 전송 → 첨부파일 기반 질문
    const sendChat = async () => {
        if (!id || !chatInput.trim() || asking) return;
        const q = chatInput.trim();
        setChatInput("");
        setMessages((prev) => [...prev, { role: "user", text: q }]);

        try {
            setAsking(true);
            const { answer, fileCount } = await askWithFiles(id, q);
            setMessages((prev) => [
                ...prev,
                { role: "assistant", text: `(첨부파일 ${fileCount}개 사용)\n${answer}` },
            ]);
        } catch (e: any) {
            setMessages((prev) => [...prev, { role: "assistant", text: `에러: ${e?.message ?? "요청 실패"}` }]);
        } finally {
            setAsking(false);
        }
    };

    useEffect(() => {
        const el = document.getElementById("cgpt-scroll");
        if (el) el.scrollTop = el.scrollHeight;
    }, [messages, asking]);


    //채팅방 UI
    function ChatBubble({ role, text }: { role: "user" | "assistant"; text: string }) {
        const isUser = role === "user";
        const copy = async () => {
            try {
                await navigator.clipboard.writeText(text);
            } catch {}
        };

        return (
            <div className={`cgpt-row ${isUser ? "user" : "ai"}`}>
                <div className="cgpt-avatar">{isUser ? "🧑" : "🤖"}</div>
                <div className={`cgpt-bubble ${isUser ? "user" : "ai"}`}>
                    {text.split("\n").map((line, i) => <div key={i}>{line}</div>)}

                    {/* AI 말풍선에서만 Copy 버튼 표시 */}
                    {!isUser && (
                        <button className="cgpt-copy" onClick={copy} title="복사">
                            Copy
                        </button>
                    )}
                </div>
            </div>
        );
    }


    return (
        <div className="note-detail">
            <header className="nd-header">
                <button className="nd-back" onClick={() => navigate("/notes")}>← 목록</button>
                <h2 className="nd-logo">AI NoteBook</h2>

                <div className="nd-actions">
                    {uploading && <span className="nd-badge">업로드중…</span>}
                    {saving === "saving" && <span className="nd-badge">저장중…</span>}
                    {saving === "saved" && <span className="nd-badge nd-badge-ok">저장됨 {lastSavedAtRef.current && `(${lastSavedAtRef.current})`}</span>}
                    {saving === "idle" && !uploading && <span className="nd-badge nd-badge-dim">Ctrl+S 저장</span>}
                </div>
            </header>

            <div className="nd-grid">
                {/* 좌측: 소스/영상 (UI 동일) */}
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
                                    <li key={s.id} className="nd-list-item">
                                        {s.type === "FILE" ? (
                                            <a href={s.value} target="_blank" rel="noreferrer">{s.name}</a>
                                        ) : (
                                            s.name
                                        )}
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

                {/* 가운데: 본문 (UI 동일) */}
                <main className="nd-main">
                    <div className="nd-main-panel">
                        <div className="nd-section">
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

                {/* 우측: AI 채팅 패널 (UI 동일) */}
                <aside className="nd-chat">
                    <div className="cgpt-chat">
                        {/* 헤더 */}
                        <div className="cgpt-header">
                            <div className="cgpt-title">AI 도우미</div>
                            <div className="cgpt-sub">첨부파일 기반 Q&A</div>
                        </div>

                        {/* 메시지 영역 */}
                        <div className="cgpt-scroll" id="cgpt-scroll">
                            {messages.length === 0 ? (
                                <div className="cgpt-empty">
                                    <div>안녕하세요! 오른쪽 아래에 질문을 입력해보세요.</div>
                                    <div className="cgpt-hint">예) “lec02를 5줄로 요약해줘”</div>
                                </div>
                            ) : (
                                messages.map((m, i) => (
                                    <ChatBubble key={i} role={m.role} text={m.text} />
                                ))
                            )}
                            {asking && (
                                <div className="cgpt-row ai">
                                    <div className="cgpt-avatar">🤖</div>
                                    <div className="cgpt-bubble ai">
            <span className="cgpt-typing">
              <span className="dot" />
              <span className="dot" />
              <span className="dot" />
            </span>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* 입력 바 */}
                        <div className="cgpt-inputbar">
                            <input
                                className="cgpt-input"
                                placeholder="무엇이든 물어보세요…"
                                value={chatInput}
                                onChange={(e) => setChatInput(e.target.value)}
                                onKeyDown={(e) => { if (e.key === "Enter") sendChat(); }}
                            />
                            <button
                                className="cgpt-send"
                                onClick={sendChat}
                                disabled={asking || !chatInput.trim()}
                                title="보내기"
                            >
                                {asking ? "…" : "보내기"}
                            </button>
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    );
}