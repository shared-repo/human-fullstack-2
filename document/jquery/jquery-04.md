# 4장. 이벤트 처리

> 학습 목표
> - `on()`, `off()`, `one()`으로 이벤트를 등록하고 해제할 수 있다.
> - 이벤트 위임(Event Delegation)의 원리를 이해하고 동적 요소에 적용할 수 있다.
> - click, change, keyup, submit, focus/blur 등 실무 이벤트를 능숙하게 활용할 수 있다.
> - 이벤트 객체를 통해 `preventDefault()`와 `stopPropagation()`을 적절히 사용할 수 있다.

---

## 4.1 이벤트 처리란?

이벤트(Event)는 사용자 행동(클릭, 입력, 스크롤 등)이나 브라우저 동작(페이지 로드, 리사이즈 등)에 의해 발생하는 신호다. 이벤트 처리(Event Handling)는 특정 이벤트가 발생했을 때 실행할 함수(핸들러)를 등록하는 작업이다.

jQuery 이전에는 `onclick="..."` 같은 HTML 인라인 이벤트나 `element.addEventListener()`를 직접 사용했다. jQuery는 `on()` 메서드 하나로 모든 이벤트를 일관되게 처리하며 브라우저 호환성도 보장한다.

---

## 4.2 `on()` — 이벤트 등록

`on()`은 jQuery에서 이벤트를 등록하는 핵심 메서드다.

### 기본 문법

```javascript
$(선택자).on(이벤트명, 핸들러함수);
```

```javascript
$('#btn').on('click', function() {
  console.log('버튼이 클릭되었습니다.');
});
```

---

### 여러 이벤트에 같은 핸들러 등록

공백으로 구분하여 여러 이벤트에 동일한 핸들러를 등록할 수 있다.

```javascript
$('input').on('focus blur', function() {
  $(this).toggleClass('focused');
});
```

---

### 여러 이벤트에 각각 다른 핸들러 등록

객체(Object) 형태로 전달하면 이벤트마다 다른 핸들러를 한 번에 등록할 수 있다.

```javascript
$('#box').on({
  mouseenter: function() {
    $(this).css('background', '#e0f0ff');
  },
  mouseleave: function() {
    $(this).css('background', '');
  },
  click: function() {
    $(this).toggleClass('selected');
  }
});
```

---

### 단축 메서드

자주 쓰는 이벤트에 대해 jQuery는 `on()`의 단축 메서드를 제공한다.

```javascript
// 아래 두 코드는 완전히 동일
$('#btn').on('click', handler);
$('#btn').click(handler);
```

| 단축 메서드 | 동등한 `on()` |
|-------------|--------------|
| `.click(fn)` | `.on('click', fn)` |
| `.dblclick(fn)` | `.on('dblclick', fn)` |
| `.keyup(fn)` | `.on('keyup', fn)` |
| `.change(fn)` | `.on('change', fn)` |
| `.submit(fn)` | `.on('submit', fn)` |
| `.focus(fn)` | `.on('focus', fn)` |
| `.blur(fn)` | `.on('blur', fn)` |
| `.hover(fn1, fn2)` | `.on('mouseenter', fn1).on('mouseleave', fn2)` |

> **실무 권장:** 단축 메서드보다 `on()`을 사용하는 것이 의도가 명확하고 이벤트 위임(4.4절)과도 일관성을 유지할 수 있다.

---

## 4.3 `off()` / `one()`

### `off()` — 이벤트 해제

등록한 이벤트 핸들러를 제거한다.

```javascript
// 특정 이벤트 해제
$('#btn').off('click');

// 특정 핸들러만 해제 (핸들러를 변수로 분리해야 함)
function handleClick() {
  console.log('클릭');
}
$('#btn').on('click', handleClick);
$('#btn').off('click', handleClick);   // handleClick만 제거

// 모든 이벤트 해제
$('#btn').off();
```

---

### 네임스페이스를 이용한 이벤트 관리

이벤트 이름 뒤에 `.네임스페이스`를 붙이면 핸들러 함수를 변수로 분리하지 않아도 특정 핸들러만 정밀하게 해제할 수 있다.

```javascript
// 등록
$('#btn').on('click.myModule', function() {
  console.log('myModule의 클릭 핸들러');
});
$('#btn').on('click.otherModule', function() {
  console.log('otherModule의 클릭 핸들러');
});

// myModule의 click만 해제 (otherModule은 유지)
$('#btn').off('click.myModule');

// 네임스페이스 전체 해제
$('#btn').off('.myModule');
```

