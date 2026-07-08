import { Link, useParams } from 'react-router-dom';

export default function ProductDetail() {

    // const {id} = useParams() // 경로에 포함된 데이터 읽기
    const params = useParams()

    return (
        <div>
            <Link to="/">홈</Link>
            <br/>
            <Link to="/products">상품 목록 보기</Link>

            <hr />
            {/* <h1>{id}번 상품의 상세 정보</h1> */}
            <h1>{params.id}번 상품의 상세 정보</h1>
            
        </div>
    )
}