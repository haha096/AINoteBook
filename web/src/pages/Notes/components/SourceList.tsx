// Notes/components/SourceList.tsx

import pdfIcon  from "../../../assets/icons/pdf.png";
import pptIcon  from "../../../assets/icons/ppt.png";
import wordIcon from "../../../assets/icons/word.png";

import "../../../css/Notes/components/SourceList.css"

export type SourceRow = {
    id: number;
    type: "FILE" | "URL" | "NOTION";
    name: string;
    value: string;
    openaiFileId?: string | null; // ★★★ null일 수 있음을 명시 (처리중)
};

// (getFileIcon, ICONS 등 ... 기존 코드 생략 ...)
const ICONS: Record<string, string> = {
    pdf: pdfIcon,
    ppt: pptIcon,
    pptx: pptIcon,
    doc: wordIcon,
    docx: wordIcon,
    default: pdfIcon,
};
function getExt(name: string) {
    const m = name?.toLowerCase().match(/\.([a-z0-9]+)$/);
    return m ? m[1] : "";
}
function getFileIcon(name: string) {
    const ext = getExt(name);
    return ICONS[ext] ?? ICONS.default;
}


export default function SourceList({ sources, onClickAdd }: {
    sources: SourceRow[]; onClickAdd: () => void;
}) {
    return (
        <div className="nd-panel">
            <div className="nd-panel-title">
                <span>소스</span>
                <button className="nd-add-btn" onClick={onClickAdd} title="파일 추가">+</button>
            </div>

            <div className="nd-panel-body">
                {sources.length === 0 ? (
                    <div className="nd-source-empty">소스 없음</div>
                ) : (
                    <ul className="nd-list">
                        {sources.map(s => (
                            <li
                                key={s.id}
                                className="nd-list-item"
                                // 파일 + value가 있을 때만 클릭 가능
                                onClick={() => { if (s.type === "FILE" && s.value) window.open(s.value, "_blank"); }}
                                style={{ cursor: (s.type === "FILE" && s.value) ? "pointer" : "default" }}
                            >
                                <img className="nd-list-icon" src={getFileIcon(s.name)} alt="" />
                                <span className="nd-list-text" title={s.name}>{s.name}</span>

                                {/* ▼ [UX 개선] 파일 처리 상태 배지 추가 ▼ */}
                                {s.type === 'FILE' && (
                                    <span className={`nd-status-badge ${s.openaiFileId ? 'ready' : 'processing'}`}>
                                        {s.openaiFileId ? '준비됨' : '처리중...'}
                                    </span>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}

// //  Notes/components/SourceList.tsx
//
// import pdfIcon  from "../../../assets/icons/pdf.png";
// import pptIcon  from "../../../assets/icons/ppt.png";
// import wordIcon from "../../../assets/icons/word.png";
//
// import "../../../css/Notes/components/SourceList.css"
//
// export type SourceRow = {
//     id: number;
//     type: "FILE" | "URL" | "NOTION";
//     name: string;
//     value: string;           // 서버 로컬 경로 문자열 (파일일 때 href)
//     openaiFileId?: string;   // "file-xxxx"
// };
//
// const ICONS: Record<string, string> = {
//     pdf: pdfIcon,
//     ppt: pptIcon,
//     pptx: pptIcon,
//     doc: wordIcon,
//     docx: wordIcon,
//     default: pdfIcon,
// };
//
// function getExt(name: string) {
//     const m = name?.toLowerCase().match(/\.([a-z0-9]+)$/);
//     return m ? m[1] : "";
// }
// function getFileIcon(name: string) {
//     const ext = getExt(name);
//     return ICONS[ext] ?? ICONS.default;  // nullish 병합으로 안정성
// }
//
// export default function SourceList({ sources, onClickAdd }: {
//     sources: SourceRow[]; onClickAdd: () => void;
// }) {
//     return (
//         <div className="nd-panel">
//             <div className="nd-panel-title">
//                 <span>소스</span>
//                 <button className="nd-add-btn" onClick={onClickAdd} title="파일 추가">+</button>
//             </div>
//
//             {/* ▼ 목록만 스크롤 */}
//             <div className="nd-panel-body">
//                 {sources.length === 0 ? (
//                     <div className="nd-source-empty">소스 없음</div>
//                 ) : (
//                     <ul className="nd-list">
//                         {sources.map(s => (
//                             <li
//                                 key={s.id}
//                                 className="nd-list-item"
//                                 onClick={() => { if (s.type === "FILE" && s.value) window.open(s.value, "_blank"); }}
//                                 style={{ cursor: s.type === "FILE" ? "pointer" : "default" }}
//                             >
//                                 <img className="nd-list-icon" src={getFileIcon(s.name)} alt="" />
//                                 <span className="nd-list-text">{s.name}</span>
//                             </li>
//                         ))}
//                     </ul>
//                 )}
//             </div>
//         </div>
//     );
// }