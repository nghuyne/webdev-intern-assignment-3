const BASE_URL = import.meta.env.VITE_API_BASE_URL

async function request(path) {
  const res = await fetch(`${BASE_URL}${path}`)
  const contentType = res.headers.get('content-type') || ''
  const body = contentType.includes('application/json') ? await res.json() : null

  if (!res.ok) {
    const message = body?.message || `Yêu cầu thất bại (HTTP ${res.status})`
    const error = new Error(message)
    error.status = res.status
    throw error
  }

  return body
}

export function getScoreBySbd(sbd) {
  return request(`/api/scores/${encodeURIComponent(sbd)}`)
}

export function getBandCounts() {
  return request('/api/report/band-counts')
}

export function getLeaderboardGroupA() {
  return request('/api/leaderboard/group-a')
}
