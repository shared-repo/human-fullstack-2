import { useState } from 'react'
import './ToDo.css'
import { useTodoStore } from '../store/TodoStore'

function ToDoEditor() {

  const [text, setText] = useState("")
  const addTodo = useTodoStore( (state) => state.addTodo )

  function handleClick() {
    if (text.length == 0) {
      alert('할 일을 입력하세요')
      return
    }
    //alert(text)    
    // 입력된 할 일 데이터를 스토어에 저당되어 있는 목록에 추가
    addTodo(text)
    setText("")
  }

  return (
    <div className="todo-editor">
      <input
        type="text"
        placeholder="할 일을 입력하세요"
        className="todo-input"
        value={text}
        onChange={ (event) => setText(event.target.value) }
      />
      <button type="button" className="todo-add-btn" onClick={ handleClick }>
        <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="3" fill="none">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
      </button>
    </div>
  )
}

export default ToDoEditor
