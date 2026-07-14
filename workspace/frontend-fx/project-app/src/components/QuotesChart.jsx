import {
  ComposedChart,
  Line,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'

function QuotesChart({ quotes }) {
  return (
    <div className="quotes-chart">
      <h2>주가 정보 그래프</h2>
      {quotes.length === 0 ? (
        <p>조회된 주가 정보가 없습니다.</p>
      ) : (
        <ResponsiveContainer width="100%" height={420}>
          <ComposedChart data={quotes} margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis yAxisId="price" orientation="left" />
            <YAxis yAxisId="volume" orientation="right" />
            <Tooltip />
            <Legend />
            <Bar yAxisId="volume" dataKey="volume" name="거래량" fill="#c7c2f0" />
            <Line
              yAxisId="price"
              type="monotone"
              dataKey="close"
              name="종가"
              stroke="#aa3bff"
              dot={false}
              strokeWidth={2}
            />
          </ComposedChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}

export default QuotesChart
