import { useState } from 'react'
import { getScoreBySbd } from '../api/client'

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
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-4">Tra cứu điểm thi</h1>

      <form onSubmit={handleSubmit} className="flex gap-2 mb-6">
        <input
          type="text"
          value={sbd}
          onChange={(e) => setSbd(e.target.value)}
          placeholder="Nhập số báo danh (8 chữ số)"
          className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          maxLength={8}
        />
        <button
          type="submit"
          disabled={loading || !sbd.trim()}
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {loading ? 'Đang tìm...' : 'Tra cứu'}
        </button>
      </form>

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {result && (
        <div className="rounded-md border border-gray-200 overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 text-sm text-gray-700">
            <span className="font-medium">SBD:</span> {result.sbd}
            {result.tenNgoaiNgu && (
              <span className="ml-4">
                <span className="font-medium">Ngoại ngữ:</span> {result.tenNgoaiNgu}
              </span>
            )}
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-t border-gray-200 text-left text-gray-500">
                <th className="px-4 py-2 font-medium">Môn</th>
                <th className="px-4 py-2 font-medium text-right">Điểm</th>
              </tr>
            </thead>
            <tbody>
              {result.scores.map((s) => (
                <tr key={s.maMon} className="border-t border-gray-100">
                  <td className="px-4 py-2 text-gray-800">{s.tenMon}</td>
                  <td className="px-4 py-2 text-right text-gray-900 font-medium">
                    {s.diem ?? '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
