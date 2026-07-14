import { useState } from 'react'
import SearchCompany from '../components/SearchCompany'
import CompanyInfo from '../components/CompanyInfo'

function Company() {
  const [selectedCode, setSelectedCode] = useState(null)

  return (
    <div className="company-page">
      <div className="company-page-left">
        <SearchCompany onSelect={setSelectedCode} />
      </div>
      <div className="company-page-right">
        <CompanyInfo code={selectedCode} />
      </div>
    </div>
  )
}

export default Company
