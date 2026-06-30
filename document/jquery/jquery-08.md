# 8장. Ajax

> 학습 목표
> - Ajax의 개념과 동작 원리를 이해한다.
> - `$.ajax()`로 GET/POST 요청을 구성하고 옵션을 제어할 수 있다.
> - `$.get()`, `$.post()`, `$.getJSON()` 단축 메서드를 상황에 맞게 사용할 수 있다.
> - `done()`, `fail()`, `always()`로 Promise 패턴 기반의 비동기 흐름을 제어할 수 있다.
> - JSON 데이터를 받아 DOM에 렌더링하고, 에러 처리와 로딩 상태 UX를 구현할 수 있다.

---

## 8.1 Ajax란?

**Ajax**(Asynchronous JavaScript and XML)는 페이지를 새로 고침하지 않고 서버와 데이터를 주고받는 기술이다. 오늘날에는 XML 대신 **JSON** 형식이 표준으로 사용되지만, 명칭은 그대로 Ajax로 부른다.

### 기존 방식 vs Ajax 방식

**기존 방식 (전체 페이지 갱신)**
```
사용자 클릭
  → 브라우저가 서버에 전체 페이지 요청
  → 서버가 HTML 전체를 응답
  → 브라우저가 페이지 전체를 다시 렌더링  ← 깜빡임 발생
```

**Ajax 방식 (부분 갱신)**
```
사용자 클릭
  → 브라우저가 서버에 데이터만 요청 (백그라운드)
  → 서버가 JSON 데이터만 응답
  → JavaScript가 필요한 부분만 DOM 갱신  ← 깜빡임 없음
```

### Ajax가 사용되는 대표적인 사례

- 검색창 자동완성 (타이핑 중 결과 미리 표시)
- 무한 스크롤 (스크롤 내릴 때 추가 데이터 로드)
- 폼 제출 후 페이지 이동 없이 결과 표시
- 좋아요·댓글 등 실시간 상호작용
- 탭 전환 시 콘텐츠 동적 로드

---

## 8.2 JSON 기초 복습

Ajax 응답 데이터의 대부분은 **JSON(JavaScript Object Notation)** 형식이다. jQuery Ajax를 사용하기 전에 JSON 구조를 다시 확인한다.

```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@example.com",
  "tags": ["admin", "editor"],
  "address": {
    "city": "서울",
    "zip": "04524"
  }
}
```

### JavaScript에서 JSON 다루기

```javascript
// JSON 문자열 → JavaScript 객체
const obj = JSON.parse('{"name":"홍길동","age":30}');
console.log(obj.name);   // "홍길동"

// JavaScript 객체 → JSON 문자열
const json = JSON.stringify({ name: '홍길동', age: 30 });
console.log(json);       // '{"name":"홍길동","age":30}'
```

> jQuery의 Ajax 메서드는 JSON 응답을 자동으로 파싱하므로 대부분의 경우 `JSON.parse()`를 직접 호출할 필요가 없다.

---

## 8.3 `$.ajax()` — 핵심 메서드

`$.ajax()`는 jQuery Ajax의 가장 기본이 되는 메서드로, 모든 옵션을 세밀하게 제어할 수 있다. `$.get()`, `$.post()` 등 단축 메서드는 모두 `$.ajax()`를 내부적으로 호출한다.

### 기본 문법

```javascript
$.ajax({
  url:      '요청 URL',
  method:   'GET',          // 'GET', 'POST', 'PUT', 'DELETE' 등
  data:     {},             // 서버로 전송할 데이터
  dataType: 'json',         // 응답 데이터 형식
  success:  function(response) { /* 성공 시 */ },
  error:    function(xhr, status, error) { /* 실패 시 */ },
  complete: function() { /* 성공/실패 무관하게 항상 */ }
});
```

---

