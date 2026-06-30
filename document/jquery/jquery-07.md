# 7장. 효과와 애니메이션

> 학습 목표
> - `show()`, `hide()`, `toggle()`로 요소를 표시하거나 숨길 수 있다.
> - `fadeIn()`, `fadeOut()`, `slideUp()`, `slideDown()` 등 내장 효과 메서드를 활용할 수 있다.
> - `animate()`로 CSS 속성을 기반으로 한 커스텀 애니메이션을 구현할 수 있다.
> - `stop()`으로 진행 중인 애니메이션을 제어하고, 콜백으로 순차 실행 흐름을 구성할 수 있다.

---

## 7.1 효과와 애니메이션 개요

jQuery의 효과(Effect) 메서드는 요소를 보이거나 숨기는 작업을 **부드러운 전환 효과**와 함께 처리한다. CSS `transition`이나 `@keyframes` 없이도 간결한 코드 한 줄로 시각적 피드백을 제공할 수 있다.

jQuery 내장 효과는 크게 세 가지로 분류된다.

| 분류 | 메서드 | 특징 |
|------|--------|------|
| 표시/숨김 | `show()`, `hide()`, `toggle()` | 크기·투명도 동시 변화 |
| 페이드 | `fadeIn()`, `fadeOut()`, `fadeTo()`, `fadeToggle()` | 투명도(opacity)만 변화 |
| 슬라이드 | `slideDown()`, `slideUp()`, `slideToggle()` | 높이(height)만 변화 |
| 커스텀 | `animate()` | 원하는 CSS 속성을 직접 지정 |

---

## 7.2 `show()` / `hide()` / `toggle()`

### 기본 사용 — 즉시 표시/숨김

인수 없이 호출하면 애니메이션 없이 즉시 `display` 속성을 변경한다.

```javascript
$('#panel').hide();    // display: none
$('#panel').show();    // display: (원래 값으로 복원)
$('#panel').toggle();  // hide ↔ show 전환
```

> `hide()` 후 `show()`를 호출하면 숨기기 전의 `display` 값(block, flex, inline 등)으로 복원된다. `display: none`을 직접 설정한 경우와 달리 원래 레이아웃이 유지된다.

---

### 애니메이션과 함께 — 지속 시간 지정

첫 번째 인수로 지속 시간(duration)을 지정하면 크기와 투명도가 함께 변화하는 애니메이션이 실행된다.

```javascript
// 밀리초(ms) 단위 숫자
$('#panel').hide(500);    // 0.5초에 걸쳐 숨김
$('#panel').show(1000);   // 1초에 걸쳐 표시

// 문자열 키워드
$('#panel').show('fast');   // 약 200ms
$('#panel').show('normal'); // 약 400ms (기본값)
$('#panel').show('slow');   // 약 600ms
```

---

### 콜백 함수 — 애니메이션 완료 후 실행

두 번째 인수로 콜백 함수를 전달하면 애니메이션이 끝난 뒤 실행된다.

```javascript
$('#panel').hide(400, function() {
  console.log('숨김 완료');
  $(this).remove();   // 숨긴 뒤 DOM에서 제거
});
```

---

### `toggle()` — 상태에 따라 자동 전환

현재 `display` 상태를 감지하여 `show()`와 `hide()`를 자동으로 선택한다.

```javascript
$('#btn-toggle').on('click', function() {
  $('#panel').toggle(300);
});

// 두 번째 인수로 강제 지정 가능
$('#panel').toggle(true);    // 강제 show
$('#panel').toggle(false);   // 강제 hide
```

---

## 7.3 페이드 효과

요소의 **투명도(opacity)**만 변화시킨다. 레이아웃(크기, 위치)은 그대로 유지되므로 오버레이나 알림 메시지에 적합하다.

### `fadeIn()` / `fadeOut()`

```javascript
$('#toast').fadeIn(300);              // 투명 → 불투명
$('#toast').fadeOut(300);             // 불투명 → 투명 (display: none)

// 콜백 조합: 2초 후 서서히 사라지기
setTimeout(function() {
  $('#toast').fadeOut(500, function() {
    $(this).remove();
  });
}, 2000);
```

