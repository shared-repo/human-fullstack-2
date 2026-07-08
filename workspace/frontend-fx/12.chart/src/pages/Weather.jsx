import { useEffect } from "react"
import { useState } from "react"

import {
  LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer,
} from 'recharts';

import axios from "axios"

export default function Weather() {

    const [data, setData] = useState([])
    useEffect( () => {

        const url = 'http://apis.data.go.kr/1360000/AsosDalyInfoService/getWthrDataList'
        const serviceKey = 'R%2BBEwJ49XVmTutdoCAdNq4m7wjOsdFNhVtlzkxWVeZJPhwQqWbGcD7CNeQPipFzIWAanQVw%2FrPLsGkfcYvD4AQ%3D%3D'
        const numOfRows = 200
        const dataType = 'JSON'
        const startDt = '20260101'
        const endDt = '20260701'
        const dataCd = 'ASOS'
        const dateCd = 'DAY'
        const stnIds = 108
        axios.get(`${url}?serviceKey=${serviceKey}&dataCd=${dataCd}&dateCd=${dateCd}&stnIds=${stnIds}&numOfRows=${numOfRows}&dataType=${dataType}&startDt=${startDt}&endDt=${endDt}`)
             // .then( (response) => { console.log(response.data.response.body.items.item) })
             .then( (response) => { setData(response.data.response.body.items.item) })

    }, [])

    return (
        <>
            <h1>날씨 데이터를 보여줄 예정입니다.</h1>

            <ResponsiveContainer width="100%" height={500}>
                <LineChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="tm" />
                <YAxis />
                <Tooltip />
                <Line type="monotone" dataKey="avgTa" stroke="#3429ad" strokeWidth={5} />
                </LineChart>
            </ResponsiveContainer>
        </>
    )
}