### 주요 옵션 상세

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `url` | 현재 페이지 URL | 요청을 보낼 URL |
| `method` / `type` | `'GET'` | HTTP 메서드 (`type`은 구버전 호환용) |
| `data` | `{}` | 서버로 보낼 데이터 (객체 또는 쿼리스트링) |
| `dataType` | 자동 감지 | 응답 형식: `'json'`, `'html'`, `'text'`, `'xml'` |
| `contentType` | `'application/x-www-form-urlencoded'` | 요청 본문(body)의 콘텐츠 타입 |
| `timeout` | 없음 | 요청 제한 시간 (ms) |
| `headers` | `{}` | 추가 HTTP 헤더 |
| `async` | `true` | 비동기 여부 (`false`는 권장하지 않음) |
| `cache` | `true` (GET) | 브라우저 캐시 사용 여부 |
| `beforeSend` | — | 요청 전 실행할 함수 |

---

### GET 요청 예시

```javascript
$.ajax({
  url: 'https://jsonplaceholder.typicode.com/users/1',
  method: 'GET',
  dataType: 'json',
  success: function(user) {
    console.log(user.name);    // "Leanne Graham"
    console.log(user.email);
  },
  error: function(xhr, status, error) {
    console.error('요청 실패:', status, error);
  }
});
```

### GET 요청 — 쿼리 파라미터 전달

`data` 옵션에 객체를 전달하면 자동으로 쿼리스트링으로 변환된다.

```javascript
$.ajax({
  url: 'https://jsonplaceholder.typicode.com/posts',
  method: 'GET',
  data: {
    userId: 1,
    _limit: 5
  },
  // 실제 요청: /posts?userId=1&_limit=5
  dataType: 'json',
  success: function(posts) {
    console.log(posts.length);   // 5
  }
});
```

---

### POST 요청 예시

```javascript
$.ajax({
  url: 'https://jsonplaceholder.typicode.com/posts',
  method: 'POST',
  contentType: 'application/json',
  data: JSON.stringify({
    title: 'jQuery Ajax 학습',
    body:  '오늘은 Ajax를 배웠습니다.',
    userId: 1
  }),
  dataType: 'json',
  success: function(response) {
    console.log('생성된 ID:', response.id);
  },
  error: function(xhr) {
    console.error(xhr.status, xhr.responseText);
  }
});
```

---

### `beforeSend` — 요청 전 처리

요청이 전송되기 직전에 실행되며 로딩 표시나 버튼 비활성화에 활용한다.

```javascript
$.ajax({
  url: '/api/data',
  beforeSend: function() {
    $('#loading').show();
    $('#btn-submit').prop('disabled', true);
  },
  success: function(data) {
    renderData(data);
  },
  complete: function() {
    $('#loading').hide();
    $('#btn-submit').prop('disabled', false);
  }
});
```

---

## 8.4 단축 메서드

`$.ajax()`의 옵션 중 자주 사용되는 패턴을 간결하게 표현한 메서드들이다.

### `$.get(url, data, callback, dataType)`

GET 방식으로 데이터를 요청한다.

```javascript
// 기본 형태
$.get('https://jsonplaceholder.typicode.com/posts/1', function(data) {
  console.log(data.title);
});

// 파라미터 포함
$.get(
  'https://jsonplaceholder.typicode.com/posts',
  { userId: 1, _limit: 3 },
  function(posts) {
    posts.forEach(function(post) {
      console.log(post.title);
    });
  },
  'json'
);
```

---

### `$.post(url, data, callback, dataType)`

POST 방식으로 데이터를 전송한다.

```javascript
$.post(
  'https://jsonplaceholder.typicode.com/posts',
  { title: '새 글', body: '내용', userId: 1 },
  function(response) {
    console.log('생성 완료, ID:', response.id);
  }
);
```

---

### `$.getJSON(url, data, callback)`

GET 방식으로 JSON 데이터를 요청한다. `dataType: 'json'`이 고정된 `$.get()`과 동일하다.

```javascript
$.getJSON('https://jsonplaceholder.typicode.com/users', function(users) {
  users.forEach(function(user) {
    console.log(user.name, user.email);
  });
});

// 파라미터 포함
$.getJSON(
  'https://jsonplaceholder.typicode.com/posts',
  { userId: 2 },
  function(posts) {
    console.log(posts.length + '개의 글');
  }
);
```

---

### `.load(url, data, callback)` — HTML 조각 로드

선택한 요소 안에 서버에서 받아온 HTML을 바로 삽입한다. 부분 페이지 로드에 유용하다.