---

### `fadeToggle()`

현재 가시 상태에 따라 `fadeIn`과 `fadeOut`을 자동 전환한다.

```javascript
$('#btn').on('click', function() {
  $('#panel').fadeToggle(250);
});
```

---

### `fadeTo(duration, opacity)`

불투명도를 **특정 값**으로 부드럽게 변경한다. `0`이 되어도 `display: none`이 되지 않고 공간은 유지된다.

```javascript
$('#img').fadeTo(400, 0.3);    // 30% 투명도로
$('#img').fadeTo(400, 1);      // 완전 불투명으로 복원

// 마우스 호버 시 반투명 효과
$('.card').on('mouseenter', function() {
  $(this).fadeTo(200, 0.7);
}).on('mouseleave', function() {
  $(this).fadeTo(200, 1);
});
```

---

### 페이드 효과 비교

| 메서드 | 시작 opacity | 종료 opacity | display 변화 |
|--------|-------------|-------------|--------------|
| `fadeIn()` | 0 | 1 | none → 원래값 |
| `fadeOut()` | 1 | 0 | 원래값 → none |
| `fadeToggle()` | 현재 상태 | 반대 | 자동 |
| `fadeTo(d, n)` | 현재 값 | n | 변화 없음 |

---

## 7.4 슬라이드 효과

요소의 **높이(height)**를 변화시켜 위아래로 펼치거나 접는 효과를 낸다. 아코디언 메뉴, 드롭다운, 토글 콘텐츠 등에 자주 사용된다.

### `slideDown()` / `slideUp()`

```javascript
$('#menu').slideDown(300);   // height 0 → 원래 높이 (펼치기)
$('#menu').slideUp(300);     // height 원래 높이 → 0 (접기)
```

---

### `slideToggle()`

현재 상태에 따라 `slideDown`과 `slideUp`을 자동 전환한다.

```javascript
$('.accordion-header').on('click', function() {
  $(this).next('.accordion-body').slideToggle(250);
});
```

---

### 아코디언 메뉴 패턴

슬라이드 효과의 가장 전형적인 실무 패턴이다. 하나의 패널만 열리고 나머지는 닫히도록 구현한다.

```html
<ul id="accordion">
  <li>
    <div class="acc-header">섹션 1</div>
    <div class="acc-body">섹션 1 내용</div>
  </li>
  <li>
    <div class="acc-header">섹션 2</div>
    <div class="acc-body">섹션 2 내용</div>
  </li>
  <li>
    <div class="acc-header">섹션 3</div>
    <div class="acc-body">섹션 3 내용</div>
  </li>
</ul>
```

```javascript
// 초기: 모든 본문 숨기기
$('.acc-body').hide();

$('.acc-header').on('click', function() {
  const $body = $(this).next('.acc-body');

  // 클릭한 본문 외 나머지 모두 닫기
  $('.acc-body').not($body).slideUp(200);

  // 클릭한 본문 토글
  $body.slideToggle(200);
});
```

---

## 7.5 `animate()` — 커스텀 애니메이션

내장 효과 메서드로 표현할 수 없는 복잡한 애니메이션을 직접 정의할 때 사용한다. **숫자 값을 가지는 CSS 속성**이라면 대부분 애니메이션 가능하다.

### 기본 문법

```javascript
$(선택자).animate(properties, duration, easing, callback);
```

| 인수 | 타입 | 설명 |
|------|------|------|
| `properties` | Object | 변화시킬 CSS 속성과 목표값 |
| `duration` | Number / String | 지속 시간 (ms 또는 'fast'/'slow') |
| `easing` | String | 가속도 곡선 ('swing' 또는 'linear') |
| `callback` | Function | 완료 후 실행할 함수 |

---

### 기본 예시

