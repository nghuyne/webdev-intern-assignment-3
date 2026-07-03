import { useLocation } from 'react-router-dom'
import { IconChevronRight, IconMenu } from './icons'

const PAGE_TITLES = {
  '/': 'Tra cứu',
  '/report': 'Báo cáo',
  '/leaderboard': 'Xếp hạng',
}

export default function Header({ onOpenSidebar }) {
  const location = useLocation()
  const current = PAGE_TITLES[location.pathname] ?? 'Tra cứu'

  return (
    <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur md:pl-6">
      <button
        onClick={onOpenSidebar}
        className="rounded-md p-2 text-slate-600 hover:bg-slate-100 md:hidden"
        aria-label="Mở menu"
      >
        <IconMenu className="h-5 w-5" />
      </button>

      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-600 text-sm font-bold text-white">
          G
        </div>
        <span className="text-lg font-semibold text-slate-900">GScores</span>
      </div>

      <IconChevronRight className="h-4 w-4 text-slate-300" />
      <span className="text-sm font-medium text-slate-500">{current}</span>
    </header>
  )
}
