# 5장. CSS 및 스타일 조작

> 학습 목표
> - `css()` 메서드로 요소의 스타일을 읽고 동적으로 변경할 수 있다.
> - `width()`, `height()` 및 관련 메서드로 요소의 크기를 정확하게 다룰 수 있다.
> - `offset()`, `position()`으로 요소의 좌표를 읽고 활용할 수 있다.
> - `scrollTop()`, `scrollLeft()`로 스크롤 위치를 읽고 제어할 수 있다.

---

## 5.1 CSS 조작 개요

jQuery에서 스타일을 제어하는 방법은 크게 두 가지다.

1. **클래스 기반 제어** (3장에서 학습한 `addClass`, `removeClass`, `toggleClass`)
   - CSS와 JavaScript 역할을 분리할 수 있어 유지보수에 유리
   - 스타일 변경 패턴이 미리 정해진 경우에 적합

2. **인라인 스타일 직접 제어** (`css()` 메서드)
   - 스크롤 위치나 마우스 좌표처럼 동적으로 계산된 값을 적용할 때 사용
   - CSS 파일에 미리 정의하기 어려운 경우에 적합

실무에서는 두 방식을 상황에 맞게 혼용한다.

---

## 5.2 `css()` 메서드

### 스타일 읽기

인수로 CSS 속성 이름을 문자열로 전달하면 현재 적용된 값을 반환한다. 인라인 스타일뿐 아니라 외부 CSS까지 포함된 **계산된(computed) 값**을 반환한다.

```javascript
// 단일 속성 읽기
const color = $('#box').css('color');
const fontSize = $('#box').css('font-size');   // "16px" 형태로 반환

console.log($('#box').css('display'));          // "block", "flex" 등
console.log($('#box').css('background-color')); // "rgb(255, 0, 0)" 형태
```

> CSS 속성 이름은 `camelCase`(`backgroundColor`)와 `kebab-case`(`background-color`) 모두 사용 가능하다.

```javascript
$('#box').css('backgroundColor');   // camelCase
$('#box').css('background-color');  // kebab-case — 두 방식 모두 동일하게 동작
```

---

### 스타일 쓰기

두 번째 인수로 값을 전달하면 인라인 스타일로 설정된다.

```javascript
// 단일 속성 설정
$('#box').css('color', 'red');
$('#box').css('font-size', '18px');
$('#box').css('margin-top', '20px');

// 단위 없이 숫자만 전달하면 px로 자동 처리 (일부 속성)
$('#box').css('font-size', 18);      // "18px"로 설정
$('#box').css('opacity', 0.5);       // 단위 없이 숫자 사용
```

---

### 여러 속성 동시 설정

객체(Object) 형태로 전달하면 여러 스타일을 한 번에 적용할 수 있다.

```javascript
$('#box').css({
  color: 'white',
  backgroundColor: '#333',
  fontSize: '16px',
  padding: '12px 20px',
  borderRadius: '4px'
});
```

---

### 콜백 함수로 현재 값 기반 변경

현재 값을 받아서 새 값을 반환하는 콜백 함수를 전달할 수 있다.

```javascript
// 현재 font-size에서 2px씩 증가
$('p').css('font-size', function(index, currentValue) {
  return parseFloat(currentValue) + 2 + 'px';
});

// 클릭할 때마다 투명도 10% 감소
$('#box').on('click', function() {
  $(this).css('opacity', function(i, val) {
    return Math.max(0, parseFloat(val) - 0.1);
  });
});
```

---

### 여러 속성 동시 읽기

배열로 여러 속성 이름을 전달하면 객체 형태로 반환한다.

```javascript
const styles = $('#box').css(['width', 'height', 'color', 'background-color']);
console.log(styles);
// { width: "200px", height: "100px", color: "rgb(0,0,0)", background-color: "rgb(255,255,255)" }
```

---

### `css()`를 사용할 때 주의사항

`css()`로 읽은 값은 항상 **문자열**이다. 숫자 연산이 필요하면 `parseFloat()` 또는 `parseInt()`로 변환해야 한다.

```javascript
const fontSize = $('#box').css('font-size');  // "16px" (문자열)
const size = parseFloat(fontSize);            // 16 (숫자)
$('#box').css('font-size', size + 4 + 'px'); // "20px"
```

---

## 5.3 크기 관련 메서드

jQuery는 요소의 크기를 다루는 전용 메서드를 제공한다. `css('width')`로도 읽을 수 있지만, 크기 관련 메서드는 **숫자**를 반환하여 계산에 바로 사용할 수 있다.

### 크기 메서드 종류와 범위

박스 모델(Box Model)에서 어느 범위까지 포함하느냐에 따라 메서드가 구분된다.

