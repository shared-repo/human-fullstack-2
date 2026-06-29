# 2장. 선택자(Selector)

> 학습 목표
> - CSS 선택자를 jQuery에서 그대로 활용할 수 있다.
> - jQuery 전용 확장 선택자의 종류와 사용법을 익힌다.
> - `filter()`, `find()`, `closest()`, `siblings()` 등 탐색 메서드로 원하는 요소를 정확하게 선택할 수 있다.

---

## 2.1 선택자란?

jQuery에서 선택자(Selector)는 `$()` 함수에 전달하는 **문자열 인수**다. 어떤 HTML 요소를 대상으로 작업할지 지정하는 역할을 하며, CSS 선택자 문법을 그대로 사용하는 것이 기본이다.

```javascript
$('선택자')   // 선택자에 해당하는 모든 요소를 jQuery 객체로 반환
```

jQuery 선택자는 크게 세 가지로 분류된다.

1. **CSS 선택자** — CSS에서 사용하는 선택자를 그대로 사용
2. **jQuery 확장 선택자** — jQuery가 추가로 제공하는 `:eq()`, `:odd` 등
3. **탐색 메서드** — 선택한 요소를 기준으로 관련 요소를 찾는 메서드

---

## 2.2 CSS 선택자 활용

CSS를 학습했다면 jQuery 선택자의 기본은 이미 알고 있는 것과 같다. jQuery는 CSS의 모든 선택자를 지원한다.

### 기본 선택자

| 선택자 | 예시 | 설명 |
|--------|------|------|
| 전체 선택자 | `$('*')` | 모든 요소 |
| 태그 선택자 | `$('p')` | 모든 `<p>` 요소 |
| ID 선택자 | `$('#header')` | id가 header인 요소 |
| 클래스 선택자 | `$('.item')` | class가 item인 모든 요소 |
| 다중 선택자 | `$('h1, h2, h3')` | h1, h2, h3 모두 선택 |

```javascript
$('p').css('color', 'gray');           // 모든 p 요소의 글자색 변경
$('#header').text('메인 헤더');         // id=header 요소의 텍스트 변경
$('.item').addClass('highlight');       // class=item인 모든 요소에 클래스 추가
$('h1, h2').css('font-weight', 'bold');// h1과 h2 모두 굵게
```

---

### 계층 선택자

| 선택자 | 예시 | 설명 |
|--------|------|------|
| 자손 선택자 | `$('ul li')` | ul 안의 모든 li (직계 아님) |
| 자식 선택자 | `$('ul > li')` | ul의 직접 자식 li만 |
| 인접 형제 선택자 | `$('h2 + p')` | h2 바로 다음 p |
| 일반 형제 선택자 | `$('h2 ~ p')` | h2 이후 모든 형제 p |

```html
<ul id="menu">
  <li>메뉴 1
    <ul>
      <li>서브 메뉴 1-1</li>  <!-- $('ul li')에는 포함, $('ul > li')에는 미포함 -->
    </ul>
  </li>
  <li>메뉴 2</li>
</ul>
```

```javascript
$('#menu li').css('color', 'blue');      // 모든 li (서브 메뉴 포함)
$('#menu > li').css('color', 'red');     // 직접 자식 li만
```

---

### 속성 선택자

| 선택자 | 설명 |
|--------|------|
| `$('[href]')` | href 속성이 있는 요소 |
| `$('[type="text"]')` | type이 text인 요소 |
| `$('[href^="https"]')` | href가 https로 시작하는 요소 |
| `$('[href$=".pdf"]')` | href가 .pdf로 끝나는 요소 |
| `$('[title*="jquery"]')` | title에 jquery가 포함된 요소 |

```javascript
$('input[type="text"]').css('border', '2px solid blue');
$('a[href^="https"]').css('color', 'green');   // 외부 링크 강조
$('a[href$=".pdf"]').addClass('pdf-link');      // PDF 링크에 클래스 추가
```

