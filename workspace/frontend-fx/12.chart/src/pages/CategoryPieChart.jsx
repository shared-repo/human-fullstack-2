import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';

const data = [
  { name: '전자제품', value: 400 },
  { name: '식품', value: 300 },
  { name: '의류', value: 300 },
  { name: '가구', value: 500 },
];

const COLORS = ['#1565c0', '#2e7d32', '#f9a825'];

export default function CategoryPieChart() {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie data={data} dataKey="value" nameKey="name" outerRadius={100} label>
          {data.map((entry, index) => (
            <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip />
      </PieChart>
    </ResponsiveContainer>
  );
}