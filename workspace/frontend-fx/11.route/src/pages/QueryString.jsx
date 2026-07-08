import { Link, useSearchParams } from 'react-router-dom';

export default function QueryString() {

    const [ searchParams ] = useSearchParams(); // query-string data 읽기 도구 가져오기 ( url?data1=v1&data2=v2... )

    return (
        <div>
            <Link to="/">홈</Link>            

            <hr />
            
            <h1>{searchParams.get('email')}</h1>
            <h1>{searchParams.get('phone')}</h1>
            
        </div>
    )
}