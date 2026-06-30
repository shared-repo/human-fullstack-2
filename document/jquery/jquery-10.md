# 10장. 성능 최적화와 모범 사례

> 학습 목표
> - 선택자 캐싱으로 불필요한 DOM 탐색을 줄일 수 있다.
> - DOM 접근을 최소화하는 다양한 최적화 기법을 적용할 수 있다.
> - 상황에 따라 jQuery와 바닐라 JS를 적절히 선택하는 기준을 설명할 수 있다.
> - jQuery와 ES6+ 문법을 함께 사용하는 실무 패턴을 익힌다.

---

## 10.1 왜 성능을 고려해야 하는가?

jQuery는 편리하지만, 잘못 사용하면 불필요한 연산이 누적되어 페이지가 느려진다. 특히 다음 상황에서 성능 문제가 드러난다.

- 수백~수천 개의 DOM 요소를 반복 조작할 때
- 스크롤·마우스 이동처럼 초당 수십 번 발생하는 이벤트 안에서 DOM을 조작할 때
- 복잡한 선택자를 루프 안에서 반복 실행할 때

이 장에서는 jQuery 코드를 작성할 때 습관적으로 적용할 수 있는 최적화 기법과 판단 기준을 다룬다.

---

## 10.2 선택자 캐싱

### 문제: 같은 요소를 반복 탐색

`$('#id')` 또는 `$('.class')`를 호출할 때마다 jQuery는 DOM 트리 전체를 탐색하여 요소를 찾는다. 같은 선택자를 여러 번 사용하면 그만큼 탐색이 반복된다.

```javascript
// 나쁜 예: #list를 세 번 탐색
$('#list').addClass('active');
$('#list').find('li').css('color', 'blue');
$('#list').show();
```

---

### 해결: 변수에 저장하여 재사용

한 번 선택한 요소를 변수에 저장하면 이후에는 탐색 없이 바로 사용할 수 있다.

```javascript
// 좋은 예: 한 번 탐색하고 캐싱
const $list = $('#list');
$list.addClass('active');
$list.find('li').css('color', 'blue');
$list.show();
```

---

### 루프 안에서의 캐싱

루프 내부에서 선택자를 반복 호출하는 것은 특히 성능에 나쁜 영향을 준다.

```javascript
// 나쁜 예: 루프 100번마다 DOM 탐색 발생
for (let i = 0; i < 100; i++) {
  $('#result').append('<li>' + i + '</li>');
}

// 좋은 예: 루프 밖에서 캐싱
const $result = $('#result');
for (let i = 0; i < 100; i++) {
  $result.append('<li>' + i + '</li>');
}
```

---

### 캐싱 변수 네이밍 규칙

jQuery 객체를 담는 변수는 `$` 접두사를 붙이는 것이 관례다. 일반 DOM 요소나 다른 값과 쉽게 구분된다.

```javascript
const $header   = $('#header');          // jQuery 객체
const $navItems = $('.nav-item');        // jQuery 객체 (복수형)
const headerEl  = document.getElementById('header');  // 일반 DOM 요소
const itemCount = $navItems.length;     // 숫자값
```

---

### 탐색 범위 좁히기

전체 문서에서 탐색하는 대신, 미리 캐싱한 부모 요소 안에서 `find()`로 탐색하면 탐색 범위가 좁아져 빠르다.

```javascript
// 느림: 전체 DOM에서 .btn 탐색
$('.btn').on('click', handler);

// 빠름: 특정 컨테이너 안에서만 탐색
const $form = $('#main-form');
$form.find('.btn').on('click', handler);
```

---

## 10.3 DOM 접근 최소화

DOM 조작은 브라우저에서 가장 비용이 큰 작업 중 하나다. DOM에 접근하는 횟수 자체를 줄이는 것이 핵심이다.

### 문자열 축적 후 한 번에 삽입

루프에서 요소를 하나씩 `append()`하면 DOM을 매번 갱신하므로 느리다. 문자열이나 배열에 축적한 뒤 한 번에 삽입한다.

