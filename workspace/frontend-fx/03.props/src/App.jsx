import ContactCard from "./component/ContactCard";
import ContactCard2 from "./component/ContactCard2";
import ContactCardList from "./component/ContactCardList";
import Greeting from "./component/Greeting"

function App() {
  const name = '폼플라무스';
  const age = 37;
  const contacts = [
    { 
      name:"홍길동", 
      email: "hkd@example.com", 
      phone: "010-9632-8857" 
    },
    { 
      name:"장원영", 
      email: "jwy@example.com", 
      phone: "010-1234-5678" 
    },
    { 
      name:"박지성", 
      email: "pjs@example.com", 
      phone: "010-7412-6698" 
    },
  ]
  
  return (
    <>
      <Greeting name={name} age={age} />
      <hr />
      <ContactCard contact={ { name:"홍길동", email: "hkd@example.com", phone: "010-9632-8857" } } />
      <hr />
      <ContactCardList contacts={ contacts } />
      <hr />
      <ContactCard2>
        <h2>이름: 장원영</h2>
        <h3>이메일: jwy@example.com</h3>
        <h3>전화번호: 010-5521-7789</h3>
      </ContactCard2>
    </>
  )
}

export default App
