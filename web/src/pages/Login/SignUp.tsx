import "../../css/Login/SignUp.css";

export default function SignUp() {
    return (
        <div className="signup-page">
            {/* 상단 로고 */}
            <h2 className="logo">AI NoteBook</h2>

            {/* 회원가입 박스 */}
            <div className="signup-box">
                <h1 className="signup-title">회원가입</h1>

                <div className="signup-form">
                    <input type="text" placeholder="아이디" className="signup-input" />
                    <input type="password" placeholder="비밀번호" className="signup-input" />
                    <input type="password" placeholder="비밀번호 확인" className="signup-input" />
                    <input type="email" placeholder="이메일" className="signup-input" />

                    <div className="signup-btn-wrap">
                        <button className="signup-btn">다음</button>
                    </div>
                </div>
            </div>

            {/* 하단 언어 선택 */}

        </div>
    );
}
