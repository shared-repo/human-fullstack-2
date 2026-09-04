// src/main/resources/static/js/api.js

// 페이지 로드 시 메타 태그에서 CSRF 정보 읽기 (10.4에서 추가되는 메타 태그)
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

/**
 * CSRF 토큰이 포함된 Fetch 공통 래퍼
 * - POST · PUT · DELETE 요청에 자동으로 CSRF 헤더 추가
 * - 204 No Content 응답은 null 반환
 * - 오류 응답은 서버 메시지로 예외 throw
 */
async function apiRequest(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    // 상태를 변경하는 요청에만 CSRF 헤더 추가
    const method = (options.method || 'GET').toUpperCase();
    if (method !== 'GET' && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    const response = await fetch(url, { ...options, headers });

    // 204 No Content — 본문 없이 성공
    if (response.status === 204) return null;

    const data = await response.json(); // JSON 문자열 형식의 응답을 JSON 객체로 변환

    if (!response.ok) {
        throw new Error(data.message || `서버 오류 (${response.status})`);
    }

    return data;
}

/**
 * 디바운스 유틸리티 (10.7 자동완성에서 사용)
 * 마지막 호출 이후 delay(ms)가 지나야 fn을 실행
 */
function debounce(fn, delay) {
    let timerId;
    return function (...args) {
        clearTimeout(timerId);
        timerId = setTimeout(() => fn.apply(this, args), delay);
    };
}