---

### `one()` — 한 번만 실행되는 이벤트

이벤트가 처음 발생할 때 한 번만 핸들러를 실행하고 자동으로 해제한다.

```javascript
$('#btn-intro').one('click', function() {
  alert('처음 방문을 환영합니다!');
  // 이후 클릭에는 반응하지 않음
});
```

---

## 4.4 이벤트 위임(Event Delegation)

### 문제 상황: 동적으로 추가된 요소

이벤트를 등록한 이후에 동적으로 추가된 요소에는 이벤트가 적용되지 않는다.

```javascript
// 기존 li에만 이벤트가 등록됨
$('li').on('click', function() {
  $(this).toggleClass('done');
});

// 이후 동적으로 추가된 li는 이벤트가 없음
$('#list').append('<li>나중에 추가된 항목</li>');  // 클릭해도 반응 없음
```

---

### 이벤트 위임 원리

이벤트 버블링(Event Bubbling) 개념을 활용한다. 자식 요소에서 발생한 이벤트는 DOM 트리를 따라 부모 요소로 전파(버블링)된다. 이를 이용해 **정적인 부모 요소**에 이벤트를 등록하고, 실제 이벤트 발생 대상(자식)을 필터링한다.

```
클릭 발생: <li>나중에 추가된 항목</li>
    ↓ 버블링
<ul id="list">  ← 여기에 이벤트가 등록되어 있음
    ↓ 버블링
<div id="container">
    ↓ 버블링
<body>
```

---

### `on()`으로 이벤트 위임 구현

```javascript
// 문법: $(부모).on(이벤트, '자식 선택자', 핸들러)
$('#list').on('click', 'li', function() {
  $(this).toggleClass('done');
});

// 이후 동적으로 추가된 li도 정상 동작
$('#list').append('<li>나중에 추가된 항목</li>');
```

핵심은 두 번째 인수로 **자식 선택자 문자열**을 전달하는 것이다. jQuery가 버블링된 이벤트를 받아 실제 클릭 대상이 해당 선택자와 일치하는지 확인한다.

---

### 이벤트 위임이 유리한 경우

| 상황 | 이유 |
|------|------|
| Ajax로 동적 요소를 추가하는 경우 | 추가 시점과 무관하게 이벤트 적용 |
| 같은 이벤트를 가진 요소가 매우 많은 경우 | 핸들러 수를 줄여 메모리 절약 |
| 이벤트 등록 시점에 요소가 없는 경우 | 부모만 있어도 위임 가능 |

---

### 이벤트 위임 예시

```javascript
$(function() {
  // To-Do 리스트 이벤트 위임
  $('#todo-list').on('click', '.btn-done', function() {
    $(this).closest('li').toggleClass('done');
  });

  $('#todo-list').on('click', '.btn-del', function() {
    $(this).closest('li').remove();
  });

  // 동적으로 항목 추가해도 위에 등록한 이벤트가 정상 동작
  $('#btn-add').on('click', function() {
    $('#todo-list').append(`
      <li>
        새 항목
        <button class="btn-done">완료</button>
        <button class="btn-del">삭제</button>
      </li>
    `);
  });
});
```

---

## 4.5 자주 쓰는 이벤트

### 마우스 이벤트

| 이벤트 | 발생 시점 |
|--------|-----------|
| `click` | 마우스 클릭 (버튼 누르고 뗄 때) |
| `dblclick` | 더블 클릭 |
| `mouseenter` | 요소 안으로 마우스 진입 (자식 요소 이동 시 재발생 안 함) |
| `mouseleave` | 요소 밖으로 마우스 이탈 (자식 요소 이동 시 재발생 안 함) |
| `mouseover` | 마우스 진입 (자식 요소 이동 시에도 재발생) |
| `mouseout` | 마우스 이탈 (자식 요소 이동 시에도 재발생) |
| `mousemove` | 요소 위에서 마우스 이동 |
| `contextmenu` | 우클릭 (컨텍스트 메뉴 열릴 때) |

```javascript
// hover: mouseenter + mouseleave 조합
$('.card').on('mouseenter', function() {
  $(this).addClass('hovered');
}).on('mouseleave', function() {
  $(this).removeClass('hovered');
});

// jQuery .hover() 단축 메서드
$('.card').hover(
  function() { $(this).addClass('hovered'); },
  function() { $(this).removeClass('hovered'); }
);
```

