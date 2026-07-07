import { create } from "zustand"

// create 함수의 전달인자 : 함수(get, set) -> get, set : 관리하는 데이터에 대한 수정, 읽기 함수
export const useCommonStore = create( (set, get) => ({

    count: 0, 
    name: "John Doe", 
    email : "johndoe@example",

    increaseCount : () => set( (state) => ({ count: state.count + 1 }) ),
    decreaseCount : () => set( (state) =>  ({ count: state.count - 1 }) )

}))