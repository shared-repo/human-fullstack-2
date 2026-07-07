import { useTodoStore } from '../store/TodoStore'
import './ToDo.css'

function ToDoItem({ todo }) {

  const deleteTodo = useTodoStore( (state) => state.deleteTodo )
  const updateTodo = useTodoStore( (state) => state.updateTodo )

  const doDelete = (id) => {
    deleteTodo(id)
  }
  const doUpdate = (id) => {
    updateTodo(id)
  }
  return (
    <div className={`todo-item ${todo.done ? 'completed' : ''}`}>
      <div className="todo-checkbox" onClick={ () => doUpdate(todo.id)}>
        {todo.done ? (
          <svg viewBox="0 0 24 24" width="20" height="20" className="checked-svg">
            <rect x="2" y="2" width="20" height="20" rx="2" ry="2" fill="#22b8cf" />
            <polyline points="6 12 10 16 18 8" fill="none" stroke="#ffffff" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round"></polyline>
          </svg>
        ) : (
          <div className="checkbox-box" />
        )}
      </div>
      <span className="todo-text">{todo.text}</span>
      <button type="button" className="todo-delete-btn" aria-label="Delete todo"
              onClick={ () => doDelete(todo.id) }>
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor">
          <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2.5" />
          <line x1="8" y1="12" x2="16" y2="12" stroke="currentColor" strokeWidth="2.5" />
        </svg>
      </button>
    </div>
  )
}

export default ToDoItem