> `mouseover`/`mouseout`은 자식 요소로 이동할 때도 이벤트가 재발생하여 예기치 않은 깜박임이 생길 수 있다. 실무에서는 대부분 `mouseenter`/`mouseleave`를 사용한다.

---

### 키보드 이벤트

| 이벤트 | 발생 시점 |
|--------|-----------|
| `keydown` | 키를 누르는 순간 |
| `keypress` | 키를 누르고 있을 때 (문자 입력 시) — deprecated |
| `keyup` | 키를 뗄 때 |

```javascript
// Enter 키 감지
$('#search-input').on('keyup', function(e) {
  if (e.key === 'Enter') {
    doSearch($(this).val());
  }
});

// 실시간 글자 수 표시
$('#content').on('keyup', function() {
  const len = $(this).val().length;
  $('#char-count').text(len + '자');
});
```

> `keypress`는 deprecated(지원 중단 예정)이므로 `keydown` 또는 `keyup`을 사용한다.

---

### 폼 이벤트

| 이벤트 | 발생 시점 |
|--------|-----------|
| `focus` | 요소에 포커스가 생겼을 때 |
| `blur` | 요소에서 포커스가 사라질 때 |
| `change` | 값이 변경되고 포커스를 잃었을 때 (input, select, checkbox) |
| `input` | 값이 변경될 때마다 즉시 (실시간) |
| `submit` | 폼이 제출될 때 |
| `reset` | 폼이 리셋될 때 |

```javascript
// focus / blur: 플레이스홀더 대체 텍스트 효과
$('input').on('focus', function() {
  $(this).addClass('active');
  if ($(this).val() === $(this).data('placeholder')) {
    $(this).val('');
  }
}).on('blur', function() {
  $(this).removeClass('active');
  if ($(this).val() === '') {
    $(this).val($(this).data('placeholder'));
  }
});

// change: select 변경 감지
$('select#category').on('change', function() {
  const selected = $(this).val();
  loadSubCategory(selected);
});

// input: 실시간 검색 (change보다 즉각 반응)
$('#live-search').on('input', function() {
  filterList($(this).val());
});

// submit: 폼 제출 가로채기
$('#login-form').on('submit', function(e) {
  e.preventDefault();   // 기본 제출 동작 차단
  const data = {
    id: $('#username').val(),
    pw: $('#password').val()
  };
  sendLoginRequest(data);
});
```

---

### 문서/창 이벤트

| 이벤트 | 발생 시점 |
|--------|-----------|
| `ready` | DOM 로드 완료 |
| `scroll` | 스크롤 발생 시 |
| `resize` | 창 크기 변경 시 |
| `load` | 리소스(이미지 등) 로드 완료 |

```javascript
// 스크롤 위치에 따라 상단 버튼 표시
$(window).on('scroll', function() {
  if ($(this).scrollTop() > 300) {
    $('#btn-top').fadeIn();
  } else {
    $('#btn-top').fadeOut();
  }
});

// 창 리사이즈 감지
$(window).on('resize', function() {
  console.log('너비: ' + $(this).width());
});
```

---

## 4.6 이벤트 객체

이벤트 핸들러 함수는 첫 번째 인수로 **이벤트 객체(Event Object)**를 받는다. 이벤트가 어디서 발생했는지, 어떤 키를 눌렀는지 등의 정보를 담고 있다.

```javascript
$('#btn').on('click', function(e) {
  console.log(e);   // jQuery 이벤트 객체
});
```

---

### 주요 이벤트 객체 프로퍼티

| 프로퍼티 | 설명 |
|----------|------|
| `e.type` | 이벤트 종류 (`"click"`, `"keyup"` 등) |
| `e.target` | 이벤트가 실제 발생한 DOM 요소 |
| `e.currentTarget` | 이벤트 핸들러가 등록된 요소 (`this`와 동일) |
| `e.pageX` / `e.pageY` | 페이지 기준 마우스 좌표 |
| `e.clientX` / `e.clientY` | 뷰포트 기준 마우스 좌표 |
| `e.key` | 눌린 키 이름 (`"Enter"`, `"ArrowUp"` 등) |
| `e.which` | 눌린 키 코드 (숫자) — deprecated |
| `e.altKey` | Alt 키 눌림 여부 (boolean) |
| `e.ctrlKey` | Ctrl 키 눌림 여부 (boolean) |
| `e.shiftKey` | Shift 키 눌림 여부 (boolean) |
| `e.timeStamp` | 이벤트 발생 시각 (밀리초) |

