import { useState } from 'react'
import UserList from '../components/UserList'
import UserDetail from '../components/UserDetail'
import './User.css'

function User() {
  const [selectedUserId, setSelectedUserId] = useState(null)

  return (
    <div className="page-layout">
      <div className="list-panel">
        <UserList selectedUserId={selectedUserId} onSelect={setSelectedUserId} />
      </div>
      <div className="detail-panel">
        <UserDetail userId={selectedUserId} />
      </div>
    </div>
  )
}

export default User
