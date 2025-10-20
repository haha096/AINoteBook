
import {BrowserRouter as Router, Routes, Route, Navigate} from "react-router-dom";
import Main from "./pages/Main";
import Login from "./pages/Login/Login";
import SignUp from "./pages/Login/SignUp";
import NotesMain from "./pages/Notes/Notes_main";
import NoteDetail from "./pages/Notes/Notes_detail";

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<Main />} />
                <Route path="/login" element={<Login />} />
                <Route path="/signup" element={<SignUp />} />
                <Route path="/notes" element={<NotesMain />} />
                <Route path="/notes/:id" element={<NoteDetail />} />

                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Router>
    );
}

export default App;