```javascript
// #content 안에 about.html 내용을 로드
$('#content').load('about.html');

// HTML 내의 특정 요소만 가져오기
$('#sidebar').load('page.html #sidebar-content');

// 콜백: 로드 완료 후 실행
$('#content').load('data.html', function(response, status) {
  if (status === 'error') {
    $(this).html('<p>로드 실패</p>');
  }
});
```

---

### 단축 메서드 비교

| 메서드 | HTTP 메서드 | dataType | 특징 |
|--------|------------|----------|------|
| `$.get()` | GET | 자동 감지 | 범용 GET 요청 |
| `$.post()` | POST | 자동 감지 | 범용 POST 요청 |
| `$.getJSON()` | GET | json 고정 | JSON 전용, 가장 간결 |
| `.load()` | GET | html 고정 | HTML 조각을 요소에 바로 삽입 |

---

## 8.5 Promise 패턴 — `done()` / `fail()` / `always()`

jQuery 1.5부터 Ajax 메서드는 **jqXHR 객체**를 반환한다. 이 객체는 Promise 인터페이스를 구현하므로 `done()`, `fail()`, `always()`를 체이닝할 수 있다.

### 기본 구조

```javascript
$.ajax({ url: '/api/data' })
  .done(function(data) {
    // 요청 성공 시 실행
    console.log('성공:', data);
  })
  .fail(function(xhr, status, error) {
    // 요청 실패 시 실행
    console.error('실패:', status, error);
  })
  .always(function() {
    // 성공/실패 무관하게 항상 실행
    $('#loading').hide();
  });
```

---

### `success` / `error` 옵션과의 차이

| 방식 | 구조 | 체이닝 | 다중 핸들러 |
|------|------|--------|-------------|
| `success` / `error` 옵션 | `$.ajax({ success: fn })` | X | X |
| `done()` / `fail()` | `.done(fn).fail(fn)` | O | O |

```javascript
// done/fail은 여러 번 체이닝 가능 (등록 순서대로 실행)
$.get('/api/users')
  .done(function(data) { renderList(data); })
  .done(function(data) { updateCounter(data.length); })
  .fail(function(xhr) { showError(xhr.status); })
  .always(function() { hideLoading(); });
```

> 두 방식은 함께 사용할 수 있다. 단, 실무에서는 일관성을 위해 한 가지 방식을 선택하는 것이 좋다. `done()` / `fail()` 패턴이 더 유연하므로 권장한다.

---

### `$.when()` — 여러 요청을 병렬 처리

여러 Ajax 요청을 동시에 보내고 **모두 완료된 뒤** 처리해야 할 때 사용한다.

```javascript
const reqUsers = $.getJSON('https://jsonplaceholder.typicode.com/users');
const reqPosts = $.getJSON('https://jsonplaceholder.typicode.com/posts?_limit=5');

$.when(reqUsers, reqPosts)
  .done(function(usersResult, postsResult) {
    // 각 결과의 첫 번째 인수가 응답 데이터
    const users = usersResult[0];
    const posts = postsResult[0];

    console.log('유저 수:', users.length);
    console.log('게시글 수:', posts.length);

    renderDashboard(users, posts);
  })
  .fail(function() {
    showError('데이터 로드에 실패했습니다.');
  });
```

---

## 8.6 JSON 데이터 렌더링

서버에서 받아온 JSON 데이터를 DOM에 표시하는 패턴을 익힌다.

### 배열 데이터를 목록으로 렌더링

```javascript
$.getJSON('https://jsonplaceholder.typicode.com/users', function(users) {
  const $list = $('#user-list').empty();

  users.forEach(function(user) {
    const $li = $('<li>').html(
      '<strong>' + user.name + '</strong> — ' + user.email
    );
    $list.append($li);
  });
});
```

---

### 템플릿 함수로 분리 — 유지보수성 향상

반복되는 HTML 구조는 함수로 분리하면 가독성이 높아진다.

