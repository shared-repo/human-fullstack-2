
// function Greeting(props) {
//     return <h2>안녕하세요, ({props.age}세) {props.name}님!</h2>;
// }

// function Greeting(props) {
//     const {age, name} = props; // destructuring
//     return <h2>안녕하세요, ({age}세) {name}님!</h2>;
// }

function Greeting({age, name}) { // destructuring
    return <h2>안녕하세요, ({age}세) {name}님!</h2>;
}

export default Greeting;