---

### 의사 클래스 선택자(CSS 기반)

| 선택자 | 설명 |
|--------|------|
| `$('li:first-child')` | 부모의 첫 번째 자식인 li |
| `$('li:last-child')` | 부모의 마지막 자식인 li |
| `$('li:nth-child(2)')` | 부모의 두 번째 자식인 li |
| `$('p:not(.skip)')` | skip 클래스가 없는 p |
| `$('input:checked')` | 체크된 input |
| `$('input:disabled')` | 비활성화된 input |

```javascript
$('tr:nth-child(odd)').css('background', '#f5f5f5');  // 홀수 행 배경색
$('input:not([type="submit"])').val('');               // submit 제외 input 초기화
```

---

## 2.3 jQuery 확장 선택자

jQuery는 CSS 표준에는 없지만 자주 필요한 선택을 위해 자체 확장 선택자를 제공한다.

> **주의:** jQuery 확장 선택자(`:eq`, `:odd` 등)는 CSS 표준이 아니므로 `document.querySelector()`에서는 사용할 수 없다. jQuery 전용이다.

---

### 인덱스 기반 선택자

jQuery 객체 내 요소의 **인덱스(0부터 시작)**를 기준으로 선택한다.

| 선택자 | 설명 |
|--------|------|
| `:first` | 첫 번째 요소 |
| `:last` | 마지막 요소 |
| `:eq(n)` | 인덱스가 n인 요소 |
| `:lt(n)` | 인덱스가 n보다 작은 요소 |
| `:gt(n)` | 인덱스가 n보다 큰 요소 |
| `:even` | 인덱스가 짝수인 요소 (0, 2, 4...) |
| `:odd` | 인덱스가 홀수인 요소 (1, 3, 5...) |

```html
<ul>
  <li>항목 0</li>   <!-- :first, :even, :eq(0) -->
  <li>항목 1</li>   <!-- :odd, :eq(1) -->
  <li>항목 2</li>   <!-- :even, :eq(2) -->
  <li>항목 3</li>   <!-- :odd, :last, :eq(3) -->
</ul>
```

```javascript
$('li:first').css('color', 'red');       // 항목 0
$('li:last').css('color', 'blue');       // 항목 3
$('li:eq(2)').css('font-weight', 'bold'); // 항목 2
$('li:even').css('background', '#eee');  // 항목 0, 2 (인덱스 기준)
$('li:odd').css('background', '#ddd');   // 항목 1, 3
$('li:lt(2)').hide();                    // 항목 0, 1 숨기기
$('li:gt(1)').show();                    // 항목 2, 3 보이기
```

> `:even`과 `:odd`는 **인덱스** 기준이다. 화면에서 "첫 번째로 보이는 행"이 인덱스 0(짝수)임에 주의한다.

---

### 콘텐츠 기반 선택자

| 선택자 | 설명 |
|--------|------|
| `:contains(text)` | 특정 텍스트를 포함하는 요소 |
| `:empty` | 자식 요소와 텍스트가 없는 빈 요소 |
| `:has(selector)` | 특정 자손을 가진 요소 |
| `:parent` | 자식 요소나 텍스트가 있는 요소 (`:empty`의 반대) |

```javascript
$('p:contains("jQuery")').css('background', 'yellow'); // "jQuery" 텍스트 포함 p 강조
$('td:empty').text('-');                               // 빈 셀에 대시 표시
$('li:has(ul)').css('list-style', 'none');             // 하위 ul을 가진 li
```

---

### 폼 관련 선택자

폼 요소를 선택할 때 자주 쓰이는 jQuery 전용 선택자다.

