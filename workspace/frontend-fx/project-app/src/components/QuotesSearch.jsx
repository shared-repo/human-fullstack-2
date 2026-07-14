import { useEffect, useState } from 'react'
import { getStockPrices } from '../api/stockApi'

const ONE_YEAR_MS = 365 * 24 * 60 * 60 * 1000

// API expects dates as yyyymmdd
function toApiDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}${m}${d}`
}

// <input type="date"> yields yyyy-mm-dd
function inputDateToApiDate(value) {
  return value.replaceAll('-', '')
}

function QuotesSearch({ initialCode = '', onSearch }) {
  const [code, setCode] = useState(initialCode)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const runSearch = async (codeValue, start, end) => {
    let apiStart
    let apiEnd
    if (!start || !end) {
      const today = new Date()
      apiEnd = toApiDate(today)
      apiStart = toApiDate(new Date(today.getTime() - ONE_YEAR_MS))
    } else {
      apiStart = inputDateToApiDate(start)
      apiEnd = inputDateToApiDate(end)
    }

    setLoading(true)
    setError('')
    try {
      const data = await getStockPrices(codeValue, apiStart, apiEnd)
      onSearch(codeValue, data)
    } catch {
      setError('주가 정보를 불러오지 못했습니다.')
      onSearch(codeValue, [])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!initialCode) return
    const timer = setTimeout(() => runSearch(initialCode, '', ''), 0)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialCode])

  const handleSearch = (e) => {
    e.preventDefault()
    const trimmed = code.trim()
    if (!trimmed) {
      setError('주식코드를 입력해 주세요.')
      return
    }
    runSearch(trimmed, startDate, endDate)
  }

  return (
    <form className="quotes-search" onSubmit={handleSearch}>
      <label>
        주식코드
        <input
          type="text"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="예: 005930"
        />
      </label>
      <label>
        시작일자
        <input
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
        />
      </label>
      <label>
        종료일자
        <input
          type="date"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
        />
      </label>
      <button type="submit" disabled={loading}>
        {loading ? '조회 중...' : '조회'}
      </button>
      {error && <p className="error">{error}</p>}
    </form>
  )
}

export default QuotesSearch
