import {
  BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer,
} from 'recharts';

const data = [
  { name: '1월', sales: 400 },
  { name: '2월', sales: 300 },
  { name: '3월', sales: 600 },
];

export default function SalesChart() {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis />
        <Tooltip />
        <Bar dataKey="sales" fill="#1565c0" />
      </BarChart>
    </ResponsiveContainer>
  );
}
