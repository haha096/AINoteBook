import "../css/componetns/Modal.css";
import { type ReactNode, useEffect } from "react";
import { createPortal } from "react-dom";

type Props = {
    open: boolean;
    onClose: () => void;
    children: ReactNode;
    ariaLabel?: string;
};

export default function Modal({ open, onClose, children, ariaLabel }: Props) {
    useEffect(() => {
        if (!open) return;
        const prev = document.body.style.overflow;
        document.body.style.overflow = "hidden";
        return () => { document.body.style.overflow = prev; };
    }, [open]);

    useEffect(() => {
        if (!open) return;
        const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [open, onClose]);

    if (!open) return null;

    const backdropStyle: React.CSSProperties = {
        position: "fixed", inset: 0, background: "rgba(0,0,0,.35)",
        display: "flex", alignItems: "center", justifyContent: "center",
        zIndex: 9999, backdropFilter: "blur(1px)"
    };
    const cardStyle: React.CSSProperties = {
        width: "min(520px, 92vw)", background: "#fff", borderRadius: 12,
        boxShadow: "0 20px 50px rgba(0,0,0,.25)", padding: "20px 22px 18px"
    };

    return createPortal(
        <div
            className="modal-backdrop"
            style={backdropStyle}            // ✅ 인라인 강제
            onClick={onClose}
            aria-hidden="true"
        >
            <div
                className="modal-card"
                style={cardStyle}              // ✅ 인라인 강제
                role="dialog"
                aria-modal="true"
                aria-label={ariaLabel}
                tabIndex={-1}
                onClick={(e) => e.stopPropagation()}
            >
                {children}
            </div>
        </div>,
        document.body
    );
}