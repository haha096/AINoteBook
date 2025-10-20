import { useNavigate } from "react-router-dom";
import "../css/Main.css";

export default function Main() {
    const navigate = useNavigate();

    return (
        <div className="main-container">
            {/* 상단 로고 */}
            <header className="main-header">AI NoteBook</header>

            {/* 상단 오른쪽 회원가입 버튼 */}
            <button className="signup-btn" onClick={() => navigate("/signup")}>
                회원가입
            </button>

            {/* 중앙 콘텐츠 */}
            <main className="main-content">
                <h1 className="main-title">AI 노트필기</h1>
                <p className="main-desc">
                    GPT와 함께 신뢰하는 정보를 기반으로 노트필기를 저장하세요
                </p>
                {/* ✅ 로그인 버튼 클릭 시 /login 페이지로 이동 */}
                <button className="login-btn" onClick={() => navigate("/login")}>
                    AI NoteBook 로그인
                </button>

                {/* 카드 섹션 */}
                <div className="card-container">
                    <Card
                        icon="👤"
                        title="자신의 계정으로 노트필기를 저장하세요"
                        desc="로그인을 통해 나만의 노트를 안전하게 관리할 수 있습니다."
                    />
                    <Card
                        icon="⚡"
                        title="자료를 넣어 더 유용한 정보를 필기하세요"
                        desc="PDF, 텍스트, 영상 등 다양한 자료를 기반으로 필기할 수 있습니다."
                    />
                    <Card
                        icon="✔️"
                        title="AI가 노트를 요약하고 이해를 도와줍니다"
                        desc="GPT가 핵심 내용을 요약하고 학습에 도움을 줍니다."
                    />
                </div>
            </main>
        </div>
    );
}

function Card({
                  icon,
                  title,
                  desc,
              }: {
    icon: string;
    title: string;
    desc: string;
}) {
    return (
        <div className="card">
            <div className="card-icon">{icon}</div>
            <h3 className="card-title">{title}</h3>
            <p className="card-desc">{desc}</p>
        </div>
    );
}