import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import User from './pages/User'
import Post from './pages/Post'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <nav className="app-nav">
        <span className="nav-title">My App</span>
        <NavLink to="/" end>사용자</NavLink>
        <NavLink to="/posts">게시물</NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<User />} />
        <Route path="/posts" element={<Post />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
