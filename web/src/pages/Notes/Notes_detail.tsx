// Notes_detail.tsx (전체 코드)

import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import "../../css/Notes/Notes_detail.css";
import NoteEditor from "./NoteEditor";
import SourceList from "./components/SourceList";
import type { SourceRow } from "./components/SourceList";
import VideoList from "./components/VideoList";
import type { VideoItem } from "./components/VideoList";
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';


//AI의 프롬포트기능, 영상추천기능을 고도화
//11월13일 이 작업을 끝냄

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

    // ▼▼▼ [수정] ▼▼▼
    // const [chatInput, setChatInput] = useState(""); // <-- 이 줄은 삭제
    const [messages, setMessages] = useState<{ role: "user" | "assistant"; text: string }[]>([]);
    const [uploading, setUploading] = useState(false);
    const [asking, setAsking] = useState(false);

    // ▼▼▼ [추가] 프롬프트 빌더용 상태 ▼▼▼
    const [proMode, setProMode] = useState(false); // 프로 모드 On/Off
    const [promptTask, setPromptTask] = useState(""); // (필수) 작업 (기존 chatInput)
    const [promptRole, setPromptRole] = useState(""); // (선택) 역할
    const [promptFormat, setPromptFormat] = useState(""); // (선택) 형식
    // ▲▲▲ [추가] ▲▲▲

    const [recommendingKeyword, setRecommendingKeyword] = useState(false);

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
        const r = await fetch(`${API_BASE}/api/notes/${noteId}/ask?q=${encodeURIComponent(q)}`, {
            method: "POST",
            credentials: "include",
        });
        if (!r.ok) throw new Error(`질문 실패: ${r.status}`);
        return r.json();
    }

    // ▼▼▼ [추가됨] '프로 모드'용 API 헬퍼 ▼▼▼
    async function askStructured(
        noteId: string,
        prompt: { role: string; task: string; format: string; }
    ): Promise<{ answer: string; fileCount: number }> {

        // ★★★ (주의) 백엔드에 새로 만들어야 할 API 엔드포인트입니다.
        const r = await fetch(`${API_BASE}/api/notes/${noteId}/ask-structured`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify(prompt),
        });
        if (!r.ok) throw new Error(`질문 실패: ${r.status}`);
        return r.json();
    }
    // ▲▲▲ [추가됨] ▲▲▲

    // ▼ [기능 2] Gemini/OpenAI 영상 추천 API
    async function fetchRecommendedVideos(noteId: string): Promise<VideoItem[]> {
        const r = await fetch(`${API_BASE}/api/reco/videos/${noteId}`, {
            method: "GET",
            credentials: "include",
        });
        if (!r.ok) throw new Error(`영상 추천 실패: ${r.status}`);
        return r.json();
    }

    // ★★★ [추가] 핵심단어 영상 추천 API 헬퍼 ★★★
    async function fetchRecommendedVideosByKeyword(keyword: string): Promise<VideoItem[]> {
        // (주의!) 이 API는 백엔드에 새로 만들어야 하는 엔드포인트입니다.
        // (예시: /api/reco/videos/keyword?q=...)
        const r = await fetch(`${API_BASE}/api/reco/videos/keyword?q=${encodeURIComponent(keyword)}`, {
            method: "GET",
            credentials: "include",
        });
        if (!r.ok) throw new Error(`키워드 영상 추천 실패: ${r.status}`);
        return r.json();
    }

    // ▼ [기능 3] YouTube 영상 분석 API (기존과 동일)
    async function analyzeVideoUrl(url: string): Promise<{ summary: string }> {
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

    // ★★★ [추가] 핵심단어 영상 추천 핸들러 ★★★
    const handleRecommendVideosByKeyword = async (keyword: string) => {
        if (!keyword.trim() || recommendingKeyword || recommending) return;
        setRecommendingKeyword(true);
        try {
            const recommendedVideos = await fetchRecommendedVideosByKeyword(keyword);
            // 새 영상 목록으로 덮어쓰기
            setVideos(recommendedVideos);
        } catch (err: any) {
            alert(`키워드 추천 중 오류 발생: ${err.message}`);
            setVideos([]); // 에러 시 비우기
        } finally {
            setRecommendingKeyword(false);
        }
    };

    // ▼ [기능 3] 영상 분석 핸들러 (기존과 동일)
    const handleAnalyzeVideo = async (video: VideoItem) => {
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


    // 채팅 전송 → 첨부파일 기반 질문 (수정됨)
    const sendChat = async () => {
        // ▼▼▼ [수정됨] ▼▼▼
        if (!id || !promptTask.trim() || asking) return;

        const q = promptTask.trim();
        const r = promptRole.trim();
        const f = promptFormat.trim();

        // 입력창 비우기
        setPromptTask("");
        // (참고) 역할/형식은 사용자가 또 쓸 수 있게 비우지 않습니다.

        // 사용자가 보낸 메시지를 채팅창에 표시 (프로 모드일 경우 보낸 프롬프트도 함께 표시)
        const userMessage = proMode
            ? `[역할: ${r || '기본'}]\n[형식: ${f || '기본'}]\n\n${q}`
            : q;
        setMessages((prev) => [...prev, { role: "user", text: userMessage }]);

        try {
            setAsking(true);

            let answer = "";
            let fileCount = 0;

            if (proMode) {
                // [프로 모드] 구조화된 API 호출
                const result = await askStructured(id, { role: r, task: q, format: f });
                answer = result.answer;
                fileCount = result.fileCount;
            } else {
                // [일반 모드] 기존 API 호출
                const result = await askWithFiles(id, q);
                answer = result.answer;
                fileCount = result.fileCount;
            }

            setMessages((prev) => [
                ...prev,
                { role: "assistant", text: `(첨부파일 ${fileCount}개 사용)\n${answer}` },
            ]);
            // ▲▲▲ [수정됨] ▲▲▲
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

                    {/* * [수정] ReactMarkdown 자체에는 className prop이 없습니다.
                     * 대신, 전체를 감싸는 래퍼(wrapper) div를 만들고
                     * 여기에 className을 적용합니다.
                     */}
                    <div className="markdown-content">
                        <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                        >
                            {text}
                        </ReactMarkdown>
                    </div>

                    {!isUser && (
                        <button className="cgpt-copy" onClick={copy} title="Copy">Copy</button>
                    )}
                </div>
            </div>
        );
    }

    // ───────────────────────────────────────────────────────────────
    // 렌더링
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
                        {/* [수정됨] SourceList가 nd-panel을 내부에서 렌더링하도록
                          기존 CSS 구조에 맞춰 복원합니다.
                        */}
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
                        {/* [수정됨] VideoList가 nd-panel을 내부에서 렌더링하도록
                          기존 CSS 구조에 맞춰 복원합니다.
                        */}
                        <VideoList
                            videos={videos}
                            onClickAdd={handleRecommendVideos}
                            recommending={recommending}
                            onClickKeywordAdd={handleRecommendVideosByKeyword}
                            onAnalyze={handleAnalyzeVideo}
                            analyzingVideoId={analyzingVideoId}
                        />
                    </div>

                </aside>

                {/* 가운데: 본문 */}
                <main className="nd-main">
                    {/* [수정됨] 기존 .nd-main-panel 구조 복원 */}
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

                        {/* ▼▼▼ [수정됨] 입력 바 (프로 모드 UI) ▼▼▼ */}
                        <div className={`cgpt-inputbar ${proMode ? 'pro' : ''}`}>

                            {/* --- 프로 모드일 때만 보이는 입력창 --- */}
                            {proMode && (
                                <div className="cgpt-pro-inputs">
                                    <input
                                        className="cgpt-pro-input"
                                        placeholder="AI 역할 (예: 전문 리뷰어, 친절한 교사)"
                                        value={promptRole}
                                        onChange={(e) => setPromptRole(e.target.value)}
                                    />
                                    <input
                                        className="cgpt-pro-input"
                                        placeholder="결과 형식 (예: 3줄 요약, 마크다운 표)"
                                        value={promptFormat}
                                        onChange={(e) => setPromptFormat(e.target.value)}
                                    />
                                </div>
                            )}

                            {/* --- 메인 입력창 (Task) --- */}
                            <input
                                className="cgpt-input"
                                placeholder={proMode ? "AI에게 시킬 작업 (Task) 입력..." : "무엇이든 물어보세요…"}
                                value={promptTask}
                                onChange={(e) => setPromptTask(e.target.value)}
                                onKeyDown={(e) => { if (e.key === "Enter") sendChat(); }}
                            />

                            {/* --- 전송 버튼 및 프로 모드 토글 --- */}
                            <div className="cgpt-buttons">
                                <button
                                    className="cgpt-pro-toggle"
                                    title={proMode ? "일반 모드로" : "프로 모드로"}
                                    onClick={() => setProMode(p => !p)}
                                >
                                    {proMode ? '🎓 Pro' : '🌱'}
                                </button>
                                <button
                                    className="cgpt-send"
                                    onClick={sendChat}
                                    disabled={asking || !promptTask.trim()}
                                    title="보내기"
                                >
                                    {asking ? "…" : "보내기"}
                                </button>
                            </div>
                        </div>
                        {/* ▲▲▲ [수정됨] ▲▲▲ */}

                    </div>
                </aside>
            </div>
        </div>
    );
}