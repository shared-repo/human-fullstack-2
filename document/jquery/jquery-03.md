# 3장. DOM 조작

> 학습 목표
> - `html()`, `text()`, `val()`로 요소의 콘텐츠를 읽고 쓸 수 있다.
> - `attr()`, `prop()`, `removeAttr()`로 속성을 제어할 수 있다.
> - `addClass()`, `removeClass()`, `toggleClass()`로 클래스를 동적으로 변경할 수 있다.
> - `append()`, `prepend()`, `before()`, `after()`, `remove()`, `clone()`으로 요소를 생성·삽입·삭제할 수 있다.

---

## 3.1 DOM 조작이란?

DOM(Document Object Model)은 브라우저가 HTML을 파싱하여 만들어낸 트리 구조다. jQuery의 DOM 조작 메서드를 사용하면 페이지를 새로 고침하지 않고도 화면의 내용을 동적으로 변경할 수 있다.

DOM 조작은 크게 세 가지 영역으로 나뉜다.

1. **콘텐츠 조작** — 요소 안의 텍스트·HTML·입력값 읽기/쓰기
2. **속성·클래스 조작** — 태그의 속성(attribute)과 CSS 클래스 변경
3. **구조 조작** — 요소 추가·이동·복사·삭제

---

## 3.2 콘텐츠 조작

### `html()`

요소의 **innerHTML**을 읽거나 설정한다. HTML 태그가 해석되어 렌더링된다.

```javascript
// 읽기 (인수 없음)
const content = $('#box').html();
console.log(content);   // "<strong>텍스트</strong>"

// 쓰기 (인수 전달)
$('#box').html('<strong>굵은 텍스트</strong>');
```

여러 요소를 선택한 상태에서 읽기를 하면 **첫 번째 요소**의 값만 반환된다.

```javascript
// 선택된 모든 p의 내용을 같은 값으로 변경
$('p').html('<em>수정됨</em>');
```

---

### `text()`

요소의 **텍스트 콘텐츠**를 읽거나 설정한다. HTML 태그는 해석하지 않고 문자 그대로 처리한다.

```javascript
// 읽기
const msg = $('#msg').text();
console.log(msg);   // 태그 없이 순수 텍스트만 반환

// 쓰기
$('#msg').text('새 텍스트');

// HTML 태그를 써도 태그가 그대로 문자열로 표시됨
$('#msg').text('<strong>굵게</strong>');
// 화면에 출력: <strong>굵게</strong>  (태그가 보임)
```

> **`html()` vs `text()` 선택 기준**
> - 내부에 HTML 마크업을 넣어야 하면 → `html()`
> - 사용자 입력값을 그대로 표시해야 하면 → `text()` (XSS 방지)

---

### `val()`

`<input>`, `<select>`, `<textarea>` 등 **폼 요소의 value**를 읽거나 설정한다.

```javascript
// 텍스트 입력 읽기
const name = $('input[name="username"]').val();

// 값 설정
$('input[name="username"]').val('홍길동');

// 초기화
$('input').val('');

// select 선택값 읽기
const selected = $('select#city').val();

// select 값 설정 (option의 value와 일치해야 함)
$('select#city').val('seoul');

// 체크박스: 체크 여부는 prop()으로 (3.3절 참고)
```

---

### `html()`, `text()`, `val()` 비교 요약

| 메서드 | 대상 | HTML 해석 | 용도 |
|--------|------|-----------|------|
| `html()` | 일반 요소 | O | 마크업 포함 콘텐츠 읽기/쓰기 |
| `text()` | 일반 요소 | X | 순수 텍스트 읽기/쓰기, XSS 방지 |
| `val()` | 폼 요소 | X | input, select, textarea 값 읽기/쓰기 |

---

### 콜백 함수를 이용한 값 변경

`html()`, `text()`, `val()`은 인수로 함수를 받을 수 있다. 함수의 인수로 현재 인덱스와 현재 값이 전달된다.

