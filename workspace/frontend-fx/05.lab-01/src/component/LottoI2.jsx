import { useState } from "react";

export default function LottoI2() {

    const [numbers, setNumbers] = useState([0, 0, 0, 0, 0, 0])
    const [average, setAverage] = useState(0)

    return (
        <>
            <table>                
                <tr>
                    <td colSpan={7}>
                        <button onClick={ () => {

                            const numbers2 = []
                            for (let i = 0; i < 6; i++) {
                                const number = Math.floor(Math.random() * 45 + 1)                                
                                numbers2.push(number);
                            }
                            numbers2.sort( (n1, n2) => n1 - n2 )
                            setNumbers(numbers2)

                            let total = 0
                            numbers2.forEach( (number) => total += number )
                            setAverage( Math.floor( total / numbers2.length ) )

                        } }>로또 당첨 예상 번호 뽑기</button>
                    </td>
                </tr>
                <tr>
                    <td>{numbers[0]}</td>
                    <td>{numbers[1]}</td>
                    <td>{numbers[2]}</td>
                    <td>{numbers[3]}</td>
                    <td>{numbers[4]}</td>
                    <td>{numbers[5]}</td>
                    <td>{average}</td>         
                </tr>
            </table>
        </>
    )

}