```javascript
// 너비와 투명도를 동시에 변화
$('#box').animate({
  width: '300px',
  opacity: 0.5
}, 600);

// 위치 이동 (position이 static이 아니어야 함)
$('#ball').animate({
  left: '400px',
  top:  '200px'
}, 800, 'linear');

// 현재 값 기준 상대 이동
$('#box').animate({ left: '+=50px' }, 400);   // 오른쪽으로 50px
$('#box').animate({ left: '-=50px' }, 400);   // 왼쪽으로 50px
```

---

### `animate()`로 가능한 속성 / 불가능한 속성

| 가능 | 불가능 |
|------|--------|
| `width`, `height` | `color`, `background-color` |
| `opacity` | `border-radius` (jQuery 기본) |
| `left`, `top`, `right`, `bottom` | `display`, `visibility` |
| `margin`, `padding` | `transform` (jQuery 기본) |
| `font-size`, `line-height` | `box-shadow` |

> `color`, `background-color` 등 색상 속성을 애니메이션하려면 **jQuery UI** 또는 **jQuery Color** 플러그인이 필요하다.

---

### 이징(Easing) — 가속도 곡선

jQuery 기본은 두 가지 이징만 제공한다.

| 값 | 설명 |
|----|------|
| `'swing'` | 처음과 끝에서 느리고 중간에 빠름 (기본값) |
| `'linear'` | 일정한 속도로 변화 |

더 다양한 이징(easeInOutCubic 등)은 jQuery UI 또는 CSS `transition`과 조합하여 구현한다.

---

### 큐(Queue) — 순차 실행

`animate()`를 연속 호출하면 **큐(Queue)**에 쌓여 순서대로 실행된다.

```javascript
$('#box')
  .animate({ left: '300px' }, 500)   // 1. 오른쪽 이동
  .animate({ top:  '200px' }, 400)   // 2. 아래 이동
  .animate({ opacity: 0   }, 300)    // 3. 서서히 사라짐
  .hide();                            // 4. 숨기기
```

---

## 7.6 `stop()` — 애니메이션 제어

진행 중인 애니메이션을 중단한다. 빠르게 마우스를 움직이거나 연속 클릭할 때 애니메이션이 쌓이는 문제를 방지하는 데 필수다.

### 기본 문법

```javascript
$(선택자).stop(clearQueue, jumpToEnd);
```

| 인수 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `clearQueue` | Boolean | `false` | 큐에 남은 애니메이션도 모두 제거할지 여부 |
| `jumpToEnd` | Boolean | `false` | 현재 애니메이션을 즉시 완료 상태로 점프할지 여부 |

---

### `stop()` 동작 차이

```javascript
// 진행 중인 애니메이션만 중단, 나머지 큐는 유지
$('#box').stop();

// 진행 중인 애니메이션 + 큐 모두 제거
$('#box').stop(true);

// 현재 애니메이션을 완료 상태로 즉시 점프 후 큐의 다음 실행
$('#box').stop(false, true);

// 가장 많이 사용: 큐 비우고 현재 상태에서 즉시 중단
$('#box').stop(true, false);
```

---

### 호버 애니메이션 누적 문제 해결

`stop()`을 사용하지 않으면 마우스를 빠르게 움직일 때 애니메이션이 쌓여 반응이 늦어진다.

```javascript
// 문제 있는 코드: 빠른 마우스 이동 시 애니메이션 누적
$('.card').on('mouseenter', function() {
  $(this).animate({ marginTop: '-10px' }, 200);
}).on('mouseleave', function() {
  $(this).animate({ marginTop: '0px' }, 200);
});

// 해결: stop(true, true) 추가
$('.card').on('mouseenter', function() {
  $(this).stop(true, true).animate({ marginTop: '-10px' }, 200);
}).on('mouseleave', function() {
  $(this).stop(true, true).animate({ marginTop: '0px' }, 200);
});
```

---

### 페이드 효과와 `stop()` 조합 — 실무 패턴

```javascript
// 내비게이션 드롭다운
$('.nav-item').on('mouseenter', function() {
  $(this).find('.dropdown').stop(true, true).fadeIn(200);
}).on('mouseleave', function() {
  $(this).find('.dropdown').stop(true, true).fadeOut(200);
});
```

