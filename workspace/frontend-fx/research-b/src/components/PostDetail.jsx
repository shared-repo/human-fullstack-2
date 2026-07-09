import { useState, useEffect } from 'react'
import axios from 'axios'
import './PostDetail.css'

function PostDetail({ postId }) {
  const [post, setPost] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!postId) return
    setLoading(true)
    axios.get(`https://jsonplaceholder.typicode.com/posts/${postId}`)
      .then(res => setPost(res.data))
      .finally(() => setLoading(false))
  }, [postId])

  if (!postId) return <p className="empty-msg">게시물을 선택하세요.</p>
  if (loading) return <p className="loading">로딩 중...</p>
  if (!post) return null

  return (
    <div className="post-detail">
      <h2>게시물 상세 정보</h2>
      <table className="post-detail-table">
        <tbody>
          <tr><th>ID</th><td>{post.id}</td></tr>
          <tr><th>User ID</th><td>{post.userId}</td></tr>
          <tr><th>제목</th><td>{post.title}</td></tr>
          <tr><th>내용</th><td className="body-text">{post.body}</td></tr>
        </tbody>
      </table>
    </div>
  )
}

export default PostDetail
