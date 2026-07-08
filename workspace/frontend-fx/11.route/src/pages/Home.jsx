import { Link, useNavigate } from 'react-router-dom';

export default function Home() {

    const navigate = useNavigate() // navigate : javascript의 location.href와 같은 기능

    return (
        <div>
            <h1>홈</h1>
            <Link to="/products">상품 목록 보기 (링크로 이동)</Link> {/* Link : html의 <a> 태그 */}
            
            <br />
            <br />
            
            <button onClick={ () => navigate('/products') }>상품 목록 보기 (코드로 이동)</button>

            <br />
            <br />
            <Link to="/querystring?email=johndoe@example.com&phone=010-6598-7412">쿼리스트링 테스트</Link>
        </div>
    )
}