import {
  LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer,
} from 'recharts';

const data = [
  { name: '1월', sales: 400 },
  { name: '2월', sales: 300 },
  { name: '3월', sales: 600 },
];

export default function TrendChart() {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis />
        <Tooltip />
        <Line type="monotone" dataKey="sales" stroke="#2e7d32" />
      </LineChart>
    </ResponsiveContainer>
  );
}
