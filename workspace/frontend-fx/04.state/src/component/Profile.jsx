import { useState } from "react";

function Profile() {

    // 1. 여러 개의 상태변수 사용
    // const [name, setName] = useState('');
    // const [age, setAge] = useState(0);
    // return (
    //     <div>
    //         <p>이름: {name}</p>
    //         <p>나이: {age}</p>
    //         이름 : <input value={name} onChange={ (event) => setName( event.target.value )}  />
    //         <br/>
    //         나이 : <input value={age} onChange={ (event) => setAge( parseInt(event.target.value) )} />
    //     </div>
    // );

    // 2. 객체 상태 변수 사용
    const [user, setUser] = useState({name:'', age:0});
    return (
        <div>
            <p>이름: {user.name}</p>
            <p>나이: {user.age}</p>
            이름 : <input value={user.name} 
                         onChange={ (event) => setUser( { ...user, name:event.target.value} )}  />
            <br/>
            나이 : <input value={user.age} 
                         onChange={ (event) => setUser( { ...user, age: parseInt(event.target.value)} )} />
        </div>
    );
}

export default Profile