import { Link, NavLink, Outlet } from "react-router-dom";

export default function RootLayout() {
    return (
        <>
            <header>Header</header>
            <nav>
                <Link to="/">Home</Link>
                &nbsp;|&nbsp;
                <NavLink to="/about" className={({ isActive }) => (isActive ? "active" : "")} >
                    About
                </NavLink>
                &nbsp;|&nbsp;
                <NavLink to="/team/pink-spider/group/kpbl">
                    Team
                </NavLink>
                &nbsp;|&nbsp;
                <NavLink to="/dashboard" style={({ isActive }) => ({ color: isActive ? "red" : "black" })} >
                    Dashboard
                </NavLink>
                &nbsp;|&nbsp;
                <NavLink to="/dashboard/settings">
                    {({ isActive }) => <span>settings({isActive && "selected"})</span>}
                </NavLink>
            </nav>

            <Outlet /> {/* Outlet : App.jsx에 있는 Route 계층 구조에서 현재 페이지의 하위 요소를 삽입하는 태그 */}
            <footer>Footer</footer>
        </>
    );
}
