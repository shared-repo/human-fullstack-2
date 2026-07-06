import { useEffect, useState } from "react";

export default function UserList() {

    const [users, setUsers] = useState([])

    useEffect(() => {
        fetch("https://jsonplaceholder.typicode.com/users")
            .then( response => response.json() )
            .then( users2 => setUsers(users2) );
    }, []) // [] : 처음 마운트될 때 한 번 호출

    if (users.length == 0) {
        return "<h1>데이터 다운로드중</h1>"
    }

    return (
        <>
            <table>
                <thead>
                    <tr>
                        <th>이름</th>
                        <th>전화번호</th>
                        <th>이메일</th>
                    </tr>
                </thead>
                <tbody>
                    {
                        users.map( (user) => {
                            return (
                                <tr key={ user.id }>
                                    <td>{ user.name }</td>
                                    <td>{ user.phone }</td>
                                    <td>{ user.email }</td>
                                </tr>
                            )
                        })
                    }
                </tbody>
            </table>
        </>
    )
}