```javascript
// 카드 HTML을 반환하는 템플릿 함수
function renderUserCard(user) {
  return $('<div>').addClass('user-card').html(`
    <h3>${user.name}</h3>
    <p>📧 ${user.email}</p>
    <p>🌐 ${user.website}</p>
    <p>🏢 ${user.company.name}</p>
  `);
}

$.getJSON('https://jsonplaceholder.typicode.com/users', function(users) {
  const $container = $('#user-container').empty();

  users.forEach(function(user) {
    $container.append(renderUserCard(user));
  });
});
```

---

### 단계별 렌더링 패턴

데이터 로드 전·중·후 상태를 각각 다르게 표시하는 완성된 패턴이다.

```javascript
function loadUsers() {
  const $container = $('#user-container');

  // 1. 로딩 상태 표시
  $container.html('<p class="loading">불러오는 중...</p>');

  $.getJSON('https://jsonplaceholder.typicode.com/users')
    .done(function(users) {
      // 2. 성공: 데이터 렌더링
      $container.empty();

      if (users.length === 0) {
        $container.html('<p class="empty">데이터가 없습니다.</p>');
        return;
      }

      users.forEach(function(user) {
        $container.append(renderUserCard(user));
      });
    })
    .fail(function(xhr) {
      // 3. 실패: 에러 메시지 표시
      $container.html(
        '<p class="error">오류가 발생했습니다. (' + xhr.status + ')</p>'
      );
    });
}
```

---

## 8.7 에러 처리

### HTTP 상태 코드와 에러 유형

`fail()` 또는 `error` 콜백의 인수를 활용하면 에러 유형을 구분할 수 있다.

```javascript
$.ajax({ url: '/api/data' })
  .fail(function(xhr, textStatus, errorThrown) {
    console.log('xhr.status:',      xhr.status);      // HTTP 상태 코드 (404, 500 등)
    console.log('xhr.statusText:',  xhr.statusText);  // "Not Found", "Internal Server Error"
    console.log('textStatus:',      textStatus);      // "error", "timeout", "parseerror", "abort"
    console.log('errorThrown:',     errorThrown);     // 에러 메시지 문자열
    console.log('xhr.responseText', xhr.responseText);// 서버 응답 본문
  });
```

---

### `textStatus` 값과 의미

| `textStatus` | 원인 |
|-------------|------|
| `"error"` | HTTP 오류 응답 (4xx, 5xx) |
| `"timeout"` | `timeout` 옵션 초과 |
| `"parseerror"` | 응답 데이터를 파싱할 수 없음 (JSON 형식 오류 등) |
| `"abort"` | 요청이 코드로 중단됨 |
| `"notmodified"` | 304 응답 (캐시 사용) |

---

### 에러 유형별 처리 예시

```javascript
$.ajax({
  url: '/api/data',
  timeout: 5000   // 5초 초과 시 timeout 에러
})
  .fail(function(xhr, textStatus) {
    let message;

    switch (textStatus) {
      case 'timeout':
        message = '요청 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.';
        break;
      case 'parseerror':
        message = '데이터 형식 오류가 발생했습니다.';
        break;
      case 'abort':
        message = '요청이 취소되었습니다.';
        break;
      default:
        if (xhr.status === 401) {
          message = '로그인이 필요합니다.';
          redirectToLogin();
        } else if (xhr.status === 403) {
          message = '접근 권한이 없습니다.';
        } else if (xhr.status === 404) {
          message = '요청한 데이터를 찾을 수 없습니다.';
        } else if (xhr.status >= 500) {
          message = '서버 오류가 발생했습니다. 관리자에게 문의해 주세요.';
        } else {
          message = '알 수 없는 오류가 발생했습니다.';
        }
    }

    showErrorMessage(message);
  });
```

---

### 전역 Ajax 이벤트 핸들러

모든 Ajax 요청에 공통으로 적용되는 에러 처리나 로딩 표시가 필요할 때 사용한다.

```javascript
// 모든 Ajax 요청 시작 시
$(document).on('ajaxStart', function() {
  $('#global-loading').show();
});

// 모든 Ajax 요청 완료 시 (성공/실패 무관)
$(document).on('ajaxStop', function() {
  $('#global-loading').hide();
});

// 모든 Ajax 요청 에러 시
$(document).on('ajaxError', function(event, xhr, settings, error) {
  if (xhr.status === 401) {
    window.location.href = '/login';
  }
});
```