---

## 7.7 콜백과 체이닝으로 순차 애니메이션 구성

### 콜백 중첩 방식

애니메이션이 끝난 뒤 다음 애니메이션을 콜백으로 연결한다. 직관적이지만 중첩이 깊어지면 가독성이 떨어진다.

```javascript
$('#box')
  .fadeIn(400, function() {
    $(this).animate({ width: '300px' }, 500, function() {
      $(this).css('background', '#4f6ef7').fadeOut(300);
    });
  });
```

---

### 큐 체이닝 방식

`animate()`를 연속 호출하면 자동으로 큐에 쌓여 순차 실행된다. 콜백 중첩 없이 가독성이 높다.

```javascript
$('#box')
  .fadeIn(400)
  .delay(500)                             // 500ms 대기
  .animate({ width: '300px' }, 500)
  .animate({ height: '200px' }, 300)
  .fadeOut(400);
```

---

### `delay()` — 일시 정지

큐 내의 다음 애니메이션 실행 전 대기 시간을 설정한다.

```javascript
$('#notification')
  .fadeIn(300)
  .delay(2000)     // 2초 동안 표시
  .fadeOut(500);   // 서서히 사라짐
```

> `delay()`는 jQuery 애니메이션 큐에만 적용된다. `setTimeout()`과 다르게 애니메이션 흐름 안에서만 동작한다.

---

### `promise()` — 애니메이션 완료를 Promise로 처리

jQuery 1.8+부터 `.promise()`를 사용해 모든 애니메이션이 완료된 시점에 콜백을 실행할 수 있다.

```javascript
// 여러 요소의 애니메이션이 모두 끝난 뒤 실행
$('.item').fadeOut(400).promise().done(function() {
  console.log('모든 항목 숨김 완료');
  $('#empty-msg').show();
});
```

---

## 7.8 `jQuery.fx` — 전역 애니메이션 설정

### 애니메이션 비활성화

접근성 설정이나 테스트 환경에서 모든 애니메이션을 즉시 완료 상태로 처리할 수 있다.

```javascript
$.fx.off = true;    // 모든 애니메이션 즉시 완료 (duration 무시)
$.fx.off = false;   // 애니메이션 다시 활성화
```

---

### 기본 지속 시간 변경

`fast`와 `slow` 키워드의 기본 ms 값을 변경할 수 있다.

```javascript
$.fx.speeds.fast = 100;    // 기본 200ms → 100ms
$.fx.speeds.slow = 800;    // 기본 600ms → 800ms
$.fx.speeds._default = 300; // 기본 400ms → 300ms
```

---

## 7.9 CSS `transition`과의 비교

jQuery 애니메이션과 CSS `transition` 중 어떤 방식을 선택할지 기준을 이해해야 한다.

| 항목 | jQuery `animate()` | CSS `transition` |
|------|-------------------|-----------------|
| 구현 방법 | JavaScript로 제어 | CSS로 선언 |
| 시작/중단 제어 | `stop()` 등 메서드 사용 | JS로 클래스 토글 |
| 색상 애니메이션 | 플러그인 필요 | 기본 지원 |
| 복잡한 시퀀스 | 큐·콜백으로 간편 | 관리 복잡 |
| 성능 | JS 스레드 사용 | GPU 가속, 더 부드러움 |
| 브라우저 호환 | IE 구버전 지원 | 모던 브라우저 |

> **실무 가이드:** 단순한 토글·페이드·슬라이드는 jQuery 내장 메서드로, 복잡한 시각 효과(색상 변화, transform 등)는 CSS `transition` + 클래스 토글 방식으로 구현하는 것이 일반적이다.

```javascript
// CSS transition + 클래스 토글 패턴 (권장)
// CSS: .panel { transition: opacity 0.3s; }
//      .panel.hidden { opacity: 0; pointer-events: none; }
$('#btn').on('click', function() {
  $('#panel').toggleClass('hidden');
});
```

