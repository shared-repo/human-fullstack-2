import { Outlet } from "react-router-dom";

export default function Dashboard() {
  return (
    <>
      <h1>Dashboard</h1>
      <Outlet /> {/* 하위 컴포넌트가 표시되는 영역 */}
    </>
  );
}
