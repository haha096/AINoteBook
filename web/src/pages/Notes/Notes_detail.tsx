// Notes_detail.tsx (전체 코드)

import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import "../../css/Notes/Notes_detail.css";
import NoteEditor from "./NoteEditor";
import SourceList from "./components/SourceList";
import type { SourceRow } from "./components/SourceList";
import VideoList from "./components/VideoList";
// ▼ VideoItem 타입이 확장되었으므로, 그대로 import
import type { VideoItem } from "./components/VideoList";

const API_BASE = "http://localhost:8080";

export default function NoteDetail() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    // (기존 상태값들 ... 생략 ... )
    const [title] = useState<string>("");
    const [html, setHtml] = useState<string>("");
    const [saving, setSaving] = useState<"idle" | "saving" | "saved">("idle");
    const lastSavedAtRef = useRef<string>("");
    const [sources, setSources] = useState<SourceRow[]>([]);
    const fileRef = useRef<HTMLInputElement>(null);
    const openPicker = () => fileRef.current?.click();
    const [chatInput, setChatInput] = useState("");
    const [messages, setMessages] = useState<{ role: "user" | "assistant"; text: string }[]>([]);
    const [uploading, setUploading] = useState(false);
    const [asking, setAsking] = useState(false);

    // ▼ [기능 2] 영상 추천 상태 (기존과 동일)
    const [videos, setVideos] = useState<VideoItem[]>([]);
    const [recommending, setRecommending] = useState(false);

    // ▼ [기능 3] 영상 분석 상태 (기존과 동일)
    const [analyzingVideoId, setAnalyzingVideoId] = useState<string | null>(null);

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
        // (기존 askWithFiles 로직 ... 생략 ...)
        const r = await fetch(`${API_BASE}/api/notes/${noteId}/ask?q=${encodeURIComponent(q)}`, {
            method: "POST",
            credentials: "include",
        });
        if (!r.ok) throw new Error(`질문 실패: ${r.status}`);
        return r.json();
    }

    // ▼ [기G 2] Gemini/OpenAI 영상 추천 API
    async function fetchRecommendedVideos(noteId: string): Promise<VideoItem[]> {
        // ★★★ (버그 수정) 백엔드 RecoController.java에 매핑된 엔드포인트로 수정
        const r = await fetch(`${API_BASE}/api/reco/videos/${noteId}`, {
            method: "GET", // RecoController에서 GET으로도 열어둠
            credentials: "include",
        });
        if (!r.ok) throw new Error(`영상 추천 실패: ${r.status}`);

        // ★★★ (개선) 백엔드는 RecoItemDTO (score, reason 포함)를 반환합니다.
        return r.json();
    }

    // ▼ [기능 3] YouTube 영상 분석 API (기존과 동일)
    async function analyzeVideoUrl(url: string): Promise<{ summary: string }> {
        // (기존 analyzeVideoUrl 로직 ... 생략 ...)
        const r = await fetch(`${API_BASE}/api/videos/analyze?url=${encodeURIComponent(url)}`, {
            method: "POST",
            credentials: "include",
        });
        if (!r.ok) throw new Error(`영상 분석 실패: ${r.status}`);
        return r.json();
    }


    // ───────────────────────────────────────────────────────────────
    // 초기 로드: 소스 목록 읽기
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

    // ★★★ (UX 개선) "처리중..."인 파일이 있으면 5초마다 상태 자동 갱신
    useEffect(() => {
        if (!id) return;

        const needsRefresh = sources.some(s => s.type === 'FILE' && !s.openaiFileId);
        if (!needsRefresh) return; // 모두 "준비됨" 상태면 폴링 중지

        const timer = setInterval(() => {
            fetchSources(id)
                .then(list => setSources(list))
                .catch(console.error);
        }, 5000); // 5초마다 갱신

        return () => clearInterval(timer);
    }, [id, sources]); // sources가 갱신될 때마다 조건을 다시 체크

    // ───────────────────────────────────────────────────────────────
    // Ctrl/Cmd + S 저장 (기존과 동일)
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
        // (기존 handleSave 로직 ... 생략 ...)
        if (!id) return;
        setSaving("saving");
        localStorage.setItem(`note:${id}:title`, title.trim());
        localStorage.setItem(`note:${id}:content`, html);
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


    // 파일 선택 → 업로드 → DB 반영 (기존과 동일)
    const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = async (e) => {
        // (기존 onPickFiles 로직 ... 생략 ...)
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

    // ▼ [기능 2] 영상 추천 핸들러 (기존과 동일)
    const handleRecommendVideos = async () => {
        // (기존 handleRecommendVideos 로직 ... 생략 ...)
        if (!id || recommending) return;
        setRecommending(true);
        try {
            const recommendedVideos = await fetchRecommendedVideos(id);
            setVideos(recommendedVideos);
        } catch (err: any) {
            alert(`영상 추천 중 오류 발생: ${err.message}`);
            setVideos([]);
        } finally {
            setRecommending(false);
        }
    };

    // ▼ [기능 3] 영상 분석 핸들러 (기존과 동일)
    const handleAnalyzeVideo = async (video: VideoItem) => {
        // (기존 handleAnalyzeVideo 로직 ... 생략 ...)
        if (analyzingVideoId) return;
        setAnalyzingVideoId(video.id);
        try {
            const { summary } = await analyzeVideoUrl(video.url);
            setMessages((prev) => [
                ...prev,
                { role: "assistant", text: `[영상 분석 완료: ${video.title}]\n\n${summary}` },
            ]);
            setTimeout(() => {
                const el = document.getElementById("cgpt-scroll");
                if (el) el.scrollTop = el.scrollHeight;
            }, 0);
        } catch (err: any) {
            setMessages((prev) => [
                ...prev,
                { role: "assistant", text: `[${video.title}] 영상 분석 실패: ${err.message}` },
            ]);
        } finally {
            setAnalyzingVideoId(null);
        }
    };


    // 채팅 전송 → 첨부파일 기반 질문 (기존과 동일)
    const sendChat = async () => {
        // (기존 sendChat 로직 ... 생략 ...)
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

    // (기존 스크롤 로직 ... 생략 ...)
    useEffect(() => {
        const el = document.getElementById("cgpt-scroll");
        if (el) el.scrollTop = el.scrollHeight;
    }, [messages, asking]);


    // (기존 ChatBubble 컴포넌트 ... 생략 ...)
    function ChatBubble({ role, text }: { role: "user" | "assistant"; text: string }) {
        const isUser = role === "user";
        const copy = async () => {
            try { await navigator.clipboard.writeText(text); } catch {}
        };
        return (
            <div className={`cgpt-row ${isUser ? "user" : "ai"}`}>
                <div className="cgpt-avatar">{isUser ? "🧑" : "🤖"}</div>
                <div className={`cgpt-bubble ${isUser ? "user" : "ai"}`}>
                    {text.split("\n").map((line, i) => <div key={i}>{line}</div>)}
                    {!isUser && (
                        <button className="cgpt-copy" onClick={copy} title="복사">Copy</button>
                    )}
                </div>
            </div>
        );
    }

    // ───────────────────────────────────────────────────────────────
    // 렌더링 (기존과 동일)
    // ───────────────────────────────────────────────────────────────
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
                {/* 좌측: 소스/영상 */}
                <aside className="nd-side nd-side-split" id="nd-side_nd-side-split">

                    {/* 소스 파트 */}
                    <div className="nd-side-section">
                        <SourceList
                            sources={sources}
                            onClickAdd={openPicker}
                        />
                    </div>

                    {/* 숨겨진 파일 입력 */}
                    <input
                        ref={fileRef}
                        type="file"
                        hidden
                        multiple
                        // accept="application/pdf,..." // (필요시 주석 해제)
                        onChange={onPickFiles}
                    />

                    {/* 영상 파트 */}
                    <div className="nd-side-section">
                        <VideoList
                            videos={videos}
                            onClickAdd={handleRecommendVideos}
                            recommending={recommending}
                            onAnalyze={handleAnalyzeVideo}
                            analyzingVideoId={analyzingVideoId}
                        />
                    </div>

                </aside>

                {/* 가운데: 본문 */}
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

                {/* 우측: AI 채팅 패널 */}
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

// // Notes_detail.tsx (전체 코드)
//
// import { useParams, useNavigate } from "react-router-dom";
// import { useEffect, useRef, useState } from "react";
// import "../../css/Notes/Notes_detail.css";
// import NoteEditor from "./NoteEditor";
// import SourceList from "./components/SourceList";
// import type { SourceRow } from "./components/SourceList";
// import VideoList from "./components/VideoList";
// import type { VideoItem } from "./components/VideoList"; // ◀ VideoItem 타입 가져오기
//
// const API_BASE = "http://localhost:8080";
//
// /** 서버 반환 모델 (DB 기준) */
// // type SourceRow = {
// //     id: number;
// //     type: "FILE" | "URL" | "NOTION";
// //     name: string;
// //     value: string;           // 서버 로컬 경로 문자열
// //     openaiFileId?: string;   // "file-xxxx" (있으면 질문에 사용)
// // };
//
//
// export default function NoteDetail() {
//     const { id } = useParams<{ id: string }>();
//     const navigate = useNavigate();
//
//     // 제목/HTML 초안 저장(현 UI 유지)
//     const [title] = useState<string>("");
//     const [html, setHtml] = useState<string>("");
//     const [saving, setSaving] = useState<"idle" | "saving" | "saved">("idle");
//     const lastSavedAtRef = useRef<string>("");
//
//     // 좌측: 소스 리스트 (UI 그대로)
//     const [sources, setSources] = useState<SourceRow[]>([]);
//     const fileRef = useRef<HTMLInputElement>(null);
//     const openPicker = () => fileRef.current?.click();
//
//
//     // 우측: 채팅(UI 그대로—빈 영역에 메시지만 채움)
//     const [chatInput, setChatInput] = useState("");
//     const [messages, setMessages] = useState<{ role: "user" | "assistant"; text: string }[]>([]);
//
//     // ───────────────────────────────────────────────────────────────
//     // API helpers
//     // ───────────────────────────────────────────────────────────────
//     async function fetchSources(noteId: string): Promise<SourceRow[]> {
//         const r = await fetch(`${API_BASE}/api/notes/${noteId}/sources`, { credentials: "include" });
//         if (!r.ok) return [];
//         return r.json();
//     }
//
//     async function uploadSourceFile(noteId: string, file: File): Promise<SourceRow> {
//         const fd = new FormData();
//         fd.append("file", file);
//         const r = await fetch(`${API_BASE}/api/notes/${noteId}/sources/file`, {
//             method: "POST",
//             body: fd,
//             credentials: "include",
//         });
//         if (!r.ok) throw new Error(`업로드 실패: ${r.status}`);
//         return r.json();
//     }
//
//     async function askWithFiles(noteId: string, q: string): Promise<{ answer: string; fileCount: number }> {
//         const r = await fetch(`${API_BASE}/api/notes/${noteId}/ask?q=${encodeURIComponent(q)}`, {
//             method: "POST",
//             credentials: "include",
//         });
//         if (!r.ok) throw new Error(`질문 실패: ${r.status}`);
//         return r.json();
//     }
//
//     // ▼ [기능 2] Gemini 영상 추천 API (백엔드에 구현 필요)
//     async function fetchRecommendedVideos(noteId: string): Promise<VideoItem[]> {
//         // TODO: 백엔드 API 엔드포인트 구현 필요
//         const r = await fetch(`${API_BASE}/api/notes/${noteId}/recommend-videos`, {
//             method: "POST", // 소스 기반이므로 POST가 적절할 수 있음
//             credentials: "include",
//         });
//         if (!r.ok) throw new Error(`영상 추천 실패: ${r.status}`);
//
//         // 백엔드는 { id: string, title: string, url: string }[] 형태를 반환해야 함
//         return r.json();
//     }
//
//     // ▼ [기능 3] YouTube 영상 분석 API (백엔드에 구현 필요)
//     async function analyzeVideoUrl(url: string): Promise<{ summary: string }> {
//         // TODO: 백엔드 API 엔드포인트 구현 필요
//         const r = await fetch(`${API_BASE}/api/videos/analyze?url=${encodeURIComponent(url)}`, {
//             method: "POST",
//             credentials: "include",
//         });
//         if (!r.ok) throw new Error(`영상 분석 실패: ${r.status}`);
//
//         // 백엔드는 { summary: "..." } 형태를 반환해야 함
//         return r.json();
//     }
//
//
//     // ───────────────────────────────────────────────────────────────
//     // 초기 로드: 소스 목록만 DB 기준으로 읽기 (UI 변경 없음)
//     // ───────────────────────────────────────────────────────────────
//     useEffect(() => {
//         if (!id) return;
//         (async () => {
//             try {
//                 const list = await fetchSources(id);
//                 setSources(list);
//             } catch {
//                 setSources([]);
//             }
//         })();
//     }, [id]);
//
//     // ───────────────────────────────────────────────────────────────
//     // Ctrl/Cmd + S 저장 (UI 문구 그대로)
//     // ───────────────────────────────────────────────────────────────
//     useEffect(() => {
//         const onKeyDown = (e: KeyboardEvent) => {
//             const isSave = (e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "s";
//             if (!isSave) return;
//             e.preventDefault();
//             void handleSave();
//         };
//         window.addEventListener("keydown", onKeyDown);
//         return () => window.removeEventListener("keydown", onKeyDown);
//     }, [id, title, html]);
//
//     const handleSave = async () => {
//         if (!id) return;
//         setSaving("saving");
//
//         // 1) 로컬 초안
//         localStorage.setItem(`note:${id}:title`, title.trim());
//         localStorage.setItem(`note:${id}:content`, html);
//
//         // 2) 백엔드 저장 시도(없는 API여도 에러 무시)
//         try {
//             await fetch(`${API_BASE}/api/notes/${id}/content`, {
//                 method: "POST",
//                 headers: { "Content-Type": "application/json" },
//                 body: JSON.stringify({ title: title.trim(), html }),
//             });
//         } catch {}
//
//         setSaving("saved");
//         lastSavedAtRef.current = new Date().toLocaleTimeString();
//         setTimeout(() => setSaving("idle"), 1200);
//     };
//
//
//     // 업로드 상태
//     const [uploading, setUploading] = useState(false);
//
//     // 파일 선택 → 업로드 → DB 반영
//     const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = async (e) => {
//         const input = e.currentTarget;
//         const files = Array.from(input.files ?? []);
//         if (!files.length || !id) return;
//
//         try {
//             setUploading(true);
//             const created = await uploadSourceFile(id, files[0]);
//             setSources((prev) => [created, ...prev]);
//         } catch (err) {
//             console.error(err);
//             alert("파일 업로드 중 오류가 발생했습니다.");
//         } finally {
//             setUploading(false);
//             input.value = "";
//         }
//     };
//
//     // ▼ [기능 2] 영상 추천 상태
//     const [videos, setVideos] = useState<VideoItem[]>([]);
//     const [recommending, setRecommending] = useState(false); // 추천 로딩 상태
//
//     // ▼ [기능 2] 영상 추천 핸들러 (기존 onAddVideo 대체)
//     const handleRecommendVideos = async () => {
//         if (!id || recommending) return;
//
//         setRecommending(true);
//         try {
//             const recommendedVideos = await fetchRecommendedVideos(id);
//             setVideos(recommendedVideos); // 추천받은 영상으로 목록 교체
//         } catch (err: any) {
//             alert(`영상 추천 중 오류 발생: ${err.message}`);
//             setVideos([]);
//         } finally {
//             setRecommending(false);
//         }
//     };
//
//     // ▼ [기능 3] 영상 분석 상태
//     const [analyzingVideoId, setAnalyzingVideoId] = useState<string | null>(null);
//
//     // ▼ [기능 3] 영상 분석 핸들러
//     const handleAnalyzeVideo = async (video: VideoItem) => {
//         if (analyzingVideoId) return; // 이미 다른 영상 분석 중
//
//         setAnalyzingVideoId(video.id);
//         try {
//             const { summary } = await analyzeVideoUrl(video.url);
//
//             // 분석 결과를 AI 채팅창에 추가
//             setMessages((prev) => [
//                 ...prev,
//                 {
//                     role: "assistant",
//                     text: `[영상 분석 완료: ${video.title}]\n\n${summary}`
//                 },
//             ]);
//
//             // (선택) 채팅창이 맨 아래로 스크롤되도록 함
//             setTimeout(() => {
//                 const el = document.getElementById("cgpt-scroll");
//                 if (el) el.scrollTop = el.scrollHeight;
//             }, 0);
//
//         } catch (err: any) {
//             // 분석 실패 시 에러 메시지를 채팅창에 추가
//             setMessages((prev) => [
//                 ...prev,
//                 { role: "assistant", text: `[${video.title}] 영상 분석 실패: ${err.message}` },
//             ]);
//         } finally {
//             setAnalyzingVideoId(null); // 분석 상태 해제
//         }
//     };
//
//
//     // 질문 상태
//     const [asking, setAsking] = useState(false);
//
//     // 채팅 전송 → 첨부파일 기반 질문
//     const sendChat = async () => {
//         if (!id || !chatInput.trim() || asking) return;
//         const q = chatInput.trim();
//         setChatInput("");
//         setMessages((prev) => [...prev, { role: "user", text: q }]);
//
//         try {
//             setAsking(true);
//             const { answer, fileCount } = await askWithFiles(id, q);
//             setMessages((prev) => [
//                 ...prev,
//                 { role: "assistant", text: `(첨부파일 ${fileCount}개 사용)\n${answer}` },
//             ]);
//         } catch (e: any) {
//             setMessages((prev) => [...prev, { role: "assistant", text: `에러: ${e?.message ?? "요청 실패"}` }]);
//         } finally {
//             setAsking(false);
//         }
//     };
//
//     useEffect(() => {
//         const el = document.getElementById("cgpt-scroll");
//         if (el) el.scrollTop = el.scrollHeight;
//     }, [messages, asking]);
//
//
//     //채팅방 UI
//     function ChatBubble({ role, text }: { role: "user" | "assistant"; text: string }) {
//         const isUser = role === "user";
//         const copy = async () => {
//             try {
//                 await navigator.clipboard.writeText(text);
//             } catch {}
//         };
//
//         return (
//             <div className={`cgpt-row ${isUser ? "user" : "ai"}`}>
//                 <div className="cgpt-avatar">{isUser ? "🧑" : "🤖"}</div>
//                 <div className={`cgpt-bubble ${isUser ? "user" : "ai"}`}>
//                     {text.split("\n").map((line, i) => <div key={i}>{line}</div>)}
//
//                     {/* AI 말풍선에서만 Copy 버튼 표시 */}
//                     {!isUser && (
//                         <button className="cgpt-copy" onClick={copy} title="복사">
//                             Copy
//                         </button>
//                     )}
//                 </div>
//             </div>
//         );
//     }
//
//
//     return (
//         <div className="note-detail">
//             <header className="nd-header">
//                 <button className="nd-back" onClick={() => navigate("/notes")}>← 목록</button>
//                 <h2 className="nd-logo">AI NoteBook</h2>
//
//                 <div className="nd-actions">
//                     {uploading && <span className="nd-badge">업로드중…</span>}
//                     {saving === "saving" && <span className="nd-badge">저장중…</span>}
//                     {saving === "saved" && <span className="nd-badge nd-badge-ok">저장됨 {lastSavedAtRef.current && `(${lastSavedAtRef.current})`}</span>}
//                     {saving === "idle" && !uploading && <span className="nd-badge nd-badge-dim">Ctrl+S 저장</span>}
//                 </div>
//             </header>
//
//             <div className="nd-grid">
//                 {/* 좌측: 소스/영상 (UI 동일) */}
//                 <aside className="nd-side nd-side-split" id="nd-side_nd-side-split">
//
//                     {/* 소스 파트 */}
//                     <div className="nd-side-section">
//                         <SourceList
//                             sources={sources}
//                             onClickAdd={openPicker}
//                         />
//                     </div>
//
//                     {/* 숨겨진 파일 입력 */}
//                     <input
//                         ref={fileRef}
//                         type="file"
//                         hidden
//                         multiple
//                         accept="application/pdf,..."
//                         onChange={onPickFiles}
//                     />
//
//                     {/* ▼ 수정: 영상 파트 */}
//                     <div className="nd-side-section">
//                         <VideoList
//                             videos={videos}
//                             onClickAdd={handleRecommendVideos} // [기능 2] 핸들러 연결
//                             recommending={recommending}         // [기능 2] 로딩 상태 전달
//                             onAnalyze={handleAnalyzeVideo}      // [기능 3] 핸들러 연결
//                             analyzingVideoId={analyzingVideoId} // [기능 3] 로딩 상태 전달
//                         />
//                     </div>
//
//                 </aside>
//
//                 {/* 가운데: 본문 (UI 동일) */}
//                 <main className="nd-main">
//                     <div className="nd-main-panel">
//                         <div className="nd-section">
//                             {id && (
//                                 <NoteEditor
//                                     noteId={id}
//                                     initial={html}
//                                     onChange={(h) => setHtml(h)}
//                                 />
//                             )}
//                         </div>
//                     </div>
//                 </main>
//
//                 {/* 우측: AI 채팅 패널 (UI 동일) */}
//                 <aside className="nd-chat">
//                     <div className="cgpt-chat">
//                         {/* 헤더 */}
//                         <div className="cgpt-header">
//                             <div className="cgpt-title">AI 도우미</div>
//                             <div className="cgpt-sub">첨부파일 기반 Q&A</div>
//                         </div>
//
//                         {/* 메시지 영역 */}
//                         <div className="cgpt-scroll" id="cgpt-scroll">
//                             {messages.length === 0 ? (
//                                 <div className="cgpt-empty">
//                                     <div>안녕하세요! 오른쪽 아래에 질문을 입력해보세요.</div>
//                                     <div className="cgpt-hint">예) “lec02를 5줄로 요약해줘”</div>
//                                 </div>
//                             ) : (
//                                 messages.map((m, i) => (
//                                     <ChatBubble key={i} role={m.role} text={m.text} />
//                                 ))
//                             )}
//                             {asking && (
//                                 <div className="cgpt-row ai">
//                                     <div className="cgpt-avatar">🤖</div>
//                                     <div className="cgpt-bubble ai">
//                                         <span className="cgpt-typing">
//                                             <span className="dot" />
//                                             <span className="dot" />
//                                             <span className="dot" />
//                                         </span>
//                                     </div>
//                                 </div>
//                             )}
//                         </div>
//
//                         {/* 입력 바 */}
//                         <div className="cgpt-inputbar">
//                             <input
//                                 className="cgpt-input"
//                                 placeholder="무엇이든 물어보세요…"
//                                 value={chatInput}
//                                 onChange={(e) => setChatInput(e.target.value)}
//                                 onKeyDown={(e) => { if (e.key === "Enter") sendChat(); }}
//                             />
//                             <button
//                                 className="cgpt-send"
//                                 onClick={sendChat}
//                                 disabled={asking || !chatInput.trim()}
//                                 title="보내기"
//                             >
//                                 {asking ? "…" : "보내기"}
//                             </button>
//                         </div>
//                     </div>
//                 </aside>
//             </div>
//         </div>
//     );
// }