export default function DecreaseBtn({setCount}) {
    return (
        <>
            <button onClick={
                () => setCount((prevStateValue) => prevStateValue - 1)
            }>감소</button>
        </>
    )
}