# 1장. jQuery 소개와 환경 설정

> 학습 목표
> - jQuery가 무엇인지 이해하고 바닐라 JS와의 차이를 설명할 수 있다.
> - CDN과 로컬 설치 방식으로 jQuery를 프로젝트에 적용할 수 있다.
> - `$(document).ready()`와 `DOMContentLoaded`의 차이를 이해한다.
> - jQuery 객체의 개념과 래핑(wrapping)의 의미를 설명할 수 있다.

---

## 1.1 jQuery란?

jQuery는 2006년 John Resig이 발표한 **JavaScript 라이브러리**다. "Write Less, Do More"라는 슬로건처럼 복잡한 DOM 조작, 이벤트 처리, Ajax 통신을 짧고 일관된 문법으로 처리할 수 있도록 설계되었다.

jQuery가 등장한 배경에는 당시 브라우저 호환성 문제가 있었다. Internet Explorer, Firefox, Chrome이 각기 다른 JavaScript API를 지원하던 시절, jQuery는 브라우저 간 차이를 내부에서 흡수하여 개발자가 동일한 코드로 모든 브라우저를 지원할 수 있게 해주었다.

오늘날에는 브라우저 표준화가 많이 진행되었지만, jQuery는 여전히 수많은 레거시 프로젝트와 WordPress, Bootstrap 등 주요 프레임워크에서 사용된다. 실무에서 기존 코드를 유지보수하거나 빠른 프로토타이핑이 필요할 때 jQuery는 강력한 도구가 된다.

---

## 1.2 바닐라 JS와 jQuery 비교

같은 작업을 바닐라 JS와 jQuery로 각각 작성한 예시를 비교해 보자.

### 예시 1: 요소 선택 및 텍스트 변경

**바닐라 JS**
```javascript
document.querySelector('#title').textContent = '안녕하세요';
```

**jQuery**
```javascript
$('#title').text('안녕하세요');
```

---

### 예시 2: 여러 요소에 클래스 추가

**바닐라 JS**
```javascript
document.querySelectorAll('.item').forEach(function(el) {
  el.classList.add('active');
});
```

**jQuery**
```javascript
$('.item').addClass('active');
```

jQuery는 선택한 요소가 여러 개여도 반복문 없이 한 번에 처리한다.

---

### 예시 3: Ajax 요청

**바닐라 JS**
```javascript
fetch('https://api.example.com/data')
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error(error));
```

**jQuery**
```javascript
$.getJSON('https://api.example.com/data', function(data) {
  console.log(data);
});
```

---

### jQuery를 사용할 때와 사용하지 않을 때

| 상황 | 권장 |
|------|------|
| 레거시 프로젝트 유지보수 | jQuery 유지 |
| Bootstrap, WordPress 기반 프로젝트 | jQuery 활용 |
| React, Vue, Angular 기반 SPA | 바닐라 JS 또는 프레임워크 내장 기능 |
| 최신 브라우저만 지원하는 신규 프로젝트 | 바닐라 JS 고려 |

> **참고:** jQuery는 라이브러리다. React, Vue 같은 프레임워크와 달리 프로젝트 구조를 강제하지 않으며 필요한 부분에만 선택적으로 사용할 수 있다.

---

## 1.3 jQuery 설치와 환경 설정

### 방법 1: CDN 사용 (권장 — 실습 및 빠른 프로토타이핑)

CDN(Content Delivery Network)을 통해 jQuery를 외부에서 불러오는 방식이다. 별도 파일 다운로드 없이 `<script>` 태그 하나로 바로 사용할 수 있다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>jQuery 실습</title>
</head>
<body>

  <h1 id="title">Hello</h1>

  <!-- body 닫는 태그 바로 위에 삽입 -->
  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script>
    // 이 아래에 jQuery 코드 작성
    $('#title').text('jQuery 시작!');
  </script>
</body>
</html>
```

> **주의:** `<script>` 태그는 반드시 조작할 HTML 요소보다 **아래**에 위치해야 한다. 또는 `$(document).ready()`를 사용한다(1.4절 참고).

주요 CDN 제공처:

| CDN | URL |
|-----|-----|
| jQuery 공식 | `https://code.jquery.com/jquery-3.7.1.min.js` |
| Google | `https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js` |
| cdnjs | `https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js` |

