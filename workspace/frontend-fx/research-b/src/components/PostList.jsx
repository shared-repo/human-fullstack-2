import { useState, useEffect } from 'react'
import axios from 'axios'
import './PostList.css'

function PostList({ userId, selectedPostId, onSelect }) {
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const url = userId
      ? `https://jsonplaceholder.typicode.com/posts?userId=${userId}`
      : 'https://jsonplaceholder.typicode.com/posts'
    setLoading(true)
    axios.get(url)
      .then(res => setPosts(res.data))
      .finally(() => setLoading(false))
  }, [userId])

  return (
    <div className="post-list">
      <h2>게시물 목록{userId ? ` (사용자 ${userId})` : ''}</h2>
      {loading ? (
        <p className="loading">로딩 중...</p>
      ) : (
        <table className="post-table">
          <thead>
            <tr>
              <th className="col-id">ID</th>
              <th className="col-uid">User ID</th>
              <th>제목</th>
            </tr>
          </thead>
          <tbody>
            {posts.map(post => (
              <tr
                key={post.id}
                onClick={() => onSelect(post.id)}
                className={selectedPostId === post.id ? 'selected' : ''}
              >
                <td className="col-id">{post.id}</td>
                <td className="col-uid">{post.userId}</td>
                <td className="col-title">{post.title}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default PostList
