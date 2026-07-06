export default function IncreaseBtn({setCount}) {
    return (
        <>
            <button onClick={ 
                () => {
                    // setCount(15)
                    setCount((v) => v + 1)
                } 
            }>증가</button>
        </>
    )
}