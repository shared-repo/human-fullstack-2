import { useCommonStore } from "../store/CommonStore"

export default function DecreaseBtn() {

    const decreaseCount = useCommonStore( (state) => state.decreaseCount )

    return (
        <>
            <button onClick={ decreaseCount }>감소</button>
        </>
    )
}