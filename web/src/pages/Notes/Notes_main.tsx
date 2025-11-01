import "../../css/Notes/Notes_main.css";
import gear from "../../assets/icons/설정.png";
import {useEffect, useMemo, useState} from "react";
import Modal from "../../components/Modal";
import starFilled from "../../assets/icons/채운 별.png";
import starOutline from "../../assets/icons/안 채운 별.png";
import {useNavigate} from "react-router-dom";

type Note = {
    id: number;
    title: string;
    date: string;
    color: "pink" | "yellow" | "green" | "blue";
    favorite: boolean;
    sources?: number;
};

const API_BASE = "http://localhost:8080";

export default function Notes_main() {
    const [notes, setNotes] = useState<Note[]>([]);
    const [sortDesc, setSortDesc] = useState(true);
    const [isAddOpen, setAddOpen] = useState(false);
    const [addTitle, setAddTitle] = useState("");
    const [addColor, setAddColor] = useState<Note["color"]>("pink");

    const user = JSON.parse(localStorage.getItem("user") || "{}");

    // ✅ 아직 안쓰지만 UI 구조상 필요한 것들
    const [isSettingsOpen, setSettingsOpen] = useState(false);


    const favorites = useMemo(() => notes.filter(n => n.favorite), [notes]);
    const others    = useMemo(() => notes.filter(n => !n.favorite), [notes]);

    const sortedFav = useMemo(
        () => [...favorites].sort((a, b) => (sortDesc ? b.id - a.id : a.id - b.id)),
        [favorites, sortDesc]
    );
    const sortedOthers = useMemo(
        () => [...others].sort((a, b) => (sortDesc ? b.id - a.id : a.id - b.id)),
        [others, sortDesc]
    );

    const toggleFavorite = (id: number) => {
        setNotes((prev) =>
            prev.map((n) =>
                n.id === id ? { ...n, favorite: !n.favorite } : n
            )
        );
    };

    // ✅ 1. 페이지 로드시 DB에서 노트 불러오기
    useEffect(() => {
        if (!user?.id) return; // 로그인 안 된 상태 가드
        fetch(`${API_BASE}/api/notes/user/${user.id}`)
            .then(res => res.json())
            .then(data => {
                const mapped = data.map((n: any) => ({
                    id: n.id,
                    title: n.title,
                    date: n.createdAt?.slice(0,10).replace(/-/g, ".") ?? "----.--.--",
                    color: (n.color ?? "pink") as Note["color"],
                    favorite: false,
                    sources: n.sources?.length ?? 0,
                }));
                setNotes(mapped);
            })
            .catch(err => console.error("노트 불러오기 실패:", err));
    }, [user?.id]);
    // ✅ 2. 새 노트 생성
    const addNote = async () => {
        try {
            const res = await fetch(`${API_BASE}/api/notes/create`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    title: addTitle.trim() || "새 노트",
                    color: addColor,
                    userId: user.id,
                    sources: [],
                }),
            });
            if (!res.ok) throw new Error("노트 생성 실패");
            const data = await res.json();

            setNotes((prev) => [
                {
                    id: data.id,
                    title: data.title,
                    date: data.createdAt?.slice(0, 10).replace(/-/g, ".") ?? "----.--.--",
                    color: data.color,
                    favorite: false,
                    sources: data.sources?.length ?? 0,
                },
                ...prev,
            ]);

            setAddTitle("");
            setAddColor("pink");
            setAddOpen(false);
        } catch (err) {
            alert("노트 생성 중 오류 발생");
            console.error(err);
        }
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
                    onClick={(e) => { e.stopPropagation(); onToggleFav(note.id); }} // ← 추가
                    alt={note.favorite ? "즐겨찾기 해제" : "즐겨찾기"}
                    draggable={false}
                    width={22}
                    height={22}
                />
            </div>
        </div>
    );
}