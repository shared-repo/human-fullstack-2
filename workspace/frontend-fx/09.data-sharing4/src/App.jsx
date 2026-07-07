import DecreaseBtn from "./component/DecreaseBtn"
import IncreaseBtn from "./component/IncreaseBtn"
import Display from "./component/Display"

function App() {

  return (
    <>
      <table>
        <tr>
          <th colSpan={2}>
            <Display />
          </th>
        </tr>      
        <tr>
          <td><DecreaseBtn /></td>
          <td><IncreaseBtn /></td>
        </tr>        
      </table>      
    </>
  )
}

export default App
