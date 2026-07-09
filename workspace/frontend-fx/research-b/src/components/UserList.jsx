import { useState, useEffect } from 'react'
import axios from 'axios'
import './UserList.css'

function UserList({ selectedUserId, onSelect }) {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true) // 작업이 진행중일때 다른 작업이 개입하지 못하도록 관리하기 위한 변수

  useEffect(() => {
    axios.get('https://jsonplaceholder.typicode.com/users')
      .then(res => setUsers(res.data))
      .finally(() => setLoading(false)) // 이전 작업이 완료되면 오류 여부와 관계 없이 반드시 호출되는 함수 지정
  }, [])

  return (
    <div className="user-list">
      <h2>사용자 목록</h2>
      {loading ? (
        <p className="loading">로딩 중...</p>
      ) : (
        <table className="user-table">
          <thead>
            <tr>
              <th className="col-id">ID</th>
              <th>이름</th>
              <th>사용자명</th>
              <th>이메일</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr
                key={user.id}
                onClick={() => onSelect(user.id)}
                className={selectedUserId === user.id ? 'selected' : ''}
              >
                <td className="col-id">{user.id}</td>
                <td>{user.name}</td>
                <td>{user.username}</td>
                <td>{user.email}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default UserList
