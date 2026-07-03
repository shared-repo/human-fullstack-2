import { useState } from "react";

function Counter() {

    // 1. 지역 변수의 변경은 렌더링을 발생시키지 않습니다 : 화면 갱신 X
    // let count = 0;
    // return (
    //     <div>
    //         <p>현재 값: {count}</p>
    //         <button onClick={() => count++}>증가</button>
    //         <button onClick={() => count--}>감소</button>
    //     </div>
    // );

    // 2. 
    const [count, setCount] = useState(0);
    return (
        <div>
            <p>현재 값: {count}</p>
            <button onClick={() => setCount(count + 1)}>증가</button>
            <button onClick={() => setCount(count - 1)}>감소</button>
        </div>
    );

}

export default Counter;