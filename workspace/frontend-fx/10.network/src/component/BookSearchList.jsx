import React from 'react';
import BookSearchItem from './BookSearchItem';
import './BookSearch.css';

// 사용자 요구사항에 명시된 형태와 어울리는 더미 데이터 3종 구성
const dummyBooks = [
  {
    title: "리액트 (반응을 바꾸면 세상이 달라진다)",
    link: "https://search.shopping.naver.com/book/catalog/32473737977",
    image: "https://shopping-phinf.pstatic.net/main_3247373/32473737977.20260122071925.jpg",
    author: "네빌고다드",
    discount: "11250",
    publisher: "서른세개의계단",
    pubdate: "20200430",
    description: "내 마음속 반응만이 바뀌었을 뿐인데 정말 외부세상도 그것에 따라 바뀔까?\n이 의문에 대한 답을 알기 위해서는 직접 해보는 수밖에 없다.\n나를 옭아매는 특정한 상황을 생각해본다. 나의 마음이 일정한 상황에 대해 똑같은 반응을 하고 있다는 것을 알 수 있다. 이제 그 반응을 바꿔본다. 이 책은 반응에 중점을 두고 강의한 것을 묶은 것이다.",
    isbn: "9788997228232"
  },
  {
    title: "모던 자바스크립트 Deep Dive",
    link: "https://search.shopping.naver.com/book/catalog/32439261271",
    image: "https://shopping-phinf.pstatic.net/main_3243926/32439261271.20230919105437.jpg",
    author: "이웅모",
    discount: "40500",
    publisher: "위키북스",
    pubdate: "20200925",
    description: "자바스크립트의 기본 개념과 동작 원리를 깊이 있게 학습하고자 하는 독자를 위한 책이다. 자바스크립트를 구성하는 거의 모든 웹 기술의 핵심을 다룬다. 웹 개발자로서 알아야 할 기본 지식들이 철저하고 상세하게 정리되어 있다.",
    isbn: "9791158392239"
  },
  {
    title: "한 입 크기로 잘라먹는 리액트(React.js)",
    link: "https://search.shopping.naver.com/book/catalog/39538356619",
    image: "https://shopping-phinf.pstatic.net/main_3953835/39538356619.20230523091218.jpg",
    author: "이정환",
    discount: "25200",
    publisher: "프로그래밍인사이트",
    pubdate: "20230522",
    description: "React 기초부터 실전 프로젝트 개발까지! 기초가 부족해도, 자바스크립트를 잘 몰라도 완독할 수 있는 가장 쉬운 입문서. 단계별 실습과 친절한 도식화를 통해 복잡한 상태 관리와 컴포넌트 라이프사이클을 알기 쉽게 해설한다.",
    isbn: "9788966263936"
  }
];

export default function BookSearchList({ books }) {
  // props로 전달받은 데이터가 없으면 dummyBooks를 사용
  const list = books && books.length > 0 ? books : dummyBooks;

  return (
    <div className="book-search-list-container">
      <div className="list-header">
        <span className="results-count">
          검색 결과 <strong className="highlight">{list.length}</strong>건
        </span>
        <div className="list-filters">
          <button className="filter-btn active">정확도순</button>
          <button className="filter-btn">최신순</button>
          <button className="filter-btn">저가순</button>
        </div>
      </div>
      
      <div className="book-grid">
        {list.map((book) => (
          <BookSearchItem key={book.isbn || book.link} book={book} />
        ))}
      </div>
    </div>
  );
}