```javascript
// 모든 li의 텍스트 앞에 번호 붙이기
$('li').text(function(index, currentText) {
  return (index + 1) + '. ' + currentText;
});
```

---

## 3.3 속성 조작

### `attr()` — HTML 속성(Attribute)

HTML 태그에 명시된 **속성값**을 읽거나 설정한다.

```javascript
// 읽기
const src = $('img#logo').attr('src');
const href = $('a#link').attr('href');

// 쓰기
$('img#logo').attr('src', 'images/new-logo.png');
$('a#link').attr('href', 'https://example.com');

// 여러 속성 동시 설정 (객체 전달)
$('a#link').attr({
  href: 'https://example.com',
  target: '_blank',
  title: '외부 링크'
});
```

---

### `removeAttr()` — 속성 제거

속성 자체를 HTML에서 제거한다.

```javascript
$('input').removeAttr('disabled');   // disabled 속성 제거 (활성화)
$('a').removeAttr('target');         // target 속성 제거
```

---

### `prop()` — DOM 프로퍼티(Property)

`attr()`이 HTML 마크업의 속성을 다룬다면, `prop()`은 DOM 객체의 **JavaScript 프로퍼티**를 다룬다. 체크박스의 체크 상태처럼 현재 상태(동적으로 변하는 값)를 읽고 쓸 때 사용한다.

```javascript
// 체크박스 체크 여부 읽기
const isChecked = $('input#agree').prop('checked');  // true / false

// 체크 상태 설정
$('input#agree').prop('checked', true);   // 체크
$('input#agree').prop('checked', false);  // 체크 해제

// disabled 상태 제어
$('button#submit').prop('disabled', true);   // 버튼 비활성화
$('button#submit').prop('disabled', false);  // 버튼 활성화

// select의 multiple 속성
$('select').prop('multiple', true);
```

---

### `attr()` vs `prop()` 차이

이 둘의 차이는 처음에 혼동하기 쉽다. 핵심 기준은 "현재 상태가 변하는 값인가"다.

```html
<input type="checkbox" id="chk" checked>
```

```javascript
// 페이지 로드 직후
$('#chk').attr('checked');   // "checked" (HTML 마크업 초기값)
$('#chk').prop('checked');   // true (현재 DOM 상태)

// 사용자가 체크 해제한 뒤
$('#chk').attr('checked');   // "checked" (HTML 마크업은 변하지 않음)
$('#chk').prop('checked');   // false (현재 DOM 상태는 변함)
```

| 구분 | `attr()` | `prop()` |
|------|----------|----------|
| 대상 | HTML 마크업 속성 | DOM 객체 프로퍼티 |
| 반영 | 초기값 고정 | 현재 상태 반영 |
| 주요 사용처 | href, src, name, id, class | checked, disabled, selected, multiple |

> **실무 기준:** `checked`, `disabled`, `selected`는 항상 `prop()`을 사용하고, 나머지 일반 속성(href, src, data-* 등)은 `attr()`을 사용한다.

---

## 3.4 클래스 조작

CSS 클래스를 동적으로 추가·제거하면 JavaScript에서 스타일을 직접 변경하는 것보다 유지보수가 쉽고 CSS와 JS 역할을 분리할 수 있다.

### `addClass(className)`

하나 또는 여러 클래스를 추가한다. 이미 있는 클래스는 중복 추가되지 않는다.

```javascript
$('#box').addClass('active');
$('#box').addClass('active highlight');   // 공백으로 구분하여 여러 개 추가
```

---

### `removeClass(className)`

클래스를 제거한다. 인수 없이 호출하면 모든 클래스를 제거한다.

```javascript
$('#box').removeClass('active');
$('#box').removeClass('active highlight');  // 여러 개 동시 제거
$('#box').removeClass();                    // 모든 클래스 제거
```

---

### `toggleClass(className)`

클래스가 있으면 제거하고, 없으면 추가한다. 토글(켜기/끄기) 기능에 유용하다.

```javascript
$('#menu-btn').on('click', function() {
  $('#nav').toggleClass('open');   // 클릭할 때마다 open 클래스 토글
});
```

