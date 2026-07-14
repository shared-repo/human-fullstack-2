import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCompanyInfo } from '../api/stockApi'

function formatNumber(value) {
  if (value === null || value === undefined) return null
  const num = Number(value)
  return Number.isNaN(num) ? String(value) : num.toLocaleString()
}

function formatPercent(value) {
  if (value === null || value === undefined) return null
  const num = Number(value)
  return Number.isNaN(num) ? String(value) : `${num.toFixed(2)}%`
}

// company info payload is a large, loosely-typed object (yfinance-style);
// pick a curated subset of fields rather than dumping everything raw.
const DISPLAY_FIELDS = [
  { key: 'longName', label: '회사명' },
  { key: 'symbol', label: '심볼' },
  { key: 'sectorDisp', label: '업종' },
  { key: 'industryDisp', label: '산업' },
  { key: 'fullExchangeName', label: '거래소' },
  { key: 'currency', label: '통화' },
  { key: 'currentPrice', label: '현재가', format: formatNumber },
  { key: 'previousClose', label: '전일종가', format: formatNumber },
  { key: 'dayLow', label: '일중 저가', format: formatNumber },
  { key: 'dayHigh', label: '일중 고가', format: formatNumber },
  { key: 'fiftyTwoWeekLow', label: '52주 최저가', format: formatNumber },
  { key: 'fiftyTwoWeekHigh', label: '52주 최고가', format: formatNumber },
  { key: 'marketCap', label: '시가총액', format: formatNumber },
  { key: 'volume', label: '거래량', format: formatNumber },
  { key: 'dividendYield', label: '배당수익률', format: formatPercent },
  { key: 'website', label: '홈페이지' },
  { key: 'phone', label: '전화번호' },
]

function buildAddress(company) {
  return [company.address1, company.address2, company.city, company.country]
    .filter(Boolean)
    .join(', ')
}

function CompanyInfo({ code }) {
  const [company, setCompany] = useState(null)
  const [fetchedCode, setFetchedCode] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!code) return

    let ignore = false

    const timer = setTimeout(() => {
      setLoading(true)
      setError('')
      getCompanyInfo(code)
        .then((data) => {
          if (!ignore) {
            setCompany(data)
            setFetchedCode(code)
          }
        })
        .catch(() => {
          if (!ignore) setError('기업 정보를 불러오지 못했습니다.')
        })
        .finally(() => {
          if (!ignore) setLoading(false)
        })
    }, 0)

    return () => {
      ignore = true
      clearTimeout(timer)
    }
  }, [code])

  const showCompany = Boolean(company) && fetchedCode === code
  const address = showCompany ? buildAddress(company) : ''

  return (
    <div className="company-info">
      <h2>상장회사 정보</h2>

      {!code && <p>왼쪽에서 회사를 검색하고 선택해 주세요.</p>}
      {loading && <p>불러오는 중...</p>}
      {error && <p className="error">{error}</p>}

      {showCompany && !loading && (
        <>
          <table>
            <tbody>
              {DISPLAY_FIELDS.filter(
                ({ key }) => company[key] !== undefined && company[key] !== null,
              ).map(({ key, label, format }) => (
                <tr key={key}>
                  <th>{label}</th>
                  <td>{format ? format(company[key]) : String(company[key])}</td>
                </tr>
              ))}
              {address && (
                <tr>
                  <th>주소</th>
                  <td>{address}</td>
                </tr>
              )}
            </tbody>
          </table>

          {company.longBusinessSummary && (
            <p className="company-summary">{company.longBusinessSummary}</p>
          )}

          <Link to={`/stocks/${code}`} className="stock-link">
            주가정보 보기 →
          </Link>
        </>
      )}
    </div>
  )
}

export default CompanyInfo
