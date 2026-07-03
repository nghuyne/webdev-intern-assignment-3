import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getBandCounts } from '../api/client'

const BANDS = [
  { key: 'gioi', label: 'Giỏi (>=8)', color: '#16a34a' },
  { key: 'kha', label: 'Khá (6.5-8)', color: '#2563eb' },
  { key: 'trungBinh', label: 'Trung bình (5-6.5)', color: '#f59e0b' },
  { key: 'yeu', label: 'Yếu (<5)', color: '#dc2626' },
]

export default function ReportPage() {
  const [data, setData] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    getBandCounts()
      .then((rows) => {
        if (!cancelled) setData(rows)
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
    <div className="max-w-5xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-4">Báo cáo phổ điểm theo môn</h1>

      {loading && <p className="text-sm text-gray-500">Đang tải dữ liệu...</p>}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!loading && !error && (
        <div className="rounded-md border border-gray-200 p-4" style={{ height: 420 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="tenMon" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              {BANDS.map((band) => (
                <Bar key={band.key} dataKey={band.key} name={band.label} fill={band.color} />
              ))}
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
