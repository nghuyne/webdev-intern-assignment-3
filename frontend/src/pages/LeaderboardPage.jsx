import { useEffect, useState } from 'react'
import { getLeaderboardGroupA } from '../api/client'

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
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-4">
        Bảng xếp hạng khối A (Toán, Lý, Hóa)
      </h1>

      {loading && <p className="text-sm text-gray-500">Đang tải dữ liệu...</p>}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!loading && !error && (
        <table className="w-full text-sm rounded-md border border-gray-200 overflow-hidden">
          <thead>
            <tr className="bg-gray-50 text-left text-gray-500">
              <th className="px-4 py-2 font-medium">Hạng</th>
              <th className="px-4 py-2 font-medium">SBD</th>
              <th className="px-4 py-2 font-medium text-right">Tổng điểm</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.sbd} className="border-t border-gray-100">
                <td className="px-4 py-2 text-gray-800">{e.hang}</td>
                <td className="px-4 py-2 text-gray-800">{e.sbd}</td>
                <td className="px-4 py-2 text-right text-gray-900 font-medium">{e.tongDiem}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
