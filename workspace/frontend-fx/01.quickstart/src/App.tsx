import './App.css'

function App() {

  // 연습 1.
  // const userName = "홍길동";
  // const isLoggedIn = true;

  // return (
  //   /* 자바스크립트 주석 */
  //   <div className="container">
  //     {/* 리액트 주석 */}
  //     <h1>안녕하세요, {userName}님!</h1>
  //     {isLoggedIn ? <p>로그인 상태입니다.</p> : <p>로그인이 필요합니다.</p>}
  //   </div>
  // );

  // 연습 2.
  const name = '홍길동';
  const intro = 'React를 처음 배우는 프론트엔드 개발자입니다. 잘 부탁드립니다!';
  const hobbies = ['독서', '러닝', '코딩 테스트 풀기', '영화 감상'];
  const isPresent = true; // 출석 여부

  return (
    <div className="container">
      <div className="profile-photo">사진</div>

      <h1>{name}</h1>

      <p>
        오늘의 상태:{' '}
        {
          isPresent ? ( <span className="status present">출석</span> ) : ( <span className="status absent">결석</span> )
        }
      </p>

      <h2>소개</h2>
      <p>{intro}</p>

      <h2>좋아하는 것</h2>
      <ul className="hobby-list">
        {
          hobbies.map(
            (hobby, index) => ( <li key={index}>{hobby}</li> )
          )
        }
      </ul>
    </div>
  );

}

export default App