| 선택자 | 설명 |
|--------|------|
| `:input` | 모든 입력 요소 (input, select, textarea, button) |
| `:text` | `type="text"` 입력 |
| `:password` | `type="password"` 입력 |
| `:radio` | `type="radio"` 입력 |
| `:checkbox` | `type="checkbox"` 입력 |
| `:submit` | `type="submit"` 버튼 |
| `:selected` | `<option>` 중 선택된 것 |
| `:checked` | 체크된 radio 또는 checkbox |
| `:enabled` | 활성화된 폼 요소 |
| `:disabled` | 비활성화된 폼 요소 |

```javascript
$(':text').val('');                    // 모든 텍스트 입력 초기화
$(':checkbox:checked').each(function() {
  console.log($(this).val());          // 체크된 체크박스 값 출력
});
$('select option:selected').text();    // 선택된 옵션의 텍스트 가져오기
```

---

### 가시성 선택자

| 선택자 | 설명 |
|--------|------|
| `:visible` | 화면에 보이는 요소 |
| `:hidden` | 숨겨진 요소 (`display:none`, `visibility:hidden`, `type="hidden"` 포함) |

```javascript
$('div:visible').css('border', '1px solid red');  // 보이는 div에 테두리
$('div:hidden').show();                           // 숨겨진 div 모두 표시
```

---

## 2.4 탐색 메서드(Traversal Methods)

탐색 메서드는 이미 선택한 요소를 **기준점**으로 삼아 관련 요소(부모, 자식, 형제 등)를 찾는 메서드다. 선택자만으로 복잡한 관계를 표현하기 어려울 때 특히 유용하다.

```
DOM 트리 방향
       ↑  부모(parent, parents, closest)
[기준 요소]
  ↙  ↓  ↘  자식(children, find)
       ↔  형제(siblings, next, prev)
```

---

### 자식/자손 탐색

#### `children([selector])`

직접 자식 요소만 선택한다. 인수로 선택자를 전달하면 필터링된다.

```javascript
$('#list').children();           // #list의 모든 직접 자식
$('#list').children('.active');  // #list의 직접 자식 중 .active만
```

#### `find(selector)`

자손 요소(깊이 무관) 중 선택자에 맞는 것을 모두 찾는다. `children()`과 달리 손자, 증손자까지 탐색한다.

```javascript
$('#container').find('a');         // #container 안의 모든 a 태그
$('#form').find('input:text');     // 폼 안의 모든 텍스트 입력
$('#nav').find('li').addClass('nav-item');
```

> `$('#container a')`와 `$('#container').find('a')`는 결과가 같지만, 체이닝 중간에 탐색 범위를 좁힐 때 `find()`가 유용하다.

---

### 부모 탐색

#### `parent([selector])`

바로 위 부모 하나만 선택한다.

```javascript
$('.child').parent();              // 직접 부모 요소
$('.child').parent('.wrapper');    // 직접 부모 중 .wrapper인 것만
```

#### `parents([selector])`

최상위(html)까지 모든 조상을 선택한다.

```javascript
$('span').parents();               // span의 모든 조상
$('span').parents('div');          // span의 조상 중 div만
```

#### `closest(selector)`

자신을 포함하여 가장 가까운 조상 요소를 찾는다. 일치하는 첫 번째 요소 하나만 반환한다.

```javascript
// 클릭한 버튼이 속한 .card 요소 찾기
$('button.delete').on('click', function() {
  $(this).closest('.card').remove();
});
```

> `parents()`는 조건에 맞는 모든 조상을 반환하고, `closest()`는 **가장 가까운 하나만** 반환한다. 이벤트 핸들러에서 상위 컨테이너를 찾을 때 `closest()`를 가장 많이 쓴다.

| 메서드 | 탐색 방향 | 반환 개수 | 자신 포함 |
|--------|-----------|-----------|-----------|
| `parent()` | 바로 위 | 1개 | ✗ |
| `parents()` | 위쪽 전체 | 여러 개 | ✗ |
| `closest()` | 위쪽 전체 | 1개 | ✓ |

---

### 형제 탐색

#### `siblings([selector])`

같은 부모를 가진 모든 형제 요소를 선택한다. 자기 자신은 제외된다.

