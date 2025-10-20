
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Main from "./pages/Main";
import Login from "./pages/Login/Login";
import SignUp from "./pages/Login/SignUp";
import NotesMain from "./pages/Notes/Notes_main";

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<Main />} />
                <Route path="/login" element={<Login />} />
                <Route path="/signup" element={<SignUp />} />
                <Route path="/notes" element={<NotesMain />} />
            </Routes>
        </Router>
    );
}

export default App;