---

## 8.8 로딩 상태 UX

사용자 경험 측면에서 비동기 요청 중 적절한 피드백을 제공하는 것은 매우 중요하다. 아래 패턴들을 상황에 맞게 활용한다.

### 패턴 1: 스피너(Spinner)

```html
<div id="spinner" style="display:none;">
  <div class="spinner-circle"></div>
  <p>불러오는 중...</p>
</div>
<div id="content"></div>
```

```javascript
$.ajax({ url: '/api/data' })
  .beforeSend(function() {    // ← beforeSend는 옵션으로만 사용 가능
    // 아래처럼 별도로 처리
  });

// 올바른 방법
const req = $.ajax({ url: '/api/data' });
$('#spinner').show();

req
  .done(function(data) { renderData(data); })
  .fail(function() { showError(); })
  .always(function() { $('#spinner').hide(); });
```

---

### 패턴 2: 버튼 비활성화 + 텍스트 변경

```javascript
$('#btn-load').on('click', function() {
  const $btn = $(this);

  $btn.prop('disabled', true).text('불러오는 중...');

  $.getJSON('/api/data')
    .done(function(data) { renderData(data); })
    .fail(function() { showError(); })
    .always(function() {
      $btn.prop('disabled', false).text('데이터 불러오기');
    });
});
```

---

### 패턴 3: 스켈레톤 UI

실제 콘텐츠와 비슷한 형태의 회색 블록을 먼저 보여주어 레이아웃 이동(layout shift)을 최소화한다.

```javascript
function showSkeleton(count) {
  const $container = $('#card-container').empty();
  for (let i = 0; i < count; i++) {
    $container.append(`
      <div class="card skeleton">
        <div class="skeleton-title"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line short"></div>
      </div>
    `);
  }
}

function loadCards() {
  showSkeleton(6);   // 6개의 스켈레톤 카드 표시

  $.getJSON('/api/cards')
    .done(function(cards) {
      $('#card-container').empty();
      cards.forEach(function(card) {
        $('#card-container').append(renderCard(card));
      });
    })
    .fail(function() {
      $('#card-container').html('<p class="error">로드 실패</p>');
    });
}
```

---

### 패턴 4: 무한 스크롤

```javascript
let page = 1;
let isLoading = false;

$(window).on('scroll', function() {
  const scrollBottom = $(document).height() - $(window).scrollTop() - $(window).height();

  if (scrollBottom < 200 && !isLoading) {
    loadMore();
  }
});

function loadMore() {
  isLoading = true;
  $('#load-more-spinner').show();

  $.getJSON('/api/posts', { page: page, limit: 10 })
    .done(function(posts) {
      if (posts.length === 0) {
        $(window).off('scroll');   // 더 이상 데이터 없으면 이벤트 해제
        $('#end-message').show();
        return;
      }

      posts.forEach(function(post) {
        $('#post-list').append(renderPost(post));
      });
      page++;
    })
    .fail(function() { showError(); })
    .always(function() {
      isLoading = false;
      $('#load-more-spinner').hide();
    });
}
```

---

## 8.9 실무 패턴 — 공개 API 연동 실습

실제로 사용할 수 있는 공개 API와 연동하는 완성 예제다. `jsonplaceholder.typicode.com`을 활용한다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>Ajax 실습 — 사용자 목록</title>
  <style>
    body { font-family: sans-serif; max-width: 700px; margin: 40px auto; padding: 0 16px; }

    #search-box {
      width: 100%; padding: 10px 14px; border: 2px solid #ddd;
      border-radius: 8px; font-size: 1rem; margin-bottom: 16px;
    }
    #search-box:focus { border-color: #4f6ef7; outline: none; }

    .user-card {
      border: 1px solid #eee; border-radius: 8px;
      padding: 16px; margin-bottom: 12px;
      transition: box-shadow 0.2s;
    }
    .user-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
    .user-card h3 { margin: 0 0 6px; color: #333; }
    .user-card p  { margin: 3px 0; font-size: 0.88rem; color: #666; }

    #status-msg {
      text-align: center; padding: 40px;
      color: #aaa; font-size: 0.95rem;
    }
    #status-msg.error { color: #e74c3c; }

    #btn-reload {
      display: block; margin: 16px auto;
      padding: 10px 24px; background: #4f6ef7;
      color: #fff; border: none; border-radius: 8px;
      font-size: 0.95rem; cursor: pointer;
    }
    #btn-reload:hover { background: #3a5be0; }
    #btn-reload:disabled { opacity: 0.5; cursor: default; }
  </style>
