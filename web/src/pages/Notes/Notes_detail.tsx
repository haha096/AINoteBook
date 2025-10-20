import { useParams, useNavigate } from "react-router-dom";
import {useEffect, useMemo, useRef, useState} from "react";
import "../../css/Notes/Notes_detail.css";

type Section = { h2: string; body: string };
type NoteDoc = { title: string; sources: string[]; sections: Section[] };

// 데모 DB – 최소한 목록에서 쓰는 id들은 채워주세요
const NOTE_DB: Record<string, NoteDoc> = {
    "1": {
        title: "생성형 AI 3주차 노트필기",
        sources: ["lec03.pdf", "lec04.pdf"],
        sections: [
            { h2: "컨볼루션 연산", body: "제공된 두 소스, **lec03.pdf**와 **lec04.pdf**..." },
            { h2: "RNN 개요", body: "순환 신경망(RNN)의 기본 개념과 상태 정의..." },
        ],
    },
    "2": {
        title: "생성형 AI 4주차 노트필기",
        sources: ["lec05.pdf"],
        sections: [{ h2: "트랜스포머", body: "Self-Attention과 Positional Encoding..." }],
    },
    "3": { title: "생성형 AI 5주차 노트필기", sources: ["lec06.pdf"], sections: [] },
    "4": { title: "생성형 AI 3주차 노트필기", sources: ["lec03.pdf"], sections: [] },
    "5": { title: "생성형 AI 4주차 노트필기", sources: ["lec04.pdf"], sections: [] },
    "6": { title: "생성형 AI 5주차 노트필기", sources: ["lec05.pdf"], sections: [] },
    "7": { title: "생성형 AI 6주차 노트필기", sources: ["lec06.pdf"], sections: [] },
};

export default function NoteDetail() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    useEffect(() => {
        if (!id || !NOTE_DB[id]) navigate("/notes", { replace: true });
    }, [id, navigate]);

    const note = useMemo<NoteDoc | undefined>(() => (id ? NOTE_DB[id] : undefined), [id]);
    // 👉 소스 목록을 state로 관리
    const [sources, setSources] = useState<string[]>(() => (note?.sources ?? []));
    useEffect(() => {
        // 라우팅으로 노트 바뀌면 sources도 갱신
        setSources(note?.sources ?? []);
    }, [note]);

    // 파일 추가용 input
    const fileInputRef = useRef<HTMLInputElement>(null);
    const openFilePicker = () => fileInputRef.current?.click();

    const onPickFiles: React.ChangeEventHandler<HTMLInputElement> = (e) => {
        const files = Array.from(e.target.files ?? []);
        if (files.length) {
            setSources((prev) => [...prev, ...files.map((f) => f.name)]);
            e.target.value = ""; // 같은 파일 다시 선택 가능하게 초기화
        } else {
            // 파일 선택 취소 시 텍스트로 추가
            const name = window.prompt("추가할 자료 이름(또는 URL)을 입력하세요");
            if (name && name.trim()) setSources((prev) => [...prev, name.trim()]);
        }
    };

    // (채팅 데모 그대로)
    type Msg = { role: "user" | "ai"; text: string };
    const [messages, setMessages] = useState<Msg[]>([]);
    const [input, setInput] = useState("");
    const send = () => {
        const text = input.trim();
        if (!text) return;
        setMessages((m) => [...m, { role: "user", text }, { role: "ai", text: "요약/도움말 예시 응답" }]);
        setInput("");
    };

    if (!note) return null;

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
                        {/* 타이틀 + [+] 버튼 */}
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

                    <br/>

                    <div className="nd-panel">
                        <div className="nd-panel-title">영상</div>
                        <div className="nd-video-empty">영상 소스 없음</div>
                    </div>
                </aside>

                {/* 가운데: 본문 */}
                <main className="nd-main">
                    <div className="nd-main-panel">
                        <h2 className="nd-title">{note.title}</h2>

                        <section className="nd-section">
                            <p className="nd-paragraph">
                                제공된 두 소스, **lec03.pdf**와 **lec04.pdf**는 …
                            </p>
                        </section>

                        {note.sections.map((sec, idx) => (
                            <section className="nd-section" key={idx}>
                                <h3 className="nd-h2">{sec.h2}</h3>
                                <p className="nd-paragraph">{sec.body}</p>
                            </section>
                        ))}
                    </div>
                </main>

                {/* 우측: AI 대화 (세로 꽉 차도록 flex 구성) */}
                <aside className="nd-chat">
                    <div className="nd-chat-panel">
                        <div className="nd-chat-scroll">
                            {messages.length === 0 ? (
                                <div className="nd-chat-empty">AI와 노트필기를 시작해보세요</div>
                            ) : (
                                messages.map((m, i) => (
                                    <div key={i} className={`nd-msg ${m.role}`}>
                                        <div className="nd-bubble">{m.text}</div>
                                    </div>
                                ))
                            )}
                        </div>

                        <div className="nd-chat-input">
                            <input
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                placeholder="입력을 시작하세요"
                                onKeyDown={(e) => e.key === "Enter" && send()}
                            />
                            <button onClick={send}>보내기</button>
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    );
}