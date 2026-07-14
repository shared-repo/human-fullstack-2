import axios from 'axios'

const client = axios.create({
  baseURL: 'http://192.168.0.19:8000/api/v1/stocks',
})

export default client
