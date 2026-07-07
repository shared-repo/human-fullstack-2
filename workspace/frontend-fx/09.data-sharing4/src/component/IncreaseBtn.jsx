import { useCommonStore } from "../store/CommonStore"

export default function IncreaseBtn() {

    const increaseCount = useCommonStore( (state) => state.increaseCount )

    return (
        <>
            <button onClick={ increaseCount }>증가</button>
        </>
    )
}