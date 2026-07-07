import { useState } from "react";
import BookSearchList from "./BookSearchList";
import "./BookSearch.css";
import axios from "axios";

export default function BookSearch() {
  const [query, setQuery] = useState("리액트");  

  const [books, setBooks] = useState([])

  const searchBook = async (e) => {
      e.preventDefault();
      //const url = '
      const url = '/naver/v1/search/book.json'
      const display = 100
      const clientId = 'KgCpqDKbul3ShRt8akcP'
      const clientSecret = 'UGVK7Mn2ix'

      //비동기 방식 호출
      // axios.get ( `${url}?query=${query}&display=${display}`,
      //     { headers: {'X-Naver-Client-Id': clientId, 'X-Naver-Client-Secre': clientSecret}} )
      //     .then((response)=> console.log())

      //동기 방식 호출 : await 비동기함수 호출 (( await는 async 함수 내부에서만 사용할 수 있습니다. )
      const response = await axios.get( `${url}?query=${query}&display=${display}`,
                                      { headers: {'X-Naver-Client-Id': clientId, 'X-Naver-Client-Secret': clientSecret}} )

      console.log(response.data)
      setBooks(response.data.items) // 도서 검색 결과를 state에 저장
  }

  return (
    <div className="book-search-page">
      <header className="search-header">
        <h1>
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
          Naver 도서 검색
        </h1>
        <p>네이버 Open API를 이용한 스마트한 도서 검색 서비스</p>
      </header>

      <form className="search-bar-container" onSubmit={searchBook}>
        <div className="search-input-wrapper">
          <svg className="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            type="text"
            className="search-input"
            placeholder="검색어를 입력하세요 (예: 리액트)"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
        <button type="submit" className="search-btn">
          검색
        </button>
      </form>

      <BookSearchList books={books} />
    </div>
  );
}