```javascript
const items = ['사과', '바나나', '체리', '딸기', '포도'];

// 나쁜 예: DOM을 5번 갱신
items.forEach(function(item) {
  $('#fruit-list').append('<li>' + item + '</li>');
});

// 좋은 예 1: 문자열 축적 후 한 번에 삽입
let html = '';
items.forEach(function(item) {
  html += '<li>' + item + '</li>';
});
$('#fruit-list').html(html);

// 좋은 예 2: 배열 join 활용
const html2 = items.map(function(item) {
  return '<li>' + item + '</li>';
}).join('');
$('#fruit-list').html(html2);
```

---

### DocumentFragment 활용

복잡한 요소 구조를 생성할 때는 `DocumentFragment`에 먼저 구성한 뒤 한 번에 삽입한다.

```javascript
const fragment = document.createDocumentFragment();

for (let i = 1; i <= 100; i++) {
  const $li = $('<li>').text('항목 ' + i).addClass('list-item');
  fragment.appendChild($li[0]);   // jQuery 객체를 DOM 요소로 변환
}

$('#big-list').append(fragment);  // DOM은 한 번만 갱신
```

---

### `detach()`로 분리 후 작업

대량의 DOM 조작이 필요할 때 요소를 DOM에서 분리하고 작업한 뒤 다시 삽입하면 레이아웃 재계산 횟수를 줄일 수 있다.

```javascript
// 1. 요소를 DOM에서 분리 (이벤트 핸들러 유지)
const $table = $('#data-table').detach();

// 2. 분리된 상태에서 대량 조작 (레이아웃 재계산 없음)
$table.find('tr').each(function(i) {
  $(this).find('td').first().text(i + 1);
});

// 3. 원래 위치에 다시 삽입
$('#table-container').append($table);
```

---

### 읽기와 쓰기를 분리

DOM에서 값을 읽고 쓰는 작업이 교차하면 브라우저가 매번 레이아웃을 재계산(reflow)한다. 읽기를 먼저 모두 완료한 뒤 쓰기를 일괄 수행한다.

```javascript
// 나쁜 예: 읽기·쓰기가 교차 → 매번 reflow
$('#box1').width($('#box1').width() + 10);
$('#box2').width($('#box2').width() + 10);
$('#box3').width($('#box3').width() + 10);

// 좋은 예: 읽기 먼저 → 쓰기 일괄
const w1 = $('#box1').width();
const w2 = $('#box2').width();
const w3 = $('#box3').width();
$('#box1').width(w1 + 10);
$('#box2').width(w2 + 10);
$('#box3').width(w3 + 10);
```

---

### 스로틀(Throttle)과 디바운스(Debounce)

`scroll`, `resize`, `mousemove`처럼 빠르게 반복 발생하는 이벤트 안에서 DOM을 조작하면 프레임 드롭이 발생할 수 있다.

**디바운스(Debounce):** 마지막 이벤트 발생 후 일정 시간이 지난 뒤 한 번만 실행한다.

```javascript
// 직접 구현한 debounce
function debounce(fn, delay) {
  let timer;
  return function() {
    clearTimeout(timer);
    timer = setTimeout(fn.bind(this, ...arguments), delay);
  };
}

// resize 이벤트에 debounce 적용
$(window).on('resize', debounce(function() {
  adjustLayout();
}, 200));
```

**스로틀(Throttle):** 일정 시간 간격으로 최대 한 번만 실행한다.

```javascript
function throttle(fn, interval) {
  let last = 0;
  return function() {
    const now = Date.now();
    if (now - last >= interval) {
      last = now;
      fn.apply(this, arguments);
    }
  };
}

// scroll 이벤트에 throttle 적용 (100ms마다 최대 1회 실행)
$(window).on('scroll', throttle(function() {
  updateProgressBar();
}, 100));
```

| 기법 | 실행 방식 | 적합한 이벤트 |
|------|-----------|---------------|
| 디바운스 | 멈춘 뒤 1회 | resize, input(검색) |
| 스로틀 | 주기적으로 1회 | scroll, mousemove |

