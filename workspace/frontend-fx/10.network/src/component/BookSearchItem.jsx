import React from 'react';
import './BookSearch.css';

export default function BookSearchItem({ book }) {
  if (!book) return null;

  // 가격 포맷팅
  const formatPrice = (price) => {
    if (!price) return '판매가 정보 없음';
    return Number(price).toLocaleString() + '원';
  };

  // 날짜 포맷팅 (YYYYMMDD -> YYYY.MM.DD)
  const formatDate = (dateStr) => {
    if (!dateStr || dateStr.length !== 8) return dateStr;
    return `${dateStr.substring(0, 4)}.${dateStr.substring(4, 6)}.${dateStr.substring(6, 8)}`;
  };

  return (
    <div className="book-item">
      <div className="book-image-container">
        <img src={book.image} alt={book.title} className="book-image" />
      </div>
      <div className="book-info">
        <h3 className="book-title" title={book.title}>
          <a href={book.link} target="_blank" rel="noopener noreferrer">
            {book.title}
          </a>
        </h3>
        
        <div className="book-meta">
          <span className="book-author">{book.author}</span>
          <span className="separator">|</span>
          <span className="book-publisher">{book.publisher}</span>
          <span className="separator">|</span>
          <span className="book-date">{formatDate(book.pubdate)}</span>
        </div>

        <p className="book-description">{book.description}</p>
        
        <div className="book-footer">
          <div className="book-price-container">
            <span className="price-label">할인가</span>
            <span className="book-price">{formatPrice(book.discount)}</span>
          </div>
          <a href={book.link} target="_blank" rel="noopener noreferrer" className="book-link-btn">
            네이버 도서
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
              <polyline points="15 3 21 3 21 9"></polyline>
              <line x1="10" y1="14" x2="21" y2="3"></line>
            </svg>
          </a>
        </div>
      </div>
    </div>
  );
}