```javascript
// 마우스 좌표 추적
$(document).on('mousemove', function(e) {
  $('#coords').text('X: ' + e.pageX + ', Y: ' + e.pageY);
});

// Ctrl+S 감지
$(document).on('keydown', function(e) {
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault();   // 브라우저 저장 다이얼로그 차단
    saveDocument();
  }
});

// target vs currentTarget 차이
$('#parent').on('click', function(e) {
  console.log(e.target);         // 실제 클릭된 요소 (자식일 수 있음)
  console.log(e.currentTarget);  // 핸들러가 등록된 #parent
  console.log(this);             // e.currentTarget과 동일
});
```

---

### `e.target` 활용 — 이벤트 위임과 조합

```javascript
$('#toolbar').on('click', function(e) {
  const $clicked = $(e.target);

  if ($clicked.hasClass('btn-bold')) {
    applyBold();
  } else if ($clicked.hasClass('btn-italic')) {
    applyItalic();
  } else if ($clicked.hasClass('btn-underline')) {
    applyUnderline();
  }
});
```

---

## 4.7 `preventDefault()` — 기본 동작 차단

브라우저는 특정 요소에 대해 기본 동작을 수행한다. `preventDefault()`는 이 기본 동작을 차단한다.

| 요소/이벤트 | 기본 동작 | 차단 시 |
|-------------|-----------|---------|
| `<a>` + click | href로 페이지 이동 | 이동 안 함 |
| `<form>` + submit | 서버로 폼 데이터 전송 | 전송 안 함 |
| `<input type="checkbox">` + click | 체크/해제 | 상태 변경 안 함 |
| `scroll` / `wheel` | 페이지 스크롤 | 스크롤 안 됨 |
| `contextmenu` | 우클릭 메뉴 표시 | 메뉴 안 뜸 |

```javascript
// 앵커 클릭 시 페이지 이동 차단 후 직접 처리
$('a.ajax-link').on('click', function(e) {
  e.preventDefault();
  const url = $(this).attr('href');
  loadContent(url);   // Ajax로 콘텐츠 로드
});

// 폼 제출 차단 후 유효성 검사
$('#register-form').on('submit', function(e) {
  e.preventDefault();
  if (validateForm()) {
    this.submit();    // 유효하면 실제 제출
  }
});

// 우클릭 커스텀 메뉴
$('#canvas').on('contextmenu', function(e) {
  e.preventDefault();
  showCustomMenu(e.pageX, e.pageY);
});
```

---

## 4.8 `stopPropagation()` — 이벤트 전파 차단

이벤트 버블링을 중단하여 이벤트가 상위 요소로 전파되지 않게 한다.

### 버블링 문제 예시

```html
<div id="outer">
  외부 영역
  <div id="inner">
    내부 영역 (클릭 시 outer도 반응)
  </div>
</div>
```

```javascript
$('#outer').on('click', function() {
  console.log('outer 클릭');
});

$('#inner').on('click', function() {
  console.log('inner 클릭');
  // inner를 클릭하면 버블링으로 outer 이벤트도 실행됨
});
```

inner를 클릭하면 콘솔에 `"inner 클릭"`, `"outer 클릭"` 모두 출력된다.

---

### `stopPropagation()`으로 해결

```javascript
$('#inner').on('click', function(e) {
  e.stopPropagation();   // 버블링 중단
  console.log('inner 클릭만 처리');
});
```

---

### 모달 닫기 패턴 — 실무 활용

```javascript
// 배경 클릭 시 모달 닫기, 모달 내부 클릭은 닫히지 않아야 함
$('#modal-overlay').on('click', function() {
  closeModal();
});

$('#modal-content').on('click', function(e) {
  e.stopPropagation();   // 모달 내부 클릭이 overlay로 전파되지 않도록
});
```

---

### `stopImmediatePropagation()`

같은 요소에 등록된 다른 핸들러의 실행까지 중단한다.

```javascript
$('#btn').on('click', function(e) {
  e.stopImmediatePropagation();
  console.log('첫 번째 핸들러 — 이후 핸들러 실행 안 됨');
});

$('#btn').on('click', function() {
  console.log('두 번째 핸들러 — 실행되지 않음');
});
```

---

### `return false`