```
┌──────────────────────────── outerWidth(true) ────────────────────────────┐
│  margin                                                                   │
│  ┌─────────────────────── outerWidth() ──────────────────────────────┐   │
│  │  border                                                            │   │
│  │  ┌────────────────── innerWidth() ───────────────────────────┐    │   │
│  │  │  padding                                                   │    │   │
│  │  │  ┌──────────── width() ────────────────────────────┐      │    │   │
│  │  │  │                                                  │      │    │   │
│  │  │  │         content area                             │      │    │   │
│  │  │  └──────────────────────────────────────────────────┘      │    │   │
│  │  └────────────────────────────────────────────────────────────┘    │   │
│  └────────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────────┘
```

| 메서드 | 포함 범위 |
|--------|-----------|
| `width()` / `height()` | content 영역만 |
| `innerWidth()` / `innerHeight()` | content + padding |
| `outerWidth()` / `outerHeight()` | content + padding + border |
| `outerWidth(true)` / `outerHeight(true)` | content + padding + border + margin |

---

### `width()` / `height()`

콘텐츠 영역의 크기를 숫자(px)로 읽거나 설정한다.

```javascript
// 읽기 — 숫자 반환 (단위 없음)
const w = $('#box').width();    // 예: 300
const h = $('#box').height();   // 예: 200

// 쓰기
$('#box').width(400);
$('#box').height(300);
$('#box').width('50%');          // 문자열로 단위 포함 가능
```

---

### `innerWidth()` / `innerHeight()`

padding까지 포함한 크기를 반환한다. 읽기 전용이다.

```javascript
const innerW = $('#box').innerWidth();   // content + padding 너비
const innerH = $('#box').innerHeight();  // content + padding 높이
```

---

### `outerWidth()` / `outerHeight()`

border까지(또는 margin까지) 포함한 크기를 반환한다.

```javascript
$('#box').outerWidth();        // content + padding + border
$('#box').outerWidth(true);    // content + padding + border + margin

$('#box').outerHeight();
$('#box').outerHeight(true);
```

---

### 뷰포트(창) 크기

`$(window)`에 사용하면 브라우저 창의 크기를 구할 수 있다.

```javascript
const viewportWidth  = $(window).width();
const viewportHeight = $(window).height();

// 반응형: 창 크기 변경 시 재계산
$(window).on('resize', function() {
  if ($(this).width() < 768) {
    $('#sidebar').hide();
  } else {
    $('#sidebar').show();
  }
});
```

---

### 문서(전체 페이지) 크기

`$(document)`에 사용하면 스크롤 가능한 전체 문서의 크기를 구할 수 있다.

```javascript
const docWidth  = $(document).width();
const docHeight = $(document).height();
```

---

## 5.4 위치 관련 메서드

### `offset()`

요소의 **문서(document) 기준** 좌표를 반환한다. 스크롤 위치를 포함한 절대 위치다.

```javascript
// 읽기 — { top, left } 객체 반환
const pos = $('#box').offset();
console.log(pos.top);   // 문서 최상단으로부터의 거리 (px)
console.log(pos.left);  // 문서 최좌측으로부터의 거리 (px)

// 쓰기 — 요소를 문서 기준 절대 위치로 이동
$('#box').offset({ top: 200, left: 100 });
```

---

### `position()`

요소의 **가장 가까운 position 지정 조상 요소 기준** 좌표를 반환한다. CSS의 `position: relative` 부모를 기준으로 한 상대 위치다.

```javascript
// 읽기 전용 — { top, left } 객체 반환
const pos = $('#box').position();
console.log(pos.top);
console.log(pos.left);
```

---

### `offset()` vs `position()` 비교

```
┌── document ─────────────────────────────────────────────┐
│                                                          │
│   offset().top = 150  ←──────────────────────────────   │
│                                                          │
│   ┌── position:relative 부모 ────────────────────────┐  │
│   │                                                   │  │
│   │   position().top = 50  ←──────────────────────   │  │
│   │                                                   │  │
│   │   ┌── #box ──────────┐                           │  │
│   │   │                  │                           │  │
│   │   └──────────────────┘                           │  │
│   └───────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

| 메서드 | 기준점 | 스크롤 포함 | 쓰기 |
|--------|--------|------------|------|
| `offset()` | 문서(document) 전체 | O | O |
| `position()` | 가장 가까운 position 조상 | X | X |

---

### 실무 활용: 툴팁 위치 계산

```javascript
$('.info-icon').on('mouseenter', function() {
  const pos = $(this).offset();
  const h   = $(this).outerHeight();

  $('#tooltip')
    .css({
      top:  pos.top + h + 8,   // 아이콘 아래 8px
      left: pos.left
    })
    .show();
}).on('mouseleave', function() {
  $('#tooltip').hide();
});
```

---

## 5.5 스크롤 관련 메서드

### `scrollTop()` / `scrollLeft()`

요소 또는 창의 **스크롤 위치**를 읽거나 설정한다.

```javascript
// 현재 세로 스크롤 위치 읽기
const scrollY = $(window).scrollTop();   // 픽셀 단위 숫자 반환

