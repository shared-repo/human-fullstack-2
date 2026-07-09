import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import PostList from '../components/PostList'
import PostDetail from '../components/PostDetail'
import './Post.css'

function Post() {
  const [searchParams] = useSearchParams()
  const userId = searchParams.get('userId')
  const [selectedPostId, setSelectedPostId] = useState(null)

  return (
    <div className="page-layout">
      <div className="list-panel">
        <PostList
          userId={userId ? Number(userId) : null}
          selectedPostId={selectedPostId}
          onSelect={setSelectedPostId}
        />
      </div>
      <div className="detail-panel">
        <PostDetail postId={selectedPostId} />
      </div>
    </div>
  )
}

export default Post
