import { Routes, Route } from 'react-router-dom'
import RootLayout from './pages/RootLayout'
import SalesChart from './pages/SalesChart'
import TrendChart from './pages/TrendChart'
import CategoryPieChart from './pages/CategoryPieChart'
import Home from './pages/Home'
import Weather from './pages/Weather'

function App() {

  return (
    <Routes>
      <Route element={<RootLayout />}>
        <Route path="/" element={ <Home /> } />
        <Route path="/saleschart" element={ <SalesChart /> } />
        <Route path="/trendchart" element={ <TrendChart /> } />
        <Route path="/categorychart" element={ <CategoryPieChart /> } />
        <Route path="/weather" element={ <Weather /> } />
      </Route>
    </Routes>
  )
}

export default App