// 세로 스크롤 위치 설정 (애니메이션 없이 즉시 이동)
$(window).scrollTop(0);                  // 맨 위로 이동
$(window).scrollTop(500);               // 500px 위치로 이동

// 가로 스크롤
const scrollX = $(window).scrollLeft();
$(window).scrollLeft(0);
```

---

### 스크롤 이벤트와 조합

```javascript
// 스크롤 방향 감지
let lastScrollTop = 0;

$(window).on('scroll', function() {
  const current = $(this).scrollTop();

  if (current > lastScrollTop) {
    $('#header').addClass('hidden');    // 아래로 스크롤 — 헤더 숨김
  } else {
    $('#header').removeClass('hidden'); // 위로 스크롤 — 헤더 표시
  }

  lastScrollTop = current;
});
```

---

### 부드러운 스크롤 이동 (animate 활용)

5장의 `scrollTop()`과 7장의 `animate()`를 조합하면 부드러운 스크롤 이동을 구현할 수 있다. 여기서는 패턴만 미리 소개한다.

```javascript
// "맨 위로" 버튼 — 부드럽게 스크롤
$('#btn-top').on('click', function() {
  $('html, body').animate({ scrollTop: 0 }, 500);
});

// 특정 섹션으로 부드럽게 이동
$('a[href^="#"]').on('click', function(e) {
  e.preventDefault();
  const target = $(this).attr('href');
  const offset = $(target).offset().top;
  $('html, body').animate({ scrollTop: offset - 60 }, 600);  // 헤더 높이 60px 보정
});
```

---

### 특정 요소의 스크롤 가능 영역

`$(window)` 외에 `overflow: auto` 또는 `overflow: scroll`이 설정된 요소에도 사용할 수 있다.

```javascript
// 스크롤 가능한 div 내에서 스크롤 위치 읽기
const scrollPos = $('#scroll-container').scrollTop();

// 맨 아래로 스크롤 (채팅창 등)
function scrollToBottom() {
  const $chat = $('#chat-box');
  $chat.scrollTop($chat[0].scrollHeight);
}
```

---

## 5.6 실습: 스크롤 기반 UI

다음은 이 장의 주요 메서드를 활용한 스크롤 반응형 UI 예제다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>CSS 및 스타일 조작 실습</title>
  <style>
    body { margin: 0; font-family: sans-serif; }

    #header {
      position: fixed; top: 0; left: 0; right: 0;
      height: 60px; background: #fff;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      display: flex; align-items: center; padding: 0 20px;
      transition: transform 0.3s;
      z-index: 100;
    }
    #header.hidden { transform: translateY(-100%); }

    #progress-bar {
      position: fixed; top: 0; left: 0;
      height: 4px; background: #007bff; width: 0;
      z-index: 200;
    }

    main { padding-top: 80px; }
    section { height: 400px; display: flex; align-items: center;
              justify-content: center; font-size: 1.5rem; }
    section:nth-child(odd) { background: #f0f4ff; }

    #btn-top {
      display: none; position: fixed; bottom: 30px; right: 30px;
      padding: 10px 16px; background: #007bff; color: #fff;
      border: none; border-radius: 50%; cursor: pointer;
      font-size: 1.2rem;
    }

    #tooltip {
      display: none; position: absolute;
      background: #333; color: #fff;
      padding: 6px 12px; border-radius: 4px;
      font-size: 0.85rem; z-index: 300;
      white-space: nowrap;
    }
  </style>
</head>
<body>

  <div id="progress-bar"></div>

  <header id="header">
    <span>jQuery 스크롤 UI</span>
    <span class="info-icon" style="margin-left:auto; cursor:pointer;">ℹ️</span>
  </header>

  <div id="tooltip">jQuery 스타일 조작 실습 페이지</div>

  <main>
    <section>섹션 1</section>
    <section>섹션 2</section>
    <section>섹션 3</section>
    <section>섹션 4</section>
    <section>섹션 5</section>
  </main>

  <button id="btn-top">↑</button>

  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script>
    $(function() {
      let lastScrollTop = 0;

      $(window).on('scroll', function() {
        const scrollTop    = $(this).scrollTop();
        const docHeight    = $(document).height();
        const windowHeight = $(this).height();

        // ① 읽기 진행 바 (scrollTop 기반 퍼센트 계산)
        const progress = scrollTop / (docHeight - windowHeight) * 100;
        $('#progress-bar').width(progress + '%');

        // ② 맨 위로 버튼 표시/숨김
        if (scrollTop > 300) {
          $('#btn-top').fadeIn(200);
        } else {
          $('#btn-top').fadeOut(200);
        }

        // ③ 스크롤 방향에 따라 헤더 숨김/표시
        if (scrollTop > lastScrollTop && scrollTop > 60) {
          $('#header').addClass('hidden');
        } else {
          $('#header').removeClass('hidden');
        }
        lastScrollTop = scrollTop;
      });

      // ④ 맨 위로 부드럽게 스크롤
      $('#btn-top').on('click', function() {
        $('html, body').animate({ scrollTop: 0 }, 500);
      });

      // ⑤ offset()으로 툴팁 위치 계산
      $('.info-icon').on('mouseenter', function() {
        const pos = $(this).offset();
        const h   = $(this).outerHeight();

        $('#tooltip').css({
          top:  pos.top + h + 8,
          left: pos.left
        }).fadeIn(150);
      }).on('mouseleave', function() {
        $('#tooltip').fadeOut(150);
      });
    });
  </script>
</body>
</html>
```