```javascript
$('.active').siblings();              // .active의 모든 형제
$('.active').siblings('li');          // .active의 형제 중 li만
```

#### `next([selector])` / `prev([selector])`

바로 다음/이전 형제 하나를 선택한다.

```javascript
$('.current').next();                 // 바로 다음 형제
$('.current').prev();                 // 바로 이전 형제
$('h2').next('p');                    // h2 바로 다음 p 형제
```

#### `nextAll([selector])` / `prevAll([selector])`

다음/이전의 모든 형제를 선택한다.

```javascript
$('.divider').nextAll();              // .divider 이후 모든 형제
$('.divider').prevAll();              // .divider 이전 모든 형제
```

---

### 필터링 메서드

#### `filter(selector | function)`

선택된 요소 집합에서 조건에 맞는 것만 걸러낸다.

```javascript
// 선택자로 필터링
$('li').filter('.active');             // li 중 .active 클래스인 것만

// 함수로 필터링
$('li').filter(function(index) {
  return $(this).text().length > 5;   // 텍스트가 5자 초과인 li만
});
```

#### `not(selector | function)`

`filter()`의 반대. 조건에 해당하지 않는 요소만 남긴다.

```javascript
$('li').not('.disabled');             // .disabled가 아닌 li
$('input').not('[type="submit"]');    // submit 버튼이 아닌 input
```

#### `eq(index)`

인덱스로 단일 요소를 선택한다. 음수 인덱스는 뒤에서부터 계산한다.

```javascript
$('li').eq(0);    // 첫 번째 li
$('li').eq(-1);   // 마지막 li
$('li').eq(2);    // 세 번째 li (인덱스 2)
```

#### `first()` / `last()`

첫 번째 또는 마지막 요소를 선택한다.

```javascript
$('li').first().addClass('first-item');
$('li').last().addClass('last-item');
```

---

### `end()`로 탐색 전 상태로 되돌아가기

체이닝 중 탐색 메서드를 사용하면 선택 대상이 바뀐다. `end()`를 호출하면 이전 선택 상태로 돌아온다.

```javascript
$('#list')
  .find('li')           // li 선택
    .addClass('item')
  .end()                // 다시 #list로 복귀
  .css('border', '1px solid #ccc');
```

---

## 2.5 선택자 성능 최적화

선택자를 어떻게 작성하느냐에 따라 실행 속도가 달라진다.

### 선택자 캐싱

같은 요소를 반복해서 선택하면 매번 DOM을 탐색하므로 비효율적이다. 변수에 저장하여 재사용하자.

```javascript
// 나쁜 예: 같은 요소를 세 번 탐색
$('#list').addClass('active');
$('#list').find('li').css('color', 'red');
$('#list').show();

// 좋은 예: 한 번 선택하여 캐싱
const $list = $('#list');
$list.addClass('active');
$list.find('li').css('color', 'red');
$list.show();
```

### 구체적인 선택자 사용

범위가 넓은 선택자보다 구체적인 선택자가 빠르다.

```javascript
// 느림: 전체 DOM에서 .btn 탐색
$('.btn').hide();

// 빠름: #form 안에서만 탐색
$('#form .btn').hide();
// 또는
$('#form').find('.btn').hide();
```

### jQuery 확장 선택자 주의

`:eq`, `:odd` 같은 jQuery 전용 선택자는 CSS 엔진을 사용할 수 없어 성능이 떨어질 수 있다. 성능이 중요한 경우 `filter()` 메서드와 조합하거나 CSS 의사 클래스로 대체한다.

```javascript
// 느림
$('li:even')

// 더 나은 방법
$('li').filter(':even')
```

---

## 2.6 실습: 선택자 종합 예제

다음 HTML을 기반으로 아래 요구사항을 jQuery 선택자로 구현하라.