---

### 이벤트 위임으로 핸들러 수 줄이기

동일한 이벤트를 가진 요소가 많을 때 각각 핸들러를 등록하면 메모리 사용량이 늘어난다. 이벤트 위임으로 핸들러 하나로 처리한다.

```javascript
// 나쁜 예: 요소마다 핸들러 등록 (요소가 1000개면 핸들러도 1000개)
$('tr').on('click', function() {
  $(this).toggleClass('selected');
});

// 좋은 예: 부모 하나에 위임 (핸들러 1개)
$('#data-table').on('click', 'tr', function() {
  $(this).toggleClass('selected');
});
```

---

## 10.4 선택자 최적화

### ID 선택자를 최우선으로

ID 선택자(`#id`)는 `document.getElementById()`를 내부적으로 사용하여 가장 빠르다.

```javascript
// 빠름 (ID)
$('#submit-btn').click();

// 느림 (클래스)
$('.submit-btn').click();

// 더 느림 (속성)
$('[type="submit"]').click();
```

---

### 오른쪽에서 왼쪽으로 해석되는 선택자

jQuery 선택자 엔진(Sizzle)은 **오른쪽에서 왼쪽**으로 요소를 탐색한다. 오른쪽 선택자가 구체적일수록 후보 요소가 줄어 탐색이 빠르다.

```javascript
// 느림: 오른쪽이 div → 전체 div를 먼저 탐색
$('.sidebar div');

// 빠름: 오른쪽이 .widget → .widget만 먼저 탐색
$('.sidebar .widget');
```

---

### jQuery 확장 선택자 주의

`:eq`, `:odd`, `:even` 등 jQuery 전용 확장 선택자는 CSS 엔진 최적화를 받지 못한다. `filter()` 메서드와 조합하면 속도를 개선할 수 있다.

```javascript
// 느림: jQuery 확장 선택자
$('li:even');

// 빠름: CSS 엔진 선택 후 filter
$('li').filter(':even');

// 가장 빠름: CSS 표준 의사 클래스 사용
$('li:nth-child(odd)');
```

---

### 불필요하게 넓은 선택자 피하기

```javascript
// 나쁜 예: 전체 DOM에서 p 탐색
$('p').addClass('highlight');

// 좋은 예: 범위를 명시적으로 한정
$('#article p').addClass('highlight');
// 또는
$('#article').find('p').addClass('highlight');
```

---

## 10.5 코드 구조와 가독성 모범 사례

### 초기화 코드 구조화

`$(function() { ... })` 안의 코드가 길어지면 역할별로 함수로 분리한다.

```javascript
$(function() {
  init();
});

function init() {
  bindEvents();
  loadInitialData();
  setupUI();
}

function bindEvents() {
  $('#btn-add').on('click', handleAdd);
  $('#btn-del').on('click', handleDelete);
  $('#search').on('input', debounce(handleSearch, 300));
}

function loadInitialData() {
  $.getJSON('/api/items').done(renderList);
}

function setupUI() {
  $('#modal').hide();
  updateCounter();
}
```

---

### 매직 넘버 상수로 분리

의미 없는 숫자 리터럴을 상수로 분리하면 의도가 명확해지고 수정이 쉬워진다.

```javascript
// 나쁜 예
$(window).on('scroll', function() {
  if ($(this).scrollTop() > 300) {
    $('#btn-top').fadeIn(200);
  }
});

// 좋은 예
const SCROLL_THRESHOLD = 300;
const FADE_DURATION    = 200;

$(window).on('scroll', function() {
  if ($(this).scrollTop() > SCROLL_THRESHOLD) {
    $('#btn-top').fadeIn(FADE_DURATION);
  }
});
```

---

### 체이닝은 3단계 이내로

메서드 체이닝은 jQuery의 강점이지만, 너무 길어지면 디버깅이 어렵다.

