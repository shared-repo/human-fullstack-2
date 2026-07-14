import client from './client'

export async function searchCompanies(name) {
  const { data } = await client.get('/search', { params: { name } })
  return Array.isArray(data) ? data : (data?.items ?? [])
}

export async function getCompanyInfo(code) {
  const { data } = await client.get(`/${code}`)
  return data
}

export async function getStockPrices(code, startDate, endDate) {
  const { data } = await client.get(`/${code}/prices`, {
    params: { start_date: startDate, end_date: endDate },
  })
  return Array.isArray(data) ? data : (data?.items ?? [])
}
