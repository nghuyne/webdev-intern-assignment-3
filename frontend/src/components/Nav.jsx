import { NavLink } from 'react-router-dom'

const linkClass = ({ isActive }) =>
  `px-4 py-2 rounded-md text-sm font-medium transition-colors ${
    isActive ? 'bg-indigo-600 text-white' : 'text-gray-600 hover:bg-gray-100'
  }`

export default function Nav() {
  return (
    <nav className="border-b border-gray-200 bg-white">
      <div className="max-w-5xl mx-auto flex items-center gap-2 px-4 py-3">
        <span className="font-semibold text-gray-900 mr-4">GScores</span>
        <NavLink to="/" end className={linkClass}>
          Tra cứu
        </NavLink>
        <NavLink to="/report" className={linkClass}>
          Báo cáo
        </NavLink>
        <NavLink to="/leaderboard" className={linkClass}>
          Xếp hạng
        </NavLink>
      </div>
    </nav>
  )
}