```javascript
// 과도한 체이닝 — 어느 단계에서 오류인지 파악 어려움
$('#list').find('li').filter('.active').first().css('color','red').addClass('highlight').show().parent().css('border','1px solid red');

// 적절한 분리
const $list    = $('#list');
const $active  = $list.find('li.active');
const $first   = $active.first();

$first.css('color', 'red').addClass('highlight').show();
$list.css('border', '1px solid red');
```

---

### `data()` 메서드로 DOM에 데이터 연결

DOM 요소에 연관 데이터를 저장할 때 전역 변수나 `attr()`보다 jQuery의 `data()` 메서드를 사용하면 메모리 관리가 쉽다.

```javascript
// 나쁜 예: 전역 변수나 attribute에 저장
window.selectedId = 5;
$('#item').attr('data-selected-id', 5);   // HTML에 노출됨

// 좋은 예: jQuery data()에 저장 (HTML에 노출되지 않음)
$('#item').data('selectedId', 5);
const id = $('#item').data('selectedId');  // 5
```

> `data()` 메서드는 HTML의 `data-*` 속성도 읽을 수 있다. 단, `data()`로 설정한 값은 HTML `data-*`를 변경하지 않는다.

```html
<div id="card" data-user-id="42" data-role="admin"></div>
```

```javascript
$('#card').data('userId');  // 42  (camelCase로 읽음)
$('#card').data('role');    // "admin"
```

---

## 10.6 jQuery vs 바닐라 JS 선택 기준

브라우저 표준화가 진행되면서 바닐라 JS만으로도 jQuery가 해결하던 많은 문제를 처리할 수 있게 되었다. 상황에 맞는 선택 기준을 이해한다.

### 기능별 비교

| 기능 | jQuery | 바닐라 JS (모던) |
|------|--------|-----------------|
| DOM 선택 | `$('.item')` | `document.querySelectorAll('.item')` |
| 클래스 토글 | `toggleClass()` | `el.classList.toggle()` |
| Ajax | `$.ajax()` | `fetch()` |
| 이벤트 | `on()` | `addEventListener()` |
| 반복 처리 | `each()` | `forEach()`, `map()` |
| DOM 생성 | `$('<div>')` | `document.createElement('div')` |
| 애니메이션 | `animate()` | CSS `transition` + 클래스 토글 |
| 페이드 효과 | `fadeIn()` | CSS `opacity` + `transition` |

---

### jQuery를 계속 사용하는 것이 유리한 경우

```javascript
// 1. 레거시 코드가 이미 jQuery에 의존하는 프로젝트
// 2. Bootstrap 4 이하 (jQuery 필수)
// 3. IE 구버전 지원이 필요한 경우
// 4. 짧은 기간 안에 빠른 프로토타이핑이 필요한 경우
// 5. 팀의 jQuery 숙련도가 높은 경우

// jQuery: 한 줄로 간결하게
$('.items').addClass('active').filter(':even').css('color', 'blue');
```

---

### 바닐라 JS를 선택하는 것이 유리한 경우

```javascript
// 1. React, Vue, Angular 등 모던 프레임워크와 함께 사용
// 2. 번들 크기에 민감한 프로젝트 (jQuery: ~87KB minified)
// 3. Node.js / SSR 환경
// 4. 최신 브라우저만 지원해도 되는 신규 프로젝트
// 5. 성능이 극도로 중요한 경우

// 바닐라 JS: 표준 API로 동일한 작업
document.querySelectorAll('.items').forEach(el => {
  el.classList.add('active');
});
```

---

### 판단 흐름

```
신규 프로젝트인가?
  └─ Yes → 모던 프레임워크 사용 여부 확인
              └─ React/Vue/Angular 사용? → 바닐라 JS (또는 프레임워크 내장)
              └─ 사용 안 함, 단순 페이지? → jQuery 또는 바닐라 JS 모두 가능

기존 프로젝트인가?
  └─ jQuery 이미 사용 중? → jQuery 유지 (일관성)
  └─ jQuery 없음?         → 바닐라 JS 우선 검토
```

---

## 10.7 jQuery와 ES6+ 혼용 패턴

현대 프로젝트에서는 jQuery와 ES6+ 문법을 함께 사용하는 경우가 많다. 두 방식을 자연스럽게 혼용하는 패턴을 익힌다.