jQuery에서 핸들러에서 `return false`를 반환하면 `preventDefault()`와 `stopPropagation()`을 동시에 수행한다.

```javascript
$('a').on('click', function() {
  return false;   // e.preventDefault() + e.stopPropagation() 동시 실행
});
```

> 간편하지만 의도가 불명확해질 수 있다. 명시적으로 `e.preventDefault()` / `e.stopPropagation()`을 따로 호출하는 것을 권장한다.

---

### `preventDefault()` vs `stopPropagation()` 비교

| 메서드 | 차단 대상 | 버블링 | 기본 동작 |
|--------|-----------|--------|-----------|
| `preventDefault()` | 브라우저 기본 동작 | 계속됨 | 차단 |
| `stopPropagation()` | 이벤트 전파 | 중단 | 계속됨 |
| `return false` | 둘 다 | 중단 | 차단 |

---

## 4.9 `trigger()` — 이벤트 강제 발생

코드에서 특정 이벤트를 프로그래밍적으로 발생시킨다.

```javascript
// 버튼 클릭을 코드로 강제 실행
$('#btn').trigger('click');

// 폼 submit 강제 실행
$('#login-form').trigger('submit');

// 단축 메서드 형태
$('#btn').click();   // trigger('click')과 동일

// 커스텀 이벤트 발생
$('#item').trigger('itemUpdated');
$('#item').on('itemUpdated', function() {
  refreshView();
});
```

---

## 4.10 커스텀 이벤트

jQuery의 `trigger()`와 `on()`을 활용하면 개발자가 직접 이벤트를 정의하고 발생시킬 수 있다. 컴포넌트 간 통신에 유용하다.

```javascript
// 커스텀 이벤트 등록
$(document).on('userLoggedIn', function(e, userData) {
  $('#welcome').text('환영합니다, ' + userData.name + '님!');
  $('#login-area').hide();
  $('#user-area').show();
});

// 커스텀 이벤트 발생 (데이터 전달 가능)
$(document).trigger('userLoggedIn', [{ name: '홍길동', role: 'admin' }]);
```

---

## 4.11 실습: 이벤트 종합 예제