---

### 방법 2: 로컬 파일 설치

인터넷이 제한된 환경이나 오프라인 프로젝트에서는 파일을 직접 다운로드하여 사용한다.

1. [https://jquery.com/download/](https://jquery.com/download/) 에서 `jquery-3.x.x.min.js` 다운로드
2. 프로젝트 폴더에 복사 (예: `js/jquery.min.js`)
3. HTML에서 경로를 지정하여 로드

```html
<script src="js/jquery.min.js"></script>
```

---

### 방법 3: npm 설치 (Node.js 환경)

Node.js 기반 빌드 환경을 사용하는 경우 npm으로 설치한다.

```bash
npm install jquery
```

```javascript
// ES 모듈 환경에서 import
import $ from 'jquery';
```

---

### min 버전 vs 일반 버전

| 파일명 | 설명 | 용도 |
|--------|------|------|
| `jquery-3.7.1.js` | 원본 파일 (약 290KB), 주석 포함 | 개발/디버깅 |
| `jquery-3.7.1.min.js` | 압축 파일 (약 87KB), 공백·주석 제거 | 프로덕션 배포 |

실습에서는 `.min.js`를 사용해도 무방하다.

---

## 1.4 `$(document).ready()` vs `DOMContentLoaded`

### 왜 필요한가?

브라우저는 HTML을 위에서 아래로 순서대로 파싱한다. `<head>` 안에 `<script>`를 넣으면 아직 `<body>`의 HTML 요소가 생성되지 않은 시점에 JavaScript가 실행되어 요소를 찾지 못하는 오류가 발생한다.

```html
<head>
  <script>
    // 오류! 아직 #box 요소가 존재하지 않음
    document.querySelector('#box').style.color = 'red';
  </script>
</head>
<body>
  <div id="box">텍스트</div>
</body>
```

이를 해결하는 방법이 **DOM 로드 완료 이벤트**를 기다리는 것이다.

---

### 바닐라 JS: `DOMContentLoaded`

```javascript
document.addEventListener('DOMContentLoaded', function() {
  document.querySelector('#box').style.color = 'red';
});
```

`DOMContentLoaded`는 HTML 파싱이 완료되어 DOM 트리가 구성된 시점에 발생한다. 이미지, 스타일시트 등 외부 리소스 로드는 기다리지 않는다.

---

### jQuery: `$(document).ready()`

```javascript
$(document).ready(function() {
  $('#box').css('color', 'red');
});
```

jQuery의 `$(document).ready()`는 내부적으로 `DOMContentLoaded`를 사용하며 브라우저 호환성을 보장한다.

**단축 문법 (가장 많이 사용):**

```javascript
$(function() {
  $('#box').css('color', 'red');
});
```

`$(function() { ... })`는 `$(document).ready(function() { ... })`와 완전히 동일하다.

---

### `DOMContentLoaded` vs `window.onload` 비교

| 이벤트 | 발생 시점 | 특징 |
|--------|-----------|------|
| `DOMContentLoaded` | HTML 파싱 완료 후 | 이미지·CSS 로드 전에도 실행됨, 빠름 |
| `window.onload` | 모든 리소스(이미지 등) 로드 완료 후 | 느리지만 이미지 크기 등 접근 가능 |

```javascript
// window.onload에 해당하는 jQuery 문법
$(window).on('load', function() {
  console.log('모든 리소스 로드 완료');
});
```

---

### 실습: 스크립트 위치와 ready() 비교

아래 두 방식을 직접 실행하며 차이를 확인하라.

```html
<!-- 방식 A: body 하단에 스크립트 배치 -->
<body>
  <p id="msg">Hello</p>
  <script src="jquery.min.js"></script>
  <script>
    $('#msg').text('방식 A: 정상 동작');
  </script>
</body>
```

```html
<!-- 방식 B: head에 스크립트 + $(document).ready() 사용 -->
<head>
  <script src="jquery.min.js"></script>
  <script>
    $(function() {
      $('#msg').text('방식 B: 정상 동작');
    });
  </script>
</head>
<body>
  <p id="msg">Hello</p>
</body>
```

두 방식 모두 정상 동작한다. 실무에서는 **방식 A(body 하단 배치)**를 더 많이 사용한다.

---

## 1.5 jQuery 객체와 래핑(Wrapping) 개념

### jQuery 객체란?

`$()` 함수는 CSS 선택자를 받아 해당 DOM 요소들을 감싼 **jQuery 객체**를 반환한다. 이 과정을 **래핑(wrapping)**이라고 한다.

```javascript
// 바닐라 JS: HTMLElement 반환
const el = document.querySelector('#title');
console.log(el);           // <h1 id="title">...</h1>
console.log(typeof el);    // object (HTMLElement)

// jQuery: jQuery 객체 반환
const $el = $('#title');
console.log($el);          // jQuery {0: h1#title, length: 1, ...}
console.log(typeof $el);   // object (jQuery Object)
```

jQuery 객체는 배열처럼 내부에 DOM 요소를 담고 있으며, jQuery가 제공하는 메서드(`.text()`, `.css()`, `.on()` 등)를 사용할 수 있다.

---

### DOM 요소 ↔ jQuery 객체 변환

때로는 jQuery 객체를 일반 DOM 요소로 변환하거나, 반대로 DOM 요소를 jQuery로 감싸야 하는 경우가 있다.

```javascript
// jQuery 객체 → DOM 요소
const $el = $('#title');
const domEl = $el[0];         // 배열 인덱스로 접근
const domEl2 = $el.get(0);   // .get() 메서드 사용

// DOM 요소 → jQuery 객체
const domEl = document.querySelector('#title');
const $el = $(domEl);         // $()로 감싸기
```

---

### 여러 요소를 감싸는 jQuery 객체

jQuery 객체는 선택된 요소가 여러 개일 때 모두 담는다.

```javascript
const $items = $('li');
console.log($items.length);  // li 요소의 개수

// 모든 li에 한 번에 스타일 적용
$items.css('color', 'blue');

// 특정 인덱스의 요소만 접근
$items.eq(0).css('font-weight', 'bold');  // 첫 번째 li
$items.eq(-1).css('font-weight', 'bold'); // 마지막 li
```

---

### 변수 네이밍 컨벤션

jQuery 객체를 담는 변수는 `$`를 접두사로 붙이는 것이 관례다. 일반 DOM 요소와 쉽게 구분할 수 있다.

```javascript
const title = document.querySelector('#title');   // DOM 요소
const $title = $('#title');                        // jQuery 객체
```

---

### 메서드 체이닝(Method Chaining)

jQuery 메서드는 대부분 jQuery 객체 자신을 반환하므로, 점(`.`)으로 연결하여 여러 메서드를 이어서 호출할 수 있다.

```javascript
$('#box')
  .css('color', 'white')
  .css('background', 'blue')
  .text('jQuery 체이닝')
  .addClass('highlight');
```

체이닝은 jQuery의 핵심 특징 중 하나로, 코드를 간결하게 만들어 준다.

---

## 1.6 정리

| 개념 | 핵심 내용 |
|------|-----------|
| jQuery | DOM 조작, 이벤트, Ajax를 간결하게 처리하는 JS 라이브러리 |
| 설치 | CDN 또는 로컬 파일, npm 중 선택 |
| `$(document).ready()` | DOM 로드 완료 후 코드 실행 보장, `$(function(){})` 단축 문법 사용 |
| jQuery 객체 | `$()` 함수가 반환하는 래핑된 DOM 요소 묶음 |
| 메서드 체이닝 | 메서드를 연속 호출하여 코드를 간결하게 작성 |

---

## 연습 문제

1. `<h2 id="greeting">안녕하세요</h2>` 요소의 텍스트를 jQuery로 변경하는 코드를 작성하라.

2. 아래 바닐라 JS 코드를 jQuery로 변환하라.
   ```javascript
   document.addEventListener('DOMContentLoaded', function() {
     document.querySelectorAll('p').forEach(function(el) {
       el.style.fontSize = '18px';
     });
   });
   ```

3. jQuery 객체에서 두 번째 DOM 요소를 꺼내는 두 가지 방법을 작성하라.

---

> **다음 장 예고**  
> 2장에서는 jQuery의 강력한 선택자 문법을 학습한다. CSS 선택자를 넘어 jQuery만의 확장 선택자와 DOM 탐색 메서드를 익혀 원하는 요소를 정확하고 빠르게 선택하는 방법을 배운다.
