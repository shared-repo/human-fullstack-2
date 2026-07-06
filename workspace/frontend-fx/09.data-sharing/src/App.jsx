import { useState } from "react"
import DecreaseBtn from "./component/DecreaseBtn"
import IncreaseBtn from "./component/IncreaseBtn"
import Display from "./component/Display"

function App() {

  const [count, setCount] = useState(0)

  return (
    <>
      <table>
        <tr>
          <th colSpan={2}>
            <Display count={count} />
          </th>
        </tr>
      </table>
      <hr />
      <table>
        <tr>
          <td><DecreaseBtn setCount={setCount} /></td>
          <td><IncreaseBtn setCount={setCount} /></td>
        </tr>
        <tr>
          <th colSpan={2}>{count}</th>
        </tr>
      </table>
      
    </>
  )
}

export default App
