import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import "../../css/Login/SignUp.css";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";
console.log('API_BASE=', API_BASE);

type SignupResponse =
    | { token?: string; user?: { id: number; username: string; email?: string } }
    | { id: number; username: string; email?: string };

export default function SignUp() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ username: "", email: "", password: "", password2: "" });
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState<string | null>(null);

    const onSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!form.username || !form.email || !form.password) {
            setErr("아이디, 이메일, 비밀번호를 모두 입력하세요.");
            return;
        }
        if (form.password !== form.password2) {
            setErr("비밀번호가 일치하지 않습니다.");
            return;
        }
        setErr(null);
        setLoading(true);
        try {
            const res = await fetch(`${API_BASE}/api/users/signup`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    username: form.username,
                    email: form.email,
                    password: form.password,
                }),
            });
            const data: SignupResponse = await res.json();
            if (!res.ok) throw new Error((data as any)?.error || "회원가입 실패");

            const token = (data as any).token as string | undefined;
            const user = (data as any).user ?? data;
            if (token) localStorage.setItem("token", token);
            if (user) localStorage.setItem("user", JSON.stringify(user));

            navigate("/notes", { replace: true });
        } catch (e: any) {
            setErr(e.message || "회원가입 실패");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="signup-page">
            <h2 className="logo">AI NoteBook</h2>
            <div className="signup-box">
                <h1 className="signup-title">회원가입</h1>

                {/* ✅ 폼으로 감싸고 onSubmit 연결 */}
                <form className="signup-form" onSubmit={onSubmit}>
                    <input
                        type="text"
                        placeholder="아이디"
                        className="signup-input"
                        value={form.username}
                        onChange={(e) => setForm((p) => ({ ...p, username: e.target.value }))}
                    />
                    <input
                        type="password"
                        placeholder="비밀번호"
                        className="signup-input"
                        value={form.password}
                        onChange={(e) =>
                            setForm((p) =>
                            ({ ...p, password: e.target.value }))}
                    />
                    <input
                        type="password"
                        placeholder="비밀번호 확인"
                        className="signup-input"
                        value={form.password2}
                        onChange={(e) =>
                            setForm((p) =>
                            ({ ...p, password2: e.target.value }))}
                    />
                    <input
                        type="email"
                        placeholder="이메일"
                        className="signup-input"
                        value={form.email}
                        onChange={(e) =>
                            setForm((p) =>
                            ({ ...p, email: e.target.value }))}
                    />

                    {/* 에러/로딩 표시 */}
                    {err && <div style={{ color: "crimson", marginTop: 8 }}>{err}</div>}

                    <div className="signup-btn-wrap">
                        <button className="signup-btn" type="submit" disabled={loading}>
                            {loading ? "처리중..." : "다음"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}