다음 예제는 이 장에서 학습한 주요 이벤트 기법을 종합한다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>이벤트 처리 실습</title>
  <style>
    #search-box { border: 2px solid #ccc; padding: 6px; }
    #search-box.active { border-color: #007bff; }
    .result-item { padding: 8px; cursor: pointer; border-bottom: 1px solid #eee; }
    .result-item:hover { background: #f5f5f5; }
    .result-item.selected { background: #d0e8ff; }
    #modal-overlay {
      display: none; position: fixed; inset: 0;
      background: rgba(0,0,0,0.5);
    }
    #modal-content {
      background: #fff; width: 400px; margin: 100px auto;
      padding: 24px; border-radius: 8px;
    }
  </style>
</head>
<body>

  <h2>이벤트 실습</h2>

  <!-- 실시간 검색 -->
  <input type="text" id="search-box" placeholder="검색어 입력...">
  <div id="result-list"></div>

  <!-- 모달 -->
  <button id="btn-open-modal">상세 보기</button>
  <div id="modal-overlay">
    <div id="modal-content">
      <h3 id="modal-title">제목</h3>
      <p id="modal-body">내용</p>
      <button id="btn-close-modal">닫기</button>
    </div>
  </div>

  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script>
    $(function() {
      const items = ['Apple', 'Banana', 'Cherry', 'Date', 'Elderberry'];

      // ① 실시간 검색 — input 이벤트
      $('#search-box').on('input', function() {
        const keyword = $(this).val().toLowerCase();
        const $list = $('#result-list').empty();

        if (!keyword) return;

        items
          .filter(name => name.toLowerCase().includes(keyword))
          .forEach(name => {
            $('<div>').addClass('result-item').text(name).appendTo($list);
          });
      });

      // ② focus / blur
      $('#search-box').on('focus', function() {
        $(this).addClass('active');
      }).on('blur', function() {
        $(this).removeClass('active');
      });

      // ③ 이벤트 위임 — 동적으로 생성된 결과 아이템 클릭
      $('#result-list').on('click', '.result-item', function() {
        $('.result-item').removeClass('selected');
        $(this).addClass('selected');

        // 커스텀 이벤트로 모달에 데이터 전달
        $(document).trigger('itemSelected', [$(this).text()]);
      });

      // ④ 커스텀 이벤트 수신 — 모달 열기
      $(document).on('itemSelected', function(e, itemName) {
        $('#modal-title').text(itemName);
        $('#modal-body').text(itemName + '에 대한 상세 정보입니다.');
        $('#modal-overlay').fadeIn(200);
      });

      // ⑤ 모달 배경 클릭 시 닫기
      $('#modal-overlay').on('click', function() {
        $(this).fadeOut(200);
      });

      // ⑥ 모달 내부 클릭 — stopPropagation
      $('#modal-content').on('click', function(e) {
        e.stopPropagation();
      });

      // ⑦ 닫기 버튼
      $('#btn-close-modal').on('click', function() {
        $('#modal-overlay').fadeOut(200);
      });

      // ⑧ ESC 키로 모달 닫기
      $(document).on('keydown', function(e) {
        if (e.key === 'Escape') {
          $('#modal-overlay').fadeOut(200);
        }
      });
    });
  </script>
</body>
</html>
```

**코드 포인트 분석:**

| 번호 | 기법 | 설명 |
|------|------|------|
| ① | `input` 이벤트 | 값 변경 즉시 감지 (change보다 빠름) |
| ② | `focus` / `blur` | 포커스 상태 CSS 표현 |
| ③ | 이벤트 위임 | 동적 생성 요소에 클릭 이벤트 적용 |
| ④ | 커스텀 이벤트 | 컴포넌트 간 데이터 전달 |
| ⑤ | 배경 클릭 닫기 | 모달 UX 패턴 |
| ⑥ | `stopPropagation()` | 모달 내부 클릭이 배경으로 전파 차단 |
| ⑦ | 명시적 닫기 버튼 | 접근성 고려 |
| ⑧ | `keydown` + `e.key` | 키보드 UX 지원 |

---

## 4.12 정리

### 이벤트 등록·해제

| 메서드 | 설명 |
|--------|------|
| `on(event, handler)` | 이벤트 등록 |
| `on(event, selector, handler)` | 이벤트 위임 |
| `off(event)` | 이벤트 해제 |
| `one(event, handler)` | 한 번만 실행되는 이벤트 |
| `trigger(event)` | 이벤트 강제 발생 |

### 이벤트 객체 주요 메서드

| 메서드 | 설명 |
|--------|------|
| `e.preventDefault()` | 브라우저 기본 동작 차단 |
| `e.stopPropagation()` | 이벤트 버블링 차단 |
| `e.stopImmediatePropagation()` | 다른 핸들러 실행까지 중단 |

### 자주 쓰는 이벤트

| 분류 | 이벤트 |
|------|--------|
| 마우스 | `click`, `dblclick`, `mouseenter`, `mouseleave` |
| 키보드 | `keydown`, `keyup` |
| 폼 | `focus`, `blur`, `change`, `input`, `submit` |
| 창/문서 | `scroll`, `resize`, `load` |

---

## 연습 문제

1. `<button id="toggle-btn">` 클릭 시 `<div id="panel">`을 보이거나 숨기는 코드를 `toggleClass()`를 사용하지 않고 `on()`과 DOM 조작만으로 구현하라.

2. `<ul id="dynamic-list">`에 동적으로 `<li>`를 추가하는 버튼이 있다. 추가된 `<li>`를 클릭하면 해당 항목이 삭제되도록 이벤트 위임 방식으로 구현하라.

3. `<form id="search-form">`의 submit 기본 동작을 차단하고, 입력값이 비어 있으면 경고 메시지를 표시하고, 입력값이 있으면 `#result` 요소에 "검색어: [입력값]" 텍스트를 출력하는 코드를 작성하라.

4. `<div id="box">`에 마우스를 올리면 `"진입"`, 내리면 `"이탈"` 텍스트를 `<p id="log">`에 추가하되, 5번 이상 기록되면 가장 오래된 기록부터 자동 삭제되도록 구현하라.

5. `$(document)`에 `'dataReady'`라는 커스텀 이벤트를 등록하고, 3초 후에 `trigger()`로 해당 이벤트를 발생시키면 `#output`에 `"데이터 준비 완료"` 텍스트가 표시되도록 구현하라.

---

> **다음 장 예고**  
> 5장에서는 jQuery로 **CSS와 스타일**을 동적으로 제어하는 방법을 학습한다. `css()` 메서드를 비롯해 요소의 크기·위치·스크롤을 다루는 메서드를 익힌다.
