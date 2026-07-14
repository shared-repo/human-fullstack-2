import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import Company from './pages/Company'
import Stock from './pages/Stock'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <nav className="app-nav">
          <NavLink to="/" end>
            상장회사 검색
          </NavLink>
          <NavLink to="/stocks">주가정보</NavLink>
        </nav>
        <main className="app-main">
          <Routes>
            <Route path="/" element={<Company />} />
            <Route path="/stocks" element={<Stock />} />
            <Route path="/stocks/:code" element={<Stock />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
