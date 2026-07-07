import ToDoItem from './ToDoItem'
import './ToDo.css'
import { useTodoStore } from '../store/TodoStore'

function ToDoList() {

  const todos = useTodoStore( (state) => state.todos )

  return (
    <div className="todo-list">
      {
        todos.map((todo) => (
          <ToDoItem key={todo.id} todo={todo} />
        ))
      }
    </div>
  )
}

export default ToDoList