두 번째 인수로 `true`/`false`를 전달하면 강제로 추가하거나 제거할 수 있다.

```javascript
const isLoggedIn = true;
$('#user-area').toggleClass('logged-in', isLoggedIn);  // true면 추가, false면 제거
```

---

### `hasClass(className)`

특정 클래스가 있는지 확인하여 `true`/`false`를 반환한다.

```javascript
if ($('#box').hasClass('active')) {
  console.log('활성화 상태');
}
```

---

### 클래스 조작 종합 예시

```javascript
// 탭 메뉴: 클릭한 탭만 active
$('.tab').on('click', function() {
  $('.tab').removeClass('active');     // 모든 탭에서 active 제거
  $(this).addClass('active');          // 클릭한 탭에만 active 추가
});
```

---

## 3.5 요소 생성과 삽입

jQuery로 새 요소를 만들 때는 `$()` 함수에 HTML 문자열을 전달한다.

```javascript
// 새 요소 생성
const $newItem = $('<li>새 항목</li>');
const $newBtn  = $('<button type="button">클릭</button>');

// 속성과 함께 생성
const $newLink = $('<a>', {
  href: 'https://example.com',
  text: '링크',
  class: 'external'
});
```

---

### 내부에 삽입: `append()` / `prepend()`

선택한 요소의 **자식**으로 삽입한다.

| 메서드 | 삽입 위치 |
|--------|-----------|
| `append(content)` | 마지막 자식으로 삽입 |
| `prepend(content)` | 첫 번째 자식으로 삽입 |

```html
<ul id="list">
  <li>기존 항목</li>
</ul>
```

```javascript
$('#list').append('<li>마지막에 추가</li>');
// 결과:
// <ul id="list">
//   <li>기존 항목</li>
//   <li>마지막에 추가</li>
// </ul>

$('#list').prepend('<li>처음에 추가</li>');
// 결과:
// <ul id="list">
//   <li>처음에 추가</li>
//   <li>기존 항목</li>
//   <li>마지막에 추가</li>
// </ul>
```

---

### 역방향 삽입: `appendTo()` / `prependTo()`

`append()`/`prepend()`의 주어와 목적어가 바뀐 형태다. 체이닝 흐름에 따라 선택한다.

```javascript
// 아래 두 코드는 동일한 결과
$('#list').append('<li>항목</li>');
$('<li>항목</li>').appendTo('#list');
```

---

### 외부에 삽입: `before()` / `after()`

선택한 요소의 **형제**로 삽입한다.

| 메서드 | 삽입 위치 |
|--------|-----------|
| `before(content)` | 선택 요소 바로 앞 |
| `after(content)` | 선택 요소 바로 뒤 |

```html
<p id="target">기준 문단</p>
```

```javascript
$('#target').before('<p>앞에 삽입</p>');
$('#target').after('<p>뒤에 삽입</p>');

// 결과:
// <p>앞에 삽입</p>
// <p id="target">기준 문단</p>
// <p>뒤에 삽입</p>
```

---

### 삽입 메서드 위치 정리

```
[부모 요소]
  prepend() → [첫 번째 자식]
               [기존 자식들]
  append()  → [마지막 자식]

before() → [선택 요소] → after()
```

---

### 기존 요소를 이동시키는 삽입

삽입 메서드에 이미 DOM에 있는 요소를 전달하면 복사가 아닌 **이동**이 된다.

```javascript
// #item을 #list의 마지막으로 이동
$('#list').append($('#item'));
```

---

## 3.6 요소 삭제와 교체

### `remove([selector])`

선택한 요소를 DOM에서 완전히 제거한다. 이벤트 핸들러와 데이터도 함께 제거된다.

```javascript
$('#notice').remove();              // #notice 제거
$('li').remove('.completed');       // li 중 .completed만 제거
```

---

### `detach([selector])`

`remove()`와 동일하게 DOM에서 제거하지만, **이벤트 핸들러와 데이터를 보존**한다. 나중에 다시 삽입할 계획이 있을 때 사용한다.

