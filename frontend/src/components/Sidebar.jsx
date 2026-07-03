import { NavLink } from 'react-router-dom'
import { IconChartBar, IconClose, IconSearch, IconTrophy } from './icons'

const navItems = [
  { to: '/', label: 'Tra cứu', icon: IconSearch, end: true },
  { to: '/report', label: 'Báo cáo', icon: IconChartBar },
  { to: '/leaderboard', label: 'Xếp hạng', icon: IconTrophy },
]

function NavList({ onNavigate }) {
  return (
    <nav className="flex-1 space-y-1 px-3 py-4">
      {navItems.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          onClick={onNavigate}
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
              isActive
                ? 'bg-primary-600 text-white shadow-sm'
                : 'text-slate-600 hover:bg-primary-50 hover:text-primary-700'
            }`
          }
        >
          <Icon className="h-5 w-5 shrink-0" />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

export default function Sidebar({ open, onClose }) {
  return (
    <>
      <aside className="hidden md:fixed md:inset-y-0 md:left-0 md:flex md:w-64 md:flex-col md:border-r md:border-slate-200 md:bg-white">
        <NavList />
      </aside>

      <div className={`fixed inset-0 z-40 md:hidden ${open ? '' : 'pointer-events-none'}`}>
        <div
          className={`absolute inset-0 bg-slate-900/40 transition-opacity ${
            open ? 'opacity-100' : 'opacity-0'
          }`}
          onClick={onClose}
        />
        <aside
          className={`absolute inset-y-0 left-0 flex w-64 flex-col bg-white shadow-xl transition-transform duration-200 ${
            open ? 'translate-x-0' : '-translate-x-full'
          }`}
        >
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-4">
            <span className="font-semibold text-primary-700">GScores</span>
            <button
              onClick={onClose}
              className="rounded-md p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-700"
              aria-label="Đóng menu"
            >
              <IconClose className="h-5 w-5" />
            </button>
          </div>
          <NavList onNavigate={onClose} />
        </aside>
      </div>
    </>
  )
}
