import { create } from "zustand"
import { persist } from "zustand/middleware"
import { immer } from "zustand/middleware/immer"

export const useTodoStore = create( 
    // (set, get) => ({

    //     todos: [
    //         // { id: 1, text: "할 일 1", done: false },
    //         // { id: 2, text: "할 일 2", done: true },
    //         // { id: 3, text: "할 일 3", done: true },
    //         // { id: 4, text: "할 일 4", done: false },
    //         // { id: 5, text: "할 일 5", done: false }
    //     ],

    //     //addTodo: () => { ... }
    //     // addTodo: (text) => set( (state) => ({ todos: [...state.todos, { id: new Date().getTime(), text: text, done: false } ] }) )
    //     addTodo: function(text) {
    //         set( 
    //             function(state) {                    
    //                 return  {
    //                             todos: [...state.todos, 
    //                                     { id: new Date().getTime(), text: text, done: false } ] 
    //                         }
    //             } 
    //         )    
    //     },
    //     // deleteTodo: (id) => set( (state) => ({ todos: state.todos.filter( (todo) => todo.id !== id ) }) ),
    //     deleteTodo: function(id) {
    //         set(
    //             function(state) {
    //                 return { todos: state.todos.filter( (todo) => todo.id !== id ) }
    //             }
    //         )
    //     },
    //     updateTodo: function(id) {
    //         set(
    //             function(state) {
    //                 return { todos: state.todos.map( (todo) => todo.id !== id ? todo : { ...todo, done:!todo.done } ) }
    //             }
    //         )
    //     }

    // })

    /////////////////////////////////

    // immer( // 불변성 지원 : 상태를 직접 변경할 수 있도록 도와주는 미들웨어
    //         (set) => ({
    //             todos: [],
    //             addTodo: (text) => set((state) => { 
    //                 state.todos.push({ 
    //                     id: new Date().getTime(), 
    //                     text: text, 
    //                     done: false 
    //                 }); // end of push
    //             }), // end of set
    //             updateTodo: (id) => set( (state) => {
    //                 state.todos = state.todos.map( 
    //                     (todo) => todo.id === id ? { ...todo, done:!todo.done } : todo
    //                 );
    //             } ),
    //             deleteTodo: (id) => set( (state) => { 
    //                 state.todos = state.todos.filter( (todo) => todo.id !== id );
    //             })
    //         })
    //     )

    persist( // 브라우저의 로컬 저장소에 데이터 저장, 관리하는 미들웨어
        immer( // 불변성 지원 : 상태를 직접 변경할 수 있도록 도와주는 미들웨어
            (set) => ({
                todos: [],
                addTodo: (text) => set((state) => {
                    state.todos.push({ 
                        id: new Date().getTime(), 
                        text: text, 
                        done: false 
                    }); // end of push
                }), // end of set
                updateTodo: (id) => set( (state) => {
                    state.todos = state.todos.map( 
                        (todo) => todo.id === id ? { ...todo, done:!todo.done } : todo
                    );
                } ),
                deleteTodo: (id) => set( (state) => { 
                    state.todos = state.todos.filter( (todo) => todo.id !== id );
                })
            })
        ),
        { name: "todos" } // 브라우저 저장소에 데이터를 저장할 때 사용하는 이름

    )    

    
)