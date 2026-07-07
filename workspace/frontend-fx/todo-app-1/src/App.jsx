import ToDoHeader from './component/ToDoHeader'
import ToDoEditor from './component/ToDoEditor'
import ToDoList from './component/ToDoList'
import './App.css'

function App() {
  // Static draft data matching todo-ui2.png layout
  const initialTodos = [
    { text: '리액트의 기초 알아보기', checked: true },
    { text: '컴포넌트 스타일링해 보기', checked: true },
    { text: '일정 관리 앱 만들어 보기', checked: false },
  ]

  return (
    <div className="todo-container">
      <div className="todo-card">
        <ToDoHeader />
        <ToDoEditor />
        <ToDoList todos={initialTodos} />
      </div>
    </div>
  )
}

export default App
