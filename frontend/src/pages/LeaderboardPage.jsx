import { useEffect, useState } from 'react'
import { getLeaderboardGroupA } from '../api/client'
import Card from '../components/Card'
import { IconUser } from '../components/icons'
import { MEDALS } from '../theme'

function RankBadge({ rank }) {
  const medal = MEDALS[rank]
  if (!medal) {
    return (
      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-sm font-semibold text-slate-600">
        {rank}
      </span>
    )
  }
  return (
    <span
      className="flex h-8 w-8 items-center justify-center rounded-full border-2 text-sm font-bold shadow-sm"
      style={{ backgroundColor: medal.bg, color: medal.text, borderColor: medal.border }}
    >
      {rank}
    </span>
  )
}

export default function LeaderboardPage() {
  const [entries, setEntries] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    getLeaderboardGroupA()
      .then((rows) => {
        if (!cancelled) setEntries(rows)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Card
        title="Bảng xếp hạng khối A"
        description="Top 10 thí sinh có tổng điểm cao nhất (Toán, Lý, Hóa)."
      >
        {loading && <p className="text-sm text-slate-500">Đang tải dữ liệu...</p>}

        {error && (
          <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        {!loading && !error && (
          <div className="overflow-hidden rounded-xl border border-slate-100">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-400">
                  <th className="px-4 py-3 font-medium">Hạng</th>
                  <th className="px-4 py-3 font-medium">Thí sinh</th>
                  <th className="px-4 py-3 text-right font-medium">Tổng điểm</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((e) => (
                  <tr
                    key={e.sbd}
                    className="border-t border-slate-100 transition-colors hover:bg-primary-50/60"
                  >
                    <td className="px-4 py-3">
                      <RankBadge rank={e.hang} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-50 text-primary-600">
                          <IconUser className="h-5 w-5" />
                        </span>
                        <span className="font-medium text-slate-800">{e.sbd}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right text-base font-bold text-primary-700">
                      {e.tongDiem}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
