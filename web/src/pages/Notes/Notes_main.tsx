import "../../css/Notes/Notes_main.css";
import gear from "../../assets/icons/설정.png";
import { useMemo, useState } from "react";
import Modal from "../../components/Modal";
import starFilled from "../../assets/icons/채운 별.png";
import starOutline from "../../assets/icons/안 채운 별.png";
import {useNavigate} from "react-router-dom";

type Note = {
    id: number;
    title: string;
    date: string; // yyyy.MM.dd
    sources: number;
    color: "pink" | "yellow" | "green" | "blue";
    favorite: boolean;
};

const initialNotes: Note[] = [
    { id: 1, title: "생성형 AI 3주차", date: "2025.10.11", sources: 2, color: "pink",   favorite: true  },
    { id: 2, title: "생성형 AI 4주차", date: "2025.10.11", sources: 2, color: "yellow", favorite: true  },
    { id: 3, title: "생성형 AI 5주차", date: "2025.10.11", sources: 2, color: "green",  favorite: true  },
    { id: 4, title: "생성형 AI 3주차", date: "2025.10.11", sources: 2, color: "pink",   favorite: false },
    { id: 5, title: "생성형 AI 4주차", date: "2025.10.11", sources: 2, color: "yellow", favorite: false },
    { id: 6, title: "생성형 AI 5주차", date: "2025.10.11", sources: 2, color: "green",  favorite: false },
    { id: 7, title: "생성형 AI 6주차", date: "2025.10.11", sources: 2, color: "blue",   favorite: false },
];

const formatDate = (d = new Date()) =>
    `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;




export default function Notes_main() {

    // 상태 (중복 선언 금지)
    const [notes, setNotes] = useState<Note[]>(initialNotes);
    const [sortDesc, setSortDesc] = useState(true);
    const [isAddOpen, setAddOpen] = useState(false);
    const [isSettingsOpen, setSettingsOpen] = useState(false);

    // “노트 추가” 모달 내부 폼 상태
    const [addTitle, setAddTitle] = useState("");
    const [addColor, setAddColor] = useState<Note["color"]>("pink");

    // 파생 데이터
    const favorites = useMemo(() => notes.filter(n => n.favorite), [notes]);
    const others = useMemo(() => notes.filter(n => !n.favorite), [notes]);

    const sortedFav = useMemo(
        () => [...favorites].sort((a, b) => (sortDesc ? b.id - a.id : a.id - b.id)),
        [favorites, sortDesc]
    );
    const sortedOthers = useMemo(
        () => [...others].sort((a, b) => (sortDesc ? b.id - a.id : a.id - b.id)),
        [others, sortDesc]
    );

    const toggleFavorite = (id: number) => {
        setNotes(prev => prev.map(n => (n.id === id ? { ...n, favorite: !n.favorite } : n)));
    };

    const addNote = () => {
        const title = addTitle.trim() || "새 노트";
        const nextId = Math.max(...notes.map(n => n.id)) + 1;
        const newNote: Note = {
            id: nextId,
            title,
            date: formatDate(),
            sources: 0,
            color: addColor,
            favorite: false,
        };
        setNotes(prev => [newNote, ...prev]);
        // 폼 초기화 + 닫기
        setAddTitle("");
        setAddColor("pink");
        setAddOpen(false);
    };

    return (
        <div className="notes-page">
            {/* 헤더 */}
            <header className="notes-header">
                <h1 className="logo">AI NoteBook</h1>

                <div className="header-actions">
                    <button
                        className="sort-btn"
                        onClick={() => setSortDesc(v => !v)}
                        title="최신 항목 정렬 토글"
                    >
                        최신 항목 <span className={`arrow ${sortDesc ? "down" : "up"}`} />
                    </button>

                    <button className="create-btn" onClick={() => setAddOpen(true)}>
                        노트필기 만들기
                    </button>

                    <button className="gear-btn" aria-label="설정" onClick={() => setSettingsOpen(true)}>
                        <img src={gear} className="gear-img" alt="설정" width={20} height={20} />
                    </button>
                </div>
            </header>

            {/* 즐겨찾기 */}
            <section className="section">
                <div className="section-title">즐겨찾기 한 노트필기</div>
                <div className="card-grid">
                    {sortedFav.length === 0 ? (
                        <div className="empty">즐겨찾기한 노트가 없습니다.</div>
                    ) : (
                        sortedFav.map(n => <NoteCard key={n.id} note={n} onToggleFav={toggleFavorite} />)
                    )}
                </div>
            </section>

            {/* 전체 */}
            <section className="section">
                <div className="section-title">전체</div>
                <div className="card-grid">
                    {sortedOthers.map(n => (
                        <NoteCard key={n.id} note={n} onToggleFav={toggleFavorite} />
                    ))}
                </div>
            </section>

            {/* ===== 모달들 ===== */}

            {/* 노트 추가 모달 */}
            <Modal open={isAddOpen} onClose={() => setAddOpen(false)} ariaLabel="노트 추가">
                <h3 className="modal-title">노트 추가하기</h3>

                <div className="modal-body">
                    <input
                        className="modal-input"
                        placeholder="노트제목"
                        value={addTitle}
                        onChange={(e) => setAddTitle(e.target.value)}
                        autoFocus
                    />

                    <div className="modal-row">
                        <label className="modal-label">색상</label>
                        <div className="color-row">
                            {(["pink","yellow","green","blue"] as Note["color"][]).map(c => (
                                <label key={c} className="color-radio">
                                    <input
                                        type="radio"
                                        name="add-color"
                                        value={c}
                                        checked={addColor === c}
                                        onChange={() => setAddColor(c)}
                                    />
                                    <span className={`color-dot ${c}`} />
                                </label>
                            ))}
                        </div>
                    </div>

                    <div className="modal-actions">
                        <button type="button" className="btn-ghost" onClick={() => setAddOpen(false)}>취소</button>
                        <button type="button" className="btn-solid" onClick={addNote}>추가</button>
                    </div>
                </div>
            </Modal>

            {/* 설정 모달 */}
            <Modal open={isSettingsOpen} onClose={() => setSettingsOpen(false)} ariaLabel="설정">
                <h3 className="modal-title">설정</h3>

                <div className="settings-rows">
                    <div className="settings-row">
                        <span className="settings-key">계정 아이디</span>
                        <span className="divider">|</span>
                        <span className="settings-val">test1</span>
                    </div>
                    <div className="settings-row">
                        <span className="settings-key">계정 이메일</span>
                        <span className="divider">|</span>
                        <span className="settings-val">test1@test1.com</span>
                    </div>

                    <div className="settings-footer">
                        <button className="btn-solid" onClick={() => alert("로그아웃(연동 예정)")}>로그아웃</button>
                    </div>
                </div>
            </Modal>
        </div>
    );
}


function NoteCard({ note, onToggleFav }: { note: Note; onToggleFav: (id: number) => void }) {
    const navigate = useNavigate();

    return (
        <div
            className={`note-card ${note.color}`}
            onClick={() => navigate(`/notes/${note.id}`)}           // ✅ 클릭 → 상세
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && navigate(`/notes/${note.id}`)}
        >
            <div className="title">{note.title}</div>
            <div className="footer">
                <span className="date">{note.date}</span>
                <img
                    className="star"
                    src={note.favorite ? starFilled : starOutline}
                    onClick={() => onToggleFav(note.id)}
                    alt={note.favorite ? "즐겨찾기 해제" : "즐겨찾기"}
                    draggable={false}
                    width={22}
                    height={22}
                />
            </div>
        </div>
    );
}