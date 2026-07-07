import { useCommonStore } from "../store/CommonStore"

export default function Display() {

    const count = useCommonStore( (state) => state.count )
    const name = useCommonStore( (state) => state.name )
    const email = useCommonStore( (state) => state.email )

    return (
        <>
            <div>{ count }</div>
            <div>{ name }</div>
            <div>{ email }</div>
        </>
    )
}