```javascript
const $item = $('li.dragging').detach();   // 이벤트 유지하며 분리
// ... 다른 작업 ...
$('#new-list').append($item);              // 다른 곳에 다시 삽입
```

---

### `empty()`

선택한 요소의 **모든 자식 요소와 텍스트를 제거**한다. 요소 자체는 유지된다.

```javascript
$('#result').empty();   // #result 안의 내용만 비움, #result 태그는 남음
```

---

### `replaceWith(content)`

선택한 요소를 다른 콘텐츠로 **교체**한다.

```javascript
$('#old-btn').replaceWith('<button id="new-btn">새 버튼</button>');
```

---

### `remove()` vs `detach()` vs `empty()` 비교

| 메서드 | 요소 자체 | 자식 요소 | 이벤트/데이터 |
|--------|-----------|-----------|---------------|
| `remove()` | 제거 | 제거 | 제거 |
| `detach()` | 제거 | 제거 | **유지** |
| `empty()` | **유지** | 제거 | 제거 |

---

## 3.7 요소 복사: `clone()`

선택한 요소를 복사하여 새 jQuery 객체로 반환한다. 원본은 그대로 남는다.

```javascript
// 기본 복사 (이벤트 핸들러 미포함)
const $copy = $('#template').clone();
$('#list').append($copy);

// 이벤트 핸들러까지 복사
const $copy = $('#template').clone(true);
$('#list').append($copy);
```

`clone(true)`를 사용하면 이벤트 핸들러와 데이터가 복사된 요소에도 그대로 적용된다.

---

### `clone()` 활용 예시 — 동적 입력 필드 추가

```html
<div id="field-template" style="display:none;">
  <div class="field-row">
    <input type="text" placeholder="이름">
    <button class="btn-remove">삭제</button>
  </div>
</div>
<div id="field-container"></div>
<button id="btn-add">+ 필드 추가</button>
```

```javascript
$(function() {
  // 필드 추가
  $('#btn-add').on('click', function() {
    const $newField = $('#field-template .field-row').clone(true);
    $('#field-container').append($newField);
  });

  // 삭제 버튼 (clone(true)로 복사되었으므로 별도 이벤트 불필요)
  $('#field-template .btn-remove').on('click', function() {
    $(this).closest('.field-row').remove();
  });
});
```

---

## 3.8 `wrap()` / `unwrap()`

### `wrap(wrapper)`

선택한 각 요소를 지정한 요소로 감싼다.

```javascript
// 각 p를 div.box로 감싸기
$('p').wrap('<div class="box"></div>');

// 결과:
// <div class="box"><p>텍스트</p></div>
// <div class="box"><p>텍스트</p></div>
```

### `wrapAll(wrapper)`

선택한 모든 요소를 하나의 wrapper로 함께 감싼다.

```javascript
$('p').wrapAll('<div class="container"></div>');

// 결과:
// <div class="container">
//   <p>텍스트</p>
//   <p>텍스트</p>
// </div>
```

### `unwrap()`

선택한 요소의 부모를 제거한다(부모의 자리에 선택 요소가 올라온다).

```javascript
$('p').unwrap();   // p의 부모 div 제거
```

---

## 3.9 실습: To-Do 항목 CRUD