</head>
<body>

  <h2>👥 사용자 목록</h2>
  <input type="text" id="search-box" placeholder="이름으로 검색...">
  <button id="btn-reload">새로고침</button>

  <div id="user-container">
    <p id="status-msg">버튼을 눌러 데이터를 불러오세요.</p>
  </div>

  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script>
    $(function () {

      let allUsers = [];   // 전체 데이터 캐시

      /* ── 카드 렌더링 함수 ── */
      function renderCard(user) {
        return $('<div>').addClass('user-card').html(`
          <h3>${user.name}</h3>
          <p>📧 ${user.email}</p>
          <p>📞 ${user.phone}</p>
          <p>🌐 ${user.website}</p>
          <p>🏢 ${user.company.name}</p>
        `);
      }

      /* ── 목록 표시 함수 ── */
      function renderList(users) {
        const $container = $('#user-container').empty();

        if (users.length === 0) {
          $container.html('<p id="status-msg">검색 결과가 없습니다.</p>');
          return;
        }

        users.forEach(function (user) {
          $container.append(renderCard(user));
        });
      }

      /* ── 데이터 로드 함수 ── */
      function loadUsers() {
        const $btn = $('#btn-reload');
        $btn.prop('disabled', true).text('불러오는 중...');
        $('#user-container').html('<p id="status-msg">불러오는 중...</p>');
        $('#search-box').val('');

        $.getJSON('https://jsonplaceholder.typicode.com/users')
          .done(function (users) {
            allUsers = users;
            renderList(allUsers);
          })
          .fail(function (xhr, textStatus) {
            let msg = '데이터를 불러오지 못했습니다.';
            if (textStatus === 'timeout') msg = '요청 시간이 초과되었습니다.';
            $('#user-container').html(
              '<p id="status-msg" class="error">' + msg + '</p>'
            );
          })
          .always(function () {
            $btn.prop('disabled', false).text('새로고침');
          });
      }

      /* ── 실시간 검색 필터 ── */
      $('#search-box').on('input', function () {
        const keyword = $(this).val().toLowerCase();
        const filtered = allUsers.filter(function (user) {
          return user.name.toLowerCase().includes(keyword);
        });
        renderList(filtered);
      });

      /* ── 초기 로드 ── */
      $('#btn-reload').on('click', loadUsers);
      loadUsers();

    });
  </script>
</body>
</html>
```

---

## 8.10 `$.ajax()` 고급 옵션

### 요청 취소 (abort)

진행 중인 Ajax 요청을 취소할 수 있다. 검색창처럼 이전 요청이 불필요해지는 경우에 활용한다.

```javascript
let currentRequest = null;

$('#search-input').on('input', function () {
  const keyword = $(this).val().trim();

  // 이전 요청이 진행 중이면 취소
  if (currentRequest) {
    currentRequest.abort();
  }

  if (!keyword) return;

  currentRequest = $.getJSON('/api/search', { q: keyword })
    .done(function (results) {
      renderSearchResults(results);
    })
    .fail(function (xhr, textStatus) {
      if (textStatus !== 'abort') {   // abort는 에러가 아님
        showError();
      }
    })
    .always(function () {
      currentRequest = null;
    });
});
```

---

### 디바운스(Debounce) 적용 — 검색 최적화

사용자가 타이핑을 멈춘 뒤 일정 시간 후에만 요청을 보내 불필요한 API 호출을 줄인다.

```javascript
let searchTimer = null;

