import { useState } from 'react'
import { searchCompanies } from '../api/stockApi'

function SearchCompany({ onSelect }) {
  const [name, setName] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [hasSearched, setHasSearched] = useState(false)

  const handleSearch = async (e) => {
    e.preventDefault()
    const trimmed = name.trim() // 문자열 양쪽에서 공백 제거
    if (!trimmed) return

    setLoading(true)
    setError('')
    try {
      const data = await searchCompanies(trimmed)
      setResults(data)
    } catch {
      setError('검색 중 오류가 발생했습니다.')
      setResults([])
    } finally {
      setHasSearched(true)
      setLoading(false)
    }
  }

  return (
    <div className="search-company">
      <h2>상장회사 검색</h2>
      <form onSubmit={handleSearch}>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="회사 이름을 입력하세요"
        />
        <button type="submit" disabled={loading}>
          {loading ? '검색 중...' : '검색'}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      {hasSearched && !loading && results.length === 0 && !error && (
        <p>검색 결과가 없습니다.</p>
      )}

      {results.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>종목코드</th>
              <th>종목명</th>
            </tr>
          </thead>
          <tbody>
            {results.map((item) => (
              <tr
                key={item.code}
                className="row-clickable"
                onClick={() => onSelect(item.code)}
              >
                <td>{item.code}</td>
                <td>{item.name}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default SearchCompany
