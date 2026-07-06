import { useContext } from "react"
import { CommonContext } from "./CommonContext"

export default function DecreaseBtn() {

    const { setAppData } = useContext(CommonContext)

    return (
        <>
            <button onClick={
                () => { setAppData( (v) => { return { ...v, count: v.count - 1 } } ) }
            }>감소</button>
        </>
    )
}