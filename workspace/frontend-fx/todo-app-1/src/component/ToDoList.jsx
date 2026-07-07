import ToDoItem from './ToDoItem'
import './ToDo.css'

function ToDoList({ todos }) {

  return (
    <div className="todo-list">
      {todos.map((todo, index) => (
        <ToDoItem key={index} text={todo.text} checked={todo.checked} />
      ))}
    </div>
  )
}

export default ToDoList