### 화살표 함수(Arrow Function)

jQuery 콜백에 화살표 함수를 사용할 때 `this` 바인딩에 주의해야 한다. 화살표 함수는 자신의 `this`를 갖지 않으므로, `this`가 필요한 콜백에는 일반 함수를 사용한다.

```javascript
// 일반 함수: this = 이벤트 발생 요소 (jQuery가 바인딩)
$('.btn').on('click', function() {
  $(this).addClass('active');   // ✓ 정상 동작
});

// 화살표 함수: this = 외부 스코프 (jQuery 바인딩 무시)
$('.btn').on('click', () => {
  $(this).addClass('active');   // ✗ this가 window (또는 undefined)
});

// 화살표 함수를 쓰되 this 대신 e.currentTarget 사용
$('.btn').on('click', (e) => {
  $(e.currentTarget).addClass('active');   // ✓ 정상 동작
});
```

---

### `each()` vs `forEach()`

jQuery의 `each()`와 배열의 `forEach()`를 상황에 맞게 선택한다.

```javascript
const $items = $('li');

// jQuery each: jQuery 객체 순회, this = 개별 DOM 요소
$items.each(function(index, element) {
  $(element).text((index + 1) + '. ' + $(element).text());
});

// 배열 메서드: JavaScript 배열로 변환 후 처리
Array.from($items).forEach((el, index) => {
  $(el).text((index + 1) + '. ' + $(el).text());
});

// 또는 spread 연산자
[...$items].forEach((el, index) => {
  $(el).text((index + 1) + '. ' + $(el).text());
});
```

---

### 템플릿 리터럴(Template Literal)

HTML 문자열 생성 시 템플릿 리터럴을 사용하면 가독성이 높아진다.

```javascript
// 기존 방식: 문자열 연결
function renderCard(user) {
  return '<div class="card">' +
    '<h3>' + user.name + '</h3>' +
    '<p>' + user.email + '</p>' +
    '</div>';
}

// ES6 템플릿 리터럴
function renderCard(user) {
  return `
    <div class="card">
      <h3>${user.name}</h3>
      <p>${user.email}</p>
      <p>🌐 ${user.website}</p>
    </div>
  `;
}

// Ajax + 템플릿 리터럴 조합
$.getJSON('/api/users').done(function(users) {
  const html = users.map(user => renderCard(user)).join('');
  $('#user-list').html(html);
});
```

---

### 구조 분해 할당(Destructuring)

Ajax 응답 데이터에서 필요한 필드만 추출할 때 편리하다.

```javascript
$.getJSON('/api/user/1').done(function(user) {
  // 기존 방식
  const name    = user.name;
  const email   = user.email;
  const city    = user.address.city;

  // 구조 분해 할당
  const { name, email, address: { city } } = user;

  $('#profile').html(`
    <h2>${name}</h2>
    <p>${email}</p>
    <p>${city}</p>
  `);
});
```

---

### `async` / `await`와 jQuery Ajax

jQuery의 jqXHR 객체는 Promise를 완전히 구현하지 않으므로 `async/await`와 함께 사용할 때 `Promise.resolve()`로 감싸거나 네이티브 `fetch()`와 혼용한다.

```javascript
// 방법 1: Promise.resolve()로 변환
async function loadUser(id) {
  try {
    const user = await Promise.resolve(
      $.getJSON(`/api/users/${id}`)
    );
    renderProfile(user);
  } catch (err) {
    showError(err);
  }
}

// 방법 2: fetch()와 혼용 (더 권장)
async function loadUser(id) {
  try {
    const res  = await fetch(`/api/users/${id}`);
    const user = await res.json();
    renderProfile(user);   // 렌더링은 jQuery DOM 조작 사용
  } catch (err) {
    showError(err);
  }
}

// 방법 3: jQuery Ajax는 유지하되 데이터 처리만 async 패턴으로
function loadUsers() {
  return $.getJSON('/api/users');  // jqXHR 반환
}

$.when(loadUsers())
  .done(users => renderList(users))
  .fail(() => showError());
```

