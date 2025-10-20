// import "../../css/Login/Login.css"
// import {useNavigate} from "react-router-dom";
//
// export default function Login() {
//     const navigate = useNavigate();
//
//     return (
//         <div className="login-container">
//             <div className="login-box">
//                 <h1 className="login-title">AI NoteBook 로그인</h1>
//
//                 <p className="login-subtext">
//                     GPT 기반 노트 서비스를 이용하려면 계정으로 로그인하세요.
//                 </p>
//
//                 <form className="login-form">
//                     <label className="login-label">아이디</label>
//                     <input
//                         type="text"
//                         className="login-input"
//                         placeholder="이메일 또는 아이디"
//                     />
//
//                     <label className="login-label">비밀번호</label>
//                     <input
//                         type="password"
//                         className="login-input"
//                         placeholder="비밀번호 입력"
//                     />
//
//                     <button className="login-btn">로그인</button>
//                 </form>
//
//                 <div className="login-footer">
//                     <p>
//                         계정이 없으신가요?{" "}
//                         <a onClick={() => navigate("/signup")} className="signup-link">
//                             회원가입
//                         </a>
//                     </p>
//                 </div>
//             </div>
//         </div>
//     );
// }




import { useNavigate } from "react-router-dom";
import "../../css/Login/Login.css";

export default function Login() {
    const navigate = useNavigate();

    return (
        <div className="login-page">
            <h2 className="logo">AI NoteBook</h2>

            <div className="login-box">
                <h1 className="login-title">로그인</h1>

                <div className="login-form">
                    <input type="text" placeholder="아이디" className="login-input" />
                    <input type="password" placeholder="비밀번호" className="login-input" />

                    <div className="login-links">
                        <a href="#">비밀번호 찾기</a>
                        <span>|</span>
                        <a href="#">아이디 찾기</a>
                    </div>

                    <div className="login-footer">
                        <a onClick={() => navigate("/signup")} className="signup-link">
                            회원가입
                        </a>
                        <button
                            type="button"
                            className="login-btn"
                            onClick={() => navigate("/notes")} // ✅ 로그인 후 노트 메인으로 이동
                        >
                            로그인
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}