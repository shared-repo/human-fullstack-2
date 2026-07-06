import { useContext } from "react"
import { CommonContext } from "./CommonContext"

export default function IncreaseBtn() {

    const { setAppData } = useContext(CommonContext)

    return (
        <>
            <button onClick={ 
                () => { setAppData( (v) => { return { ...v, count: v.count + 1 } } ) } 
                //                                    count:1, name:'', email: '', count:2
            }>증가</button>
        </>
    )
}