---

### `const` / `let` 사용

`var` 대신 `const`와 `let`을 사용하면 스코프 오류를 예방할 수 있다.

```javascript
// 나쁜 예: var는 함수 스코프 — 루프 밖에서도 접근 가능
for (var i = 0; i < 5; i++) {
  setTimeout(function() {
    console.log(i);   // 항상 5 출력
  }, 100);
}

// 좋은 예: let은 블록 스코프
for (let i = 0; i < 5; i++) {
  setTimeout(function() {
    console.log(i);   // 0, 1, 2, 3, 4 각각 출력
  }, 100);
}

// jQuery 객체 참조는 const
const $btn = $('#submit');   // 재할당할 일 없음 → const
let currentPage = 1;         // 값이 변경됨 → let
```

---

### 모듈 패턴으로 코드 구조화

규모가 커지면 jQuery 코드를 IIFE(즉시 실행 함수) 또는 ES6 모듈로 구조화한다.

```javascript
// IIFE 모듈 패턴 — 전역 오염 방지
const UserModule = (function($) {
  // private 변수
  let currentUser = null;
  const API_URL   = '/api/users';

  // private 함수
  function renderCard(user) {
    return `<div class="user-card"><h3>${user.name}</h3></div>`;
  }

  // public API
  return {
    load: function(id) {
      return $.getJSON(`${API_URL}/${id}`)
        .done(function(user) {
          currentUser = user;
          $('#profile').html(renderCard(user));
        });
    },
    getCurrent: function() {
      return currentUser;
    }
  };
}($));

// 사용
UserModule.load(1).done(function() {
  console.log(UserModule.getCurrent().name);
});
```

---

## 10.8 자주 하는 실수와 해결책

### 실수 1. `$(document).ready()` 중복

```javascript
// 나쁜 예: $(document).ready() 두 번
$(document).ready(function() {
  bindEvents();
});
$(document).ready(function() {
  loadData();
});

// 좋은 예: 하나로 통합
$(function() {
  bindEvents();
  loadData();
});
```

---

### 실수 2. 동적 요소에 직접 이벤트 등록

```javascript
// 나쁜 예: 추가될 li에는 이벤트가 없음
$('li').on('click', handler);
$('#list').append('<li>새 항목</li>');   // 이 항목은 클릭해도 반응 없음

// 좋은 예: 이벤트 위임
$('#list').on('click', 'li', handler);
```

---

### 실수 3. `attr()`과 `prop()` 혼용

```javascript
// 나쁜 예: checked 상태를 attr()로 읽음 (항상 "checked" 반환)
if ($('#chk').attr('checked')) { ... }

// 좋은 예: prop()으로 현재 상태 읽기
if ($('#chk').prop('checked')) { ... }
```

---

### 실수 4. 루프 안에서 Ajax 호출

```javascript
// 나쁜 예: 루프마다 개별 요청 → 수십 개의 동시 요청 발생
$('.item').each(function() {
  const id = $(this).data('id');
  $.get('/api/item/' + id, function(data) { /* ... */ });
});

// 좋은 예: ID 목록을 모아 한 번에 요청
const ids = $('.item').map(function() {
  return $(this).data('id');
}).get();

$.post('/api/items/batch', { ids: ids }, function(data) {
  // 한 번의 요청으로 모든 데이터 처리
});
```

---

### 실수 5. 애니메이션 중복 실행

```javascript
// 나쁜 예: 빠른 클릭 시 애니메이션 누적
$('#btn').on('click', function() {
  $('#panel').slideToggle(300);
});

// 좋은 예: stop()으로 누적 방지
$('#btn').on('click', function() {
  $('#panel').stop(true, true).slideToggle(300);
});
```

---

## 10.9 성능 측정

최적화 전후를 비교하려면 브라우저 개발자 도구의 **Performance** 탭과 `console.time()`을 활용한다.