---

## 7.10 실습: 탭 패널 + 토스트 알림

이 장에서 학습한 효과 메서드를 종합 활용하는 예제다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>효과와 애니메이션 실습</title>
  <style>
    body { font-family: sans-serif; padding: 30px; background: #f5f5f5; }

    /* 탭 */
    .tab-buttons { display: flex; gap: 4px; margin-bottom: 0; }
    .tab-btn {
      padding: 8px 20px; border: 1px solid #ccc;
      border-bottom: none; background: #f0f0f0;
      cursor: pointer; border-radius: 6px 6px 0 0;
    }
    .tab-btn.active { background: #fff; border-color: #4f6ef7; color: #4f6ef7; }
    .tab-content {
      display: none; padding: 20px;
      border: 1px solid #4f6ef7; background: #fff;
      border-radius: 0 6px 6px 6px;
    }
    .tab-content.active { display: block; }

    /* 아코디언 */
    .acc { border: 1px solid #ddd; border-radius: 6px;
           margin: 20px 0; overflow: hidden; }
    .acc-header {
      padding: 12px 16px; background: #f9f9f9;
      cursor: pointer; font-weight: bold;
      border-bottom: 1px solid #eee;
    }
    .acc-header:hover { background: #eef; }
    .acc-body { padding: 16px; display: none; }

    /* 토스트 */
    #toast {
      display: none; position: fixed; bottom: 30px; right: 30px;
      background: #333; color: #fff;
      padding: 12px 20px; border-radius: 8px;
      font-size: 0.9rem; z-index: 999;
    }
    #btn-toast { margin-top: 10px; padding: 8px 16px;
                 background: #4f6ef7; color: #fff;
                 border: none; border-radius: 6px; cursor: pointer; }
  </style>
</head>
<body>

  <h2>① 탭 패널 (fadeIn)</h2>
  <div class="tab-buttons">
    <button class="tab-btn active" data-target="#tab1">탭 1</button>
    <button class="tab-btn" data-target="#tab2">탭 2</button>
    <button class="tab-btn" data-target="#tab3">탭 3</button>
  </div>
  <div id="tab1" class="tab-content active">탭 1 내용입니다.</div>
  <div id="tab2" class="tab-content">탭 2 내용입니다.</div>
  <div id="tab3" class="tab-content">탭 3 내용입니다.</div>

  <h2 style="margin-top:30px">② 아코디언 (slideToggle)</h2>
  <div class="acc">
    <div class="acc-header">▶ 섹션 1 — jQuery란?</div>
    <div class="acc-body">jQuery는 빠르고 간결한 JavaScript 라이브러리입니다.</div>
    <div class="acc-header">▶ 섹션 2 — 선택자</div>
    <div class="acc-body">CSS 선택자 문법을 그대로 사용하여 요소를 선택합니다.</div>
    <div class="acc-header">▶ 섹션 3 — 이벤트</div>
    <div class="acc-body">on() 메서드 하나로 모든 이벤트를 일관되게 처리합니다.</div>
  </div>

  <h2>③ 토스트 알림 (fadeIn → delay → fadeOut)</h2>
  <button id="btn-toast">알림 보내기</button>
  <div id="toast">✅ 저장되었습니다!</div>

  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script>
    $(function() {

      // ① 탭 패널: fadeIn 효과
      $('.tab-btn').on('click', function() {
        const target = $(this).data('target');

        // 버튼 active 전환
        $('.tab-btn').removeClass('active');
        $(this).addClass('active');

        // 현재 탭 fadeOut → 대상 탭 fadeIn
        $('.tab-content.active')
          .removeClass('active')
          .fadeOut(150, function() {
            $(target).fadeIn(200).addClass('active');
          });
      });

      // ② 아코디언: slideToggle
      $('.acc-header').on('click', function() {
        const $body = $(this).next('.acc-body');
        $('.acc-body').not($body).slideUp(200);
        $body.slideToggle(250);
      });

      // ③ 토스트 알림: fadeIn → delay → fadeOut
      $('#btn-toast').on('click', function() {
        $('#toast')
          .stop(true, true)
          .fadeIn(300)
          .delay(2000)
          .fadeOut(500);
      });

    });
  </script>
</body>
</html>
```

**코드 포인트 분석:**

| 기능 | 사용 메서드 | 포인트 |
|------|-------------|--------|
| 탭 전환 | `fadeOut()` → 콜백 → `fadeIn()` | 현재 탭이 완전히 사라진 뒤 다음 탭이 등장 |
| 아코디언 | `slideUp()`, `slideToggle()` | `not($body)`로 다른 패널 먼저 닫기 |
| 토스트 알림 | `stop()`, `fadeIn()`, `delay()`, `fadeOut()` | 연속 클릭 시 누적 방지 |

---

## 7.11 정리

### 표시/숨김 메서드

| 메서드 | 변화 요소 | 특징 |
|--------|-----------|------|
| `show()` / `hide()` | 크기 + opacity | 크기와 투명도 동시 변화 |
| `toggle()` | 크기 + opacity | 현재 상태 자동 감지 |
| `fadeIn()` / `fadeOut()` | opacity만 | 레이아웃 유지 |
| `fadeToggle()` | opacity만 | 현재 상태 자동 감지 |
| `fadeTo()` | opacity만 | display 변화 없음 |
| `slideDown()` / `slideUp()` | height만 | 수직 펼침/접기 |
| `slideToggle()` | height만 | 현재 상태 자동 감지 |

### 제어 메서드

| 메서드 | 설명 |
|--------|------|
| `animate(props, duration, easing, fn)` | 커스텀 CSS 애니메이션 |
| `stop(clearQueue, jumpToEnd)` | 진행 중인 애니메이션 중단 |
| `delay(duration)` | 큐 내 대기 시간 설정 |
| `promise()` | 모든 애니메이션 완료 후 콜백 |

### 공통 인수

| 인수 | 타입 | 설명 |
|------|------|------|
| `duration` | Number / String | ms 숫자 또는 `'fast'`(200) / `'normal'`(400) / `'slow'`(600) |
| `easing` | String | `'swing'`(기본) / `'linear'` |
| `callback` | Function | 애니메이션 완료 후 실행, `this`는 해당 DOM 요소 |

---

## 연습 문제

1. `#box`에 버튼 클릭 시 `slideToggle(400)`이 동작하도록 구현하되, 연속 클릭 시 애니메이션이 누적되지 않도록 `stop()`을 적용하라.

2. 3개의 이미지가 있는 슬라이드쇼를 구현하라. 이미지가 1초씩 표시되고, `fadeOut` → `fadeIn` 효과로 전환되며 마지막 이미지 이후 첫 번째 이미지로 돌아간다.

3. `#ball` 요소(`position: absolute`)를 클릭할 때마다 `animate()`를 사용해 다음 좌표로 순서대로 이동시켜라: (0, 0) → (200, 0) → (200, 200) → (0, 200) → (0, 0).

4. 버튼 클릭 시 `#panel`이 `fadeIn(300)` → 2초 대기 → `fadeOut(500)` 순서로 동작하는 토스트 알림을 구현하라. 버튼을 연속으로 눌러도 타이머가 초기화되어 처음부터 다시 시작해야 한다.

5. 아래 CSS와 같은 구조에서 `animate()`를 사용하지 않고 CSS `transition`과 jQuery `toggleClass()`만으로 동일한 효과를 구현하고, 두 방식의 코드를 비교하라.
   ```css
   #panel { width: 100px; transition: width 0.4s; }
   #panel.expanded { width: 300px; }
   ```

---

> **다음 장 예고**  
> 8장에서는 jQuery의 **Ajax** 기능을 학습한다. `$.ajax()`, `$.get()`, `$.post()` 등으로 서버와 비동기 통신하여 데이터를 가져오고 페이지를 동적으로 갱신하는 방법을 익힌다.