$('#search-input').on('input', function () {
  clearTimeout(searchTimer);

  const keyword = $(this).val().trim();
  if (!keyword) {
    $('#result').empty();
    return;
  }

  searchTimer = setTimeout(function () {
    $.getJSON('/api/search', { q: keyword })
      .done(function (results) { renderResults(results); })
      .fail(function () { showError(); });
  }, 300);   // 타이핑 멈춘 후 300ms 뒤에 요청
});
```

---

### 공통 설정 — `$.ajaxSetup()`

모든 Ajax 요청에 공통으로 적용할 기본값을 설정한다.

```javascript
$.ajaxSetup({
  dataType: 'json',
  timeout:  10000,
  headers: {
    'Authorization': 'Bearer ' + getToken(),
    'X-Requested-With': 'XMLHttpRequest'
  }
});

// 이후 모든 요청에 위 설정이 자동 적용됨
$.get('/api/users').done(function(users) { /* ... */ });
$.post('/api/posts', data).done(function(res) { /* ... */ });
```

---

## 8.11 정리

### Ajax 메서드 비교

| 메서드 | 방식 | 특징 |
|--------|------|------|
| `$.ajax()` | 설정에 따름 | 모든 옵션 제어 가능, 가장 유연 |
| `$.get()` | GET | 범용 GET 요청 |
| `$.post()` | POST | 범용 POST 요청 |
| `$.getJSON()` | GET | JSON 응답 전용, 가장 간결 |
| `.load()` | GET | HTML 조각을 요소에 직접 삽입 |

### Promise 메서드

| 메서드 | 실행 시점 | 비고 |
|--------|-----------|------|
| `.done(fn)` | 요청 성공 시 | 여러 번 체이닝 가능 |
| `.fail(fn)` | 요청 실패 시 | `xhr`, `textStatus`, `error` 인수 |
| `.always(fn)` | 항상 | 로딩 해제 등 공통 처리 |
| `$.when()` | 모든 요청 완료 시 | 병렬 요청 처리 |

### 에러 처리 핵심

| `textStatus` | 원인 | 대응 |
|-------------|------|------|
| `"error"` | HTTP 오류 | `xhr.status`로 코드 분기 |
| `"timeout"` | 시간 초과 | 재시도 유도 |
| `"parseerror"` | JSON 파싱 실패 | 서버 응답 확인 |
| `"abort"` | 요청 취소 | 무시 처리 |

### 로딩 UX 패턴

| 패턴 | 적합한 상황 |
|------|-------------|
| 스피너 | 전체 영역 대기 |
| 버튼 비활성화 | 폼 제출, 단순 조회 |
| 스켈레톤 UI | 카드·목록 형태 콘텐츠 |
| 무한 스크롤 | 대량 목록 데이터 |

---

## 연습 문제

1. `https://jsonplaceholder.typicode.com/posts?userId=1` 에서 데이터를 가져와 `<ul>` 안에 제목(`title`)만 `<li>`로 렌더링하는 코드를 `$.getJSON()`으로 작성하라.

2. 위 1번 코드를 `$.ajax()`로 변환하되, `timeout: 5000` 옵션을 추가하고 timeout 에러 시 "요청 시간 초과" 메시지를 표시하도록 에러 처리를 구현하라.

3. 버튼 클릭 시 `https://jsonplaceholder.typicode.com/users`와 `https://jsonplaceholder.typicode.com/todos?_limit=5`를 동시에 요청하고, 두 요청이 모두 완료된 뒤 결과를 각각 다른 `<div>`에 렌더링하는 코드를 `$.when()`으로 작성하라.

4. 텍스트 입력 필드에 이름을 입력하면 `https://jsonplaceholder.typicode.com/users`에서 전체 사용자를 가져온 뒤 클라이언트에서 이름으로 필터링하여 표시하는 실시간 검색을 구현하라. 디바운스(300ms)를 적용하라.

5. `$.ajaxSetup()`으로 `dataType: 'json'`과 `timeout: 8000`을 전역 기본값으로 설정한 뒤, 이후 `$.get()`으로 데이터를 요청하는 코드를 작성하라. 설정이 실제로 적용되는지 확인하는 방법도 설명하라.

---

> **다음 장 예고**  
> 9장에서는 **폼 처리와 유효성 검사**를 학습한다. `serialize()`, `serializeArray()`로 폼 데이터를 수집하고, 실시간 유효성 검사 패턴을 구현한 뒤 Ajax와 연동하여 폼을 제출하는 전체 흐름을 익힌다.
