function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  return Number.isNaN(num) ? String(value) : num.toLocaleString()
}

function formatChangeRate(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  const sign = num > 0 ? '+' : ''
  return `${sign}${num.toFixed(2)}%`
}

function QuotesList({ quotes }) {
  return (
    <div className="quotes-list">
      <h2>주가 정보 목록</h2>
      {quotes.length === 0 ? (
        <p>조회된 주가 정보가 없습니다.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>일자</th>
              <th>시가</th>
              <th>고가</th>
              <th>저가</th>
              <th>종가</th>
              <th>거래량</th>
              <th>등락률</th>
            </tr>
          </thead>
          <tbody>
            {quotes.map((q) => (
              <tr key={q.date}>
                <td>{q.date}</td>
                <td>{formatNumber(q.open)}</td>
                <td>{formatNumber(q.high)}</td>
                <td>{formatNumber(q.low)}</td>
                <td>{formatNumber(q.close)}</td>
                <td>{formatNumber(q.volume)}</td>
                <td className={q.change_rate > 0 ? 'positive' : q.change_rate < 0 ? 'negative' : ''}>
                  {formatChangeRate(q.change_rate)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default QuotesList