**코드 포인트 분석:**

| 번호 | 메서드 | 활용 내용 |
|------|--------|-----------|
| ① | `scrollTop()`, `$(document).height()`, `$(window).height()` | 스크롤 진행률 계산 후 `width()`로 프로그레스 바 적용 |
| ② | `scrollTop()`, `fadeIn()` / `fadeOut()` | 스크롤 300px 이상 시 버튼 표시 |
| ③ | `scrollTop()`, `addClass()` / `removeClass()` | 스크롤 방향 감지 후 헤더 숨김/표시 |
| ④ | `animate({ scrollTop })` | 부드러운 최상단 이동 |
| ⑤ | `offset()`, `outerHeight()`, `css()` | 툴팁 위치를 동적으로 계산하여 배치 |

---

## 5.7 정리

### `css()` 메서드

| 사용 방법 | 설명 |
|-----------|------|
| `.css('속성')` | 단일 속성 읽기 (문자열 반환) |
| `.css(['속성1', '속성2'])` | 여러 속성 읽기 (객체 반환) |
| `.css('속성', '값')` | 단일 속성 설정 |
| `.css({ 속성: 값, ... })` | 여러 속성 동시 설정 |
| `.css('속성', fn)` | 콜백으로 현재 값 기반 변경 |

### 크기 메서드

| 메서드 | 포함 범위 |
|--------|-----------|
| `width()` / `height()` | content |
| `innerWidth()` / `innerHeight()` | content + padding |
| `outerWidth()` / `outerHeight()` | content + padding + border |
| `outerWidth(true)` / `outerHeight(true)` | content + padding + border + margin |

### 위치·스크롤 메서드

| 메서드 | 설명 |
|--------|------|
| `offset()` | 문서 기준 절대 좌표 읽기/쓰기 |
| `position()` | position 조상 기준 상대 좌표 읽기 |
| `scrollTop()` / `scrollLeft()` | 스크롤 위치 읽기/쓰기 |

---

## 연습 문제

1. `#box` 요소의 현재 `font-size`를 읽어 2배로 키우는 코드를 `css()` 콜백 함수를 사용하여 작성하라.

2. 브라우저 창의 너비가 600px 미만이면 `#sidebar`를 숨기고, 이상이면 보이도록 `$(window).width()`와 `resize` 이벤트를 활용하여 구현하라.

3. `#floating-btn` 요소를 항상 `#container`의 우측 하단 모서리(padding 10px)에 위치시키는 코드를 `offset()`과 `outerWidth()`, `outerHeight()`를 사용하여 작성하라.

4. `$(window).scrollTop()`을 활용하여 스크롤이 페이지 전체의 절반을 넘으면 `#reading-status`의 텍스트를 `"절반 이상 읽었습니다"`로 변경하는 코드를 작성하라.

5. `#chat-log` 요소(overflow: auto)에 새 메시지 `<p>` 요소가 추가될 때마다 자동으로 맨 아래로 스크롤되도록 구현하라.

---

> **다음 장 예고**  
> 6장에서는 Day 1의 학습 내용을 종합하는 **실습 프로젝트 ①**을 진행한다. DOM 조작, 이벤트 처리, 스타일 제어를 통합하여 동적 To-Do 리스트 애플리케이션을 완성한다.
