import { Route, Routes } from 'react-router-dom'
import Nav from './components/Nav'
import LookupPage from './pages/LookupPage'
import ReportPage from './pages/ReportPage'
import LeaderboardPage from './pages/LeaderboardPage'

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Nav />
      <Routes>
        <Route path="/" element={<LookupPage />} />
        <Route path="/report" element={<ReportPage />} />
        <Route path="/leaderboard" element={<LeaderboardPage />} />
      </Routes>
    </div>
  )
}
