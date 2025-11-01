import { useEffect} from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";

type Props = {
    noteId: string;
    initial?: string;                // 초기 내용 (백엔드/로컬에서 읽어온 값)
    onChange?: (html: string) => void;
};

export default function NoteEditor({ noteId, initial = "", onChange }: Props) {
    const editor = useEditor({
        extensions: [
            StarterKit,
            Placeholder.configure({ placeholder: "여기에 노트필기를 시작하세요…" }),
        ],
        content: initial || "<p></p>",
        editorProps: {
            attributes: {
                class:
                    "prose max-w-none outline-none min-h-[480px] px-4 py-3",
            },
        },
        onUpdate({ editor }) {
            const html = editor.getHTML();
            localStorage.setItem(`note:${noteId}:content`, html);
            onChange?.(html);              // ← 부모(NoteDetail)로 최신 HTML 전달
        },

    });

    // noteId 바뀌면 저장된 초안을 불러와서 채움
    useEffect(() => {
        const saved = localStorage.getItem(`note:${noteId}:content`);
        if (saved && editor) editor.commands.setContent(saved);
    }, [noteId, editor]);

    if (!editor) return null;

    return (
        <div className="nd-editor">
            {/* 미니 툴바 */}
            <div className="nd-toolbar">
                <button onClick={() => editor.chain().focus().toggleBold().run()}
                        className={editor.isActive("bold") ? "active" : ""}>B</button>
                <button onClick={() => editor.chain().focus().toggleItalic().run()}
                        className={editor.isActive("italic") ? "active" : ""}>I</button>
                <button onClick={() => editor.chain().focus().toggleBulletList().run()}
                        className={editor.isActive("bulletList") ? "active" : ""}>● List</button>
                <button onClick={() => editor.chain().focus().toggleOrderedList().run()}
                        className={editor.isActive("orderedList") ? "active" : ""}>1. List</button>
                <button onClick={() => editor.chain().focus().setHeading({ level: 2 }).run()}
                        className={editor.isActive("heading", { level: 2 }) ? "active" : ""}>H2</button>
                <button onClick={() => editor.chain().focus().setHeading({ level: 3 }).run()}
                        className={editor.isActive("heading", { level: 3 }) ? "active" : ""}>H3</button>
                <button onClick={() => editor.chain().focus().undo().run()}>↶</button>
                <button onClick={() => editor.chain().focus().redo().run()}>↷</button>
            </div>

            <div className="nd-editor-surface">
                <EditorContent editor={editor} />
            </div>
        </div>
    );
}