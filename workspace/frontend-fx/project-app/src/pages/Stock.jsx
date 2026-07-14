import { useState } from 'react'
import { useParams } from 'react-router-dom'
import QuotesSearch from '../components/QuotesSearch'
import QuotesList from '../components/QuotesList'
import QuotesChart from '../components/QuotesChart'

function Stock() {
  const { code: codeFromRoute = '' } = useParams()
  const [quotes, setQuotes] = useState([])

  return (
    <div className="stock-page">
      <div className="stock-page-top">
        <QuotesSearch
          key={codeFromRoute}
          initialCode={codeFromRoute}
          onSearch={(_code, data) => setQuotes(data)}
        />
      </div>
      <div className="stock-page-bottom">
        <div className="stock-page-left">
          <QuotesList quotes={quotes} />
        </div>
        <div className="stock-page-right">
          <QuotesChart quotes={quotes} />
        </div>
      </div>
    </div>
  )
}

export default Stock
