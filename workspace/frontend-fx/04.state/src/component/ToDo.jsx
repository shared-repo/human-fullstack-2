function ToDo( {toDo} ) {

    return (
        <>
            <h2>제목 : { toDo.title }</h2>
            <h3>완료여부 : { toDo.completed ? "완료" : '진행중' }</h3>
        </>
    )
}

export default ToDo