다음은 DOM 조작 메서드를 종합 활용하는 To-Do 예제다. 6장 프로젝트 ①의 사전 실습으로 활용한다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>DOM 조작 실습</title>
  <style>
    .done { text-decoration: line-through; color: #aaa; }
    li { margin: 6px 0; }
  </style>
</head>
<body>
  <h2>To-Do 실습</h2>
  <input type="text" id="todo-input" placeholder="할 일 입력">
  <button id="btn-add">추가</button>
  <ul id="todo-list"></ul>

  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script>
    $(function() {

      // 항목 추가
      $('#btn-add').on('click', function() {
        const text = $('#todo-input').val().trim();
        if (!text) return;

        const $li = $('<li>').text(text);
        const $doneBtn = $('<button>').text('완료').addClass('btn-done');
        const $delBtn  = $('<button>').text('삭제').addClass('btn-del');

        $li.append(' ', $doneBtn, ' ', $delBtn);
        $('#todo-list').append($li);
        $('#todo-input').val('').focus();
      });

      // 완료 토글
      $('#todo-list').on('click', '.btn-done', function() {
        $(this).closest('li').toggleClass('done');
      });

      // 항목 삭제
      $('#todo-list').on('click', '.btn-del', function() {
        $(this).closest('li').remove();
      });

    });
  </script>
</body>
</html>
```

**코드 포인트 분석:**

| 코드 | 사용 메서드 | 역할 |
|------|-------------|------|
| `$('<li>').text(text)` | `text()` | 새 li 생성 및 텍스트 설정 |
| `$li.append(...)` | `append()` | 버튼을 li 안에 삽입 |
| `$('#todo-list').append($li)` | `append()` | 완성된 li를 목록에 추가 |
| `$('#todo-input').val('')` | `val()` | 입력 필드 초기화 |
| `$(this).closest('li').toggleClass('done')` | `toggleClass()` | 완료 스타일 토글 |
| `$(this).closest('li').remove()` | `remove()` | 항목 삭제 |

---

## 3.10 정리

### 콘텐츠 조작

| 메서드 | 읽기 | 쓰기 | 대상 |
|--------|------|------|------|
| `html()` | ✓ | ✓ | innerHTML (HTML 해석) |
| `text()` | ✓ | ✓ | textContent (HTML 미해석) |
| `val()` | ✓ | ✓ | 폼 요소 value |

### 속성·클래스 조작

| 메서드 | 설명 |
|--------|------|
| `attr(name)` / `attr(name, value)` | HTML 속성 읽기/쓰기 |
| `removeAttr(name)` | 속성 제거 |
| `prop(name)` / `prop(name, value)` | DOM 프로퍼티 읽기/쓰기 |
| `addClass(name)` | 클래스 추가 |
| `removeClass(name)` | 클래스 제거 |
| `toggleClass(name)` | 클래스 토글 |
| `hasClass(name)` | 클래스 존재 여부 확인 |

### 구조 조작

| 메서드 | 설명 |
|--------|------|
| `append()` / `prepend()` | 마지막/첫 번째 자식으로 삽입 |
| `after()` / `before()` | 형제로 삽입 |
| `remove()` | 요소 완전 제거 |
| `detach()` | 이벤트 보존하며 제거 |
| `empty()` | 자식 요소만 제거 |
| `clone([true])` | 요소 복사 |
| `replaceWith()` | 요소 교체 |
| `wrap()` / `unwrap()` | 요소 감싸기/벗기기 |

---

## 연습 문제

1. 아래 요소의 `<a>` 태그의 `href`를 `https://www.google.com`으로 바꾸고, 링크 텍스트는 `"구글"` 로 변경하는 코드를 작성하라.
   ```html
   <p id="link-wrap"><a id="mylink" href="#">링크</a></p>
   ```

2. `<input type="checkbox" id="all-check">` 체크박스를 클릭할 때마다 페이지 내 모든 `.item-check` 체크박스의 체크 상태가 동일하게 따라가도록 구현하라.

3. 버튼을 클릭할 때마다 `<ul id="log">` 안에 현재 시각을 텍스트로 담은 `<li>`를 **맨 위에** 추가하는 코드를 작성하라.

4. `<table id="data-table">` 안의 행(tr)을 클릭하면 해당 행이 삭제되도록 구현하라. 단, 이벤트 위임(event delegation) 방식을 사용하라.

5. `#source-list`의 마지막 `<li>`를 복사하여 `#target-list`의 첫 번째 자식으로 삽입하는 코드를 작성하라.

---

> **다음 장 예고**  
> 4장에서는 jQuery의 **이벤트 처리** 시스템을 학습한다. `on()`, `off()`, `one()`의 사용법과 이벤트 위임의 원리, 그리고 실무에서 자주 쓰이는 이벤트 종류를 익힌다.
