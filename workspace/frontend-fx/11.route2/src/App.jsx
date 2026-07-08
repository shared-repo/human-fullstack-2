
import { Route, Routes } from 'react-router-dom'
import './App.css'
import About from './pages/About'
import Home from './pages/Home'
import Dashboard from './pages/Dashboard'
import Summary from './pages/Summary'
import Settings from './pages/Settings'
import RootLayout from './pages/RootLayout'
import Team from './pages/Team'

function App() {

  return (
    <Routes>
      <Route element={<RootLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="about" element={<About />} />
        <Route path="team/:teamId/group/:groupId" element={ <Team /> } />
        <Route path="dashboard" element={<Dashboard />}>
          <Route index element={<Summary />} />
          <Route path="settings" element={<Settings />} />
        </Route>
        <Route path="*" element={<Home />} />
      </Route>
    </Routes>
  )
}

export default App
