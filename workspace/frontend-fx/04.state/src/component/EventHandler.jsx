import { useState } from "react"

function EventHandler() {
    const [ number, setNumber ] = useState(0)
    return (
        <>
            <button onClick={ () => { setNumber( Math.floor(Math.random() * 900) + 100 ) } }>행운 숫자 뽑기</button>
            <br/>
            <h2>{number}</h2>
        </>
    )
}

export default EventHandler