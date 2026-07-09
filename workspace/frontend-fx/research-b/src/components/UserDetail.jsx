import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import './UserDetail.css'

function UserDetail({ userId }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!userId) return // 현재 선택된 사용자가 없다면 작업 중지
    setLoading(true)
    axios.get(`https://jsonplaceholder.typicode.com/users/${userId}`)
      .then(res => setUser(res.data))
      .finally(() => setLoading(false))
  }, [userId]) // userId가 변경되면 위의 함수를 호출하는 설정

  if (!userId) return <p className="empty-msg">사용자를 선택하세요.</p>
  if (loading) return <p className="loading">로딩 중...</p>
  if (!user) return null // Open API를 통해서 데이터 조회에 실패한 경우

  return (
    <div className="user-detail">
      <h2>사용자 상세 정보</h2>
      <table className="detail-table">
        <tbody>
          <tr><th>ID</th><td>{user.id}</td></tr>
          <tr><th>이름</th><td>{user.name}</td></tr>
          <tr><th>사용자명</th><td>{user.username}</td></tr>
          <tr><th>이메일</th><td>{user.email}</td></tr>
          <tr><th>전화</th><td>{user.phone}</td></tr>
          <tr><th>웹사이트</th><td>{user.website}</td></tr>
          <tr><th>회사</th><td>{user.company?.name}</td></tr>
          <tr><th>주소</th><td>{user.address?.street}, {user.address?.city}</td></tr>
        </tbody>
      </table>
      <Link className="posts-link" to={`/posts?userId=${user.id}`}>
        이 사용자의 게시물 보기 →
      </Link>
    </div>
  )
}

export default UserDetail