```javascript
// console.time()으로 코드 실행 시간 측정
console.time('DOM 삽입');

const $list = $('#big-list');
for (let i = 0; i < 1000; i++) {
  $list.append('<li>항목 ' + i + '</li>');
}

console.timeEnd('DOM 삽입');   // "DOM 삽입: 12.3ms" 형태로 출력
```

```javascript
// 최적화 버전과 비교
console.time('최적화 DOM 삽입');

const items = Array.from({ length: 1000 }, (_, i) => '<li>항목 ' + i + '</li>');
$('#big-list').html(items.join(''));

console.timeEnd('최적화 DOM 삽입');   // 훨씬 빠름
```

---

## 10.10 정리

### 핵심 최적화 체크리스트

| 항목 | 나쁜 패턴 | 좋은 패턴 |
|------|-----------|-----------|
| 선택자 반복 | `$('#id')` 매번 호출 | `const $el = $('#id')` 캐싱 |
| 루프 내 삽입 | `append()` 반복 | 문자열 축적 후 `html()` 한 번 |
| 이벤트 핸들러 | 요소마다 등록 | 이벤트 위임 |
| 빈번한 이벤트 | 핸들러 내 직접 DOM 조작 | 디바운스 / 스로틀 적용 |
| 읽기·쓰기 | 교차 수행 | 읽기 완료 후 쓰기 일괄 처리 |
| 전역 선택 | `$('p')` | `$('#article').find('p')` |
| 확장 선택자 | `$('li:even')` | `$('li').filter(':even')` |
| this 사용 | 화살표 함수 내 `$(this)` | `$(e.currentTarget)` |
| var 사용 | `var $btn = ...` | `const $btn = ...` |

### jQuery vs 바닐라 JS 선택 기준

| jQuery 선택 | 바닐라 JS 선택 |
|-------------|----------------|
| 레거시 프로젝트 유지보수 | 모던 프레임워크와 함께 사용 |
| IE 구버전 지원 필요 | 번들 크기 최적화 필요 |
| Bootstrap 4 이하 사용 | 최신 브라우저만 지원 |
| 빠른 프로토타이핑 | SSR / Node.js 환경 |

---

## 연습 문제

1. 아래 코드에서 성능상 문제가 있는 부분을 모두 찾고 개선된 코드를 작성하라.
   ```javascript
   for (let i = 0; i < 500; i++) {
     $('#result').append('<li>' + $('#source li').eq(i).text() + '</li>');
   }
   ```

2. `$(window).on('scroll', handler)` 이벤트에서 스크롤할 때마다 `$('#header').css('background', ...)` 가 실행되고 있다. 이를 `throttle`을 적용하여 100ms마다 최대 1회만 실행되도록 개선하라.

3. 아래 두 코드의 동작 차이를 설명하고, 어느 쪽이 더 적합한지 이유와 함께 작성하라.
   ```javascript
   // A
   $('.btn').on('click', function() { $(this).toggleClass('active'); });
   // B
   $('.btn').on('click', (e) => { $(e.currentTarget).toggleClass('active'); });
   ```

4. `serializeArray()`로 수집한 폼 데이터를 구조 분해 할당과 템플릿 리터럴을 활용하여 `#preview` 영역에 실시간으로 미리보기하는 코드를 ES6+ 문법으로 작성하라.

5. 아래 코드를 IIFE 모듈 패턴으로 리팩터링하라. `init()`, `bindEvents()`, `render()` 함수를 분리하고 외부에서 `init()`만 호출할 수 있도록 구성하라.
   ```javascript
   $(function() {
     $('#btn').on('click', function() {
       $.getJSON('/api/data').done(function(data) {
         data.forEach(function(item) {
           $('#list').append('<li>' + item.name + '</li>');
         });
       });
     });
   });
   ```

---

> **다음 장 예고**  
> 11장에서는 이 과정의 최종 실습 프로젝트로 Ajax 기반 **영화/도서 검색 앱**을 구현한다. 검색, 결과 렌더링, 상세 보기, 즐겨찾기 저장까지 Day 2에서 학습한 모든 내용을 통합하여 완성한다.
