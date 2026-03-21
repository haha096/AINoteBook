import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import "../../css/Login/Login.css";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";
console.log('API_BASE=', API_BASE);

type LoginResponse =
    | { token?: string; user?: { id: number; username: string; email?: string } }
    | { id: number; username: string; email?: string }; // 백엔드가 user만 주는 경우 대비

export default function Login() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ username: "", password: "" });
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState<string | null>(null);

    const onSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (!form.username || !form.password) {
            setErr("아이디와 비밀번호를 입력하세요.");
            return;
        }
        setErr(null);
        setLoading(true);
        try {
            const res = await fetch(`${API_BASE}/api/users/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(form),
            });
            const data: LoginResponse = await res.json();
            if (!res.ok) throw new Error((data as any)?.error || "로그인 실패");

            // token/user 저장 (둘 다/둘 중 하나 올 수 있음)
            const token = (data as any).token as string | undefined;
            const user = (data as any).user ?? data;
            if (token) localStorage.setItem("token", token);
            if (user) localStorage.setItem("user", JSON.stringify(user));

            navigate("/notes", { replace: true });
        } catch (e: any) {
            setErr(e.message || "로그인 실패");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <h2 className="logo">AI NoteBook</h2>

            <div className="login-box">
                <h1 className="login-title">로그인</h1>

                {/* ✅ 폼으로 감싸고 onSubmit 연결 */}
                <form className="login-form" onSubmit={onSubmit}>
                    <input
                        type="text"
                        placeholder="아이디"
                        className="login-input"
                        value={form.username}
                        onChange={(e) =>
                            setForm((p)=>
                                ({ ...p, username: e.target.value }))}
                    />
                    <input
                        type="password"
                        placeholder="비밀번호"
                        className="login-input"
                        value={form.password}
                        onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
                    />

                    <div className="login-links">
                        <a href="#" onClick={(e) => e.preventDefault()}>비밀번호 찾기</a>
                        <span>|</span>
                        <a href="#" onClick={(e) => e.preventDefault()}>아이디 찾기</a>
                    </div>

                    {/* 에러/로딩 표시 */}
                    {err && <div style={{ color: "crimson", marginTop: 8 }}>{err}</div>}

                    <div className="login-footer">
                        <a onClick={() => navigate("/signup")} className="signup-link">회원가입</a>
                        <button type="submit" className="login-btn" disabled={loading}>
                            {loading ? "처리중..." : "로그인"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}