```html
<div id="container">
  <h2>공지사항</h2>
  <ul id="notice-list">
    <li class="important">필독 공지 1</li>
    <li>일반 공지 2</li>
    <li class="important">필독 공지 3</li>
    <li>일반 공지 4</li>
    <li class="new">새 공지 5</li>
  </ul>
  <button id="btn-hide">짝수 항목 숨기기</button>
  <button id="btn-highlight">필독 강조</button>
</div>
```

**요구사항:**

1. `.important` 클래스 항목의 글자색을 빨간색으로 변경
2. 인덱스 기준 짝수 항목에 배경색 `#f0f0f0` 적용
3. `#btn-hide` 클릭 시 인덱스 기준 짝수 항목 숨기기
4. `#btn-highlight` 클릭 시 `.important` 항목에 `font-weight: bold` 적용
5. 마지막 `li`의 바로 이전 형제 항목의 텍스트를 콘솔에 출력

**예시 답안:**

```javascript
$(function() {
  // 1. .important 글자색 변경
  $('#notice-list li.important').css('color', 'red');

  // 2. 짝수 인덱스 배경색
  $('#notice-list li:even').css('background', '#f0f0f0');

  // 3. 짝수 항목 숨기기 버튼
  $('#btn-hide').on('click', function() {
    $('#notice-list li:even').toggle();
  });

  // 4. 필독 강조 버튼
  $('#btn-highlight').on('click', function() {
    $('#notice-list').find('.important').css('font-weight', 'bold');
  });

  // 5. 마지막 li의 이전 형제 텍스트 출력
  console.log($('#notice-list li').last().prev().text());
});
```

---

## 2.7 정리

| 분류 | 주요 선택자/메서드 | 핵심 포인트 |
|------|-------------------|-------------|
| 기본 CSS | `#id`, `.class`, `태그`, `,` | CSS 문법 그대로 사용 |
| 계층 CSS | `공백`, `>`, `+`, `~` | 자손 vs 직접 자식 구분 |
| jQuery 확장 | `:eq`, `:odd`, `:even`, `:first`, `:last`, `:lt`, `:gt` | 인덱스 기준, jQuery 전용 |
| 콘텐츠/폼 | `:contains`, `:empty`, `:checked`, `:selected` | 상태·내용 기반 선택 |
| 자식 탐색 | `children()`, `find()` | 직접 자식 vs 모든 자손 |
| 부모 탐색 | `parent()`, `parents()`, `closest()` | closest()가 실무에서 가장 많이 사용 |
| 형제 탐색 | `siblings()`, `next()`, `prev()` | 같은 레벨 요소 탐색 |
| 필터링 | `filter()`, `not()`, `eq()`, `first()`, `last()` | 선택 집합 내 추가 필터 |

---

## 연습 문제

1. `<table>` 안의 짝수 번째 행(tr)에 배경색을 적용하는 코드를 두 가지 방법으로 작성하라. (확장 선택자 사용 / `filter()` 메서드 사용)

2. 클릭한 `<li>` 요소에 `active` 클래스를 추가하고, 나머지 형제 `<li>`에서는 `active` 클래스를 제거하는 코드를 작성하라. (`siblings()` 활용)

3. 아래 코드에서 `find()`와 `children()`의 결과가 다른 이유를 설명하라.
   ```html
   <div id="box">
     <p>직접 자식 p</p>
     <div>
       <p>손자 p</p>
     </div>
   </div>
   ```
   ```javascript
   $('#box').find('p').length;       // ?
   $('#box').children('p').length;   // ?
   ```

4. 폼에서 `type="submit"`을 제외한 모든 `input` 요소의 값을 초기화하는 코드를 작성하라.

---

> **다음 장 예고**  
> 3장에서는 선택한 요소를 실제로 조작하는 **DOM 조작** 메서드를 학습한다. 콘텐츠 읽기/쓰기, 속성 변경, 클래스 토글, 요소 추가·삭제 등 jQuery DOM 조작의 핵심 API를 익힌다.
