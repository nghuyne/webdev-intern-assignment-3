import { useState } from 'react'
import { getScoreBySbd } from '../api/client'
import Card from '../components/Card'
import { IconSearch } from '../components/icons'

export default function LookupPage() {
  const [sbd, setSbd] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const data = await getScoreBySbd(sbd.trim())
      setResult(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Card
        title="Tra cứu điểm thi"
        description="Nhập số báo danh để xem điểm chi tiết từng môn thi."
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row">
          <div className="relative flex-1">
            <IconSearch className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={sbd}
              onChange={(e) => setSbd(e.target.value)}
              placeholder="Nhập số báo danh (8 chữ số)"
              className="w-full rounded-lg border border-slate-300 py-2.5 pl-9 pr-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/30"
              maxLength={8}
            />
          </div>
          <button
            type="submit"
            disabled={loading || !sbd.trim()}
            className="rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-medium text-white shadow-sm transition-colors hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? 'Đang tìm...' : 'Tra cứu'}
          </button>
        </form>
      </Card>

      {error && (
        <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {result && (
        <Card>
          <div className="mb-5 flex flex-wrap items-center gap-x-6 gap-y-2 border-b border-slate-100 pb-4">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">Số báo danh</p>
              <p className="text-base font-semibold text-slate-900">{result.sbd}</p>
            </div>
            {result.tenNgoaiNgu && (
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Ngoại ngữ</p>
                <p className="text-base font-semibold text-slate-900">{result.tenNgoaiNgu}</p>
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {result.scores.map((s) => (
              <div
                key={s.maMon}
                className="rounded-xl border border-slate-100 bg-slate-50 px-4 py-3 text-center"
              >
                <p className="text-xs font-medium text-slate-500">{s.tenMon}</p>
                <p className="mt-1 text-2xl font-bold text-primary-700">{s.diem ?? '-'}</p>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  )
}
