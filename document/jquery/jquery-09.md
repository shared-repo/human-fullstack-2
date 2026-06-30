# 9장. 폼 처리와 유효성 검사

> 학습 목표
> - `serialize()`, `serializeArray()`로 폼 데이터를 효율적으로 수집할 수 있다.
> - 필드별 실시간 유효성 검사 패턴을 구현할 수 있다.
> - 유효성 검사를 통과한 폼 데이터를 Ajax로 제출하는 전체 흐름을 구성할 수 있다.

---

## 9.1 폼 처리 개요

웹 애플리케이션에서 폼(Form)은 사용자 입력을 받는 핵심 UI다. jQuery를 활용한 폼 처리는 크게 세 단계로 이루어진다.

```
① 데이터 수집        ② 유효성 검사        ③ 서버 전송
  val(), serialize()  → 규칙 검증, 피드백  → Ajax 제출
```

HTML5의 기본 폼 제출(`action`, `method`)은 페이지를 새로 고침하므로, 실무에서는 대부분 `e.preventDefault()`로 기본 동작을 차단하고 Ajax로 처리한다.

---

## 9.2 폼 데이터 수집

### `val()`로 개별 수집

필드가 적을 때는 각각 `val()`로 직접 읽는다.

```javascript
$('#login-form').on('submit', function(e) {
  e.preventDefault();

  const username = $('#username').val().trim();
  const password = $('#password').val();

  console.log(username, password);
});
```

---

### `serialize()` — 쿼리스트링으로 수집

폼 안의 모든 입력 필드를 `name=value&name=value` 형식의 **쿼리스트링**으로 변환한다. `$.ajax()`의 `data` 옵션이나 `$.post()`에 그대로 전달할 수 있다.

```html
<form id="contact-form">
  <input type="text"  name="username" value="홍길동">
  <input type="email" name="email"    value="hong@example.com">
  <input type="text"  name="subject"  value="문의합니다">
</form>
```

```javascript
const serialized = $('#contact-form').serialize();
console.log(serialized);
// "username=%ED%99%8D%EA%B8%B8%EB%8F%99&email=hong%40example.com&subject=%EB%AC%B8%EC%9D%98%ED%95%A9%EB%8B%88%EB%8B%A4"

// Ajax POST로 바로 전송
$.post('/api/contact', $('#contact-form').serialize())
  .done(function(res) { console.log('전송 완료'); });
```

> `serialize()`는 `name` 속성이 있는 요소만 수집한다. `disabled` 상태인 필드는 제외된다. 값은 자동으로 URL 인코딩된다.

---

### `serializeArray()` — 배열 객체로 수집

폼 데이터를 `[{ name, value }, ...]` 형태의 **배열**로 반환한다. 데이터를 가공하거나 객체로 변환해야 할 때 유용하다.

```javascript
const arr = $('#contact-form').serializeArray();
console.log(arr);
// [
//   { name: "username", value: "홍길동" },
//   { name: "email",    value: "hong@example.com" },
//   { name: "subject",  value: "문의합니다" }
// ]
```

---

### `serializeArray()`를 객체로 변환

```javascript
function formToObject($form) {
  const result = {};
  $form.serializeArray().forEach(function(item) {
    result[item.name] = item.value;
  });
  return result;
}

const data = formToObject($('#contact-form'));
console.log(data);
// { username: "홍길동", email: "hong@example.com", subject: "문의합니다" }

// JSON으로 전송
$.ajax({
  url: '/api/contact',
  method: 'POST',
  contentType: 'application/json',
  data: JSON.stringify(data)
});
```

---

### 체크박스·라디오·셀렉트 수집

```html
<form id="survey-form">
  <input type="text"     name="name"    value="홍길동">
  <input type="radio"    name="gender"  value="male"   checked>
  <input type="radio"    name="gender"  value="female">
  <input type="checkbox" name="agree"   value="Y"      checked>
  <select name="city">
    <option value="seoul" selected>서울</option>
    <option value="busan">부산</option>
  </select>
</form>
```

```javascript
console.log($('#survey-form').serialize());
// "name=%ED%99%8D%EA%B8%B8%EB%8F%99&gender=male&agree=Y&city=seoul"

// 체크 해제된 체크박스는 serialize()에 포함되지 않음
// → 서버에서 값 없음 = 미체크로 처리
```

---

### 수집 방법 비교

| 방법 | 반환 형식 | 장점 | 단점 |
|------|-----------|------|------|
| `val()` 개별 수집 | 문자열 | 특정 필드만 선택적 수집 | 필드 수가 많으면 코드 반복 |
| `serialize()` | 쿼리스트링 | Ajax `data`에 바로 사용 가능 | 데이터 가공 불편 |
| `serializeArray()` | 배열 객체 | 가공·변환 유연 | 객체 변환 코드 필요 |

---

## 9.3 유효성 검사 기초

### 유효성 검사가 필요한 이유

서버에만 의존하는 유효성 검사는 응답이 오기 전까지 사용자가 피드백을 받지 못한다. **클라이언트 유효성 검사**는 즉각적인 피드백으로 사용자 경험을 높이고 불필요한 서버 요청을 줄인다.

> 클라이언트 유효성 검사는 편의성을 위한 것이지 **보안 수단이 아니다**. 서버에서도 반드시 검증해야 한다.

---

### 유효성 검사 결과 표시 패턴

필드 아래에 메시지 요소를 두고 `addClass` / `removeClass`로 상태를 전환하는 방식이 일반적이다.

```html
<div class="field-group">
  <label for="email">이메일</label>
  <input type="text" id="email" name="email">
  <span class="field-msg"></span>  <!-- 오류/성공 메시지 표시 영역 -->
</div>
```

```css
.field-group input.valid   { border-color: #27ae60; }
.field-group input.invalid { border-color: #e74c3c; }
.field-msg.error   { color: #e74c3c; font-size: 0.82rem; }
.field-msg.success { color: #27ae60; font-size: 0.82rem; }
```

```javascript
function setValid($input, message) {
  $input
    .removeClass('invalid').addClass('valid')
    .siblings('.field-msg')
    .removeClass('error').addClass('success')
    .text(message || '');
}

function setInvalid($input, message) {
  $input
    .removeClass('valid').addClass('invalid')
    .siblings('.field-msg')
    .removeClass('success').addClass('error')
    .text(message);
}

function clearState($input) {
  $input
    .removeClass('valid invalid')
    .siblings('.field-msg')
    .removeClass('error success')
    .text('');
}
```

---

## 9.4 실시간 유효성 검사 패턴

### 필드별 검사 규칙 함수 분리

각 필드의 검사 로직을 함수로 분리하면 재사용성과 유지보수성이 높아진다.

```javascript
/* ── 검사 규칙 함수들 ── */

function validateUsername($input) {
  const val = $input.val().trim();
  if (!val) {
    setInvalid($input, '이름을 입력해 주세요.');
    return false;
  }
  if (val.length < 2) {
    setInvalid($input, '이름은 2자 이상이어야 합니다.');
    return false;
  }
  setValid($input, '');
  return true;
}

function validateEmail($input) {
  const val = $input.val().trim();
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!val) {
    setInvalid($input, '이메일을 입력해 주세요.');
    return false;
  }
  if (!emailRegex.test(val)) {
    setInvalid($input, '올바른 이메일 형식이 아닙니다.');
    return false;
  }
  setValid($input, '');
  return true;
}

function validatePassword($input) {
  const val = $input.val();
  if (!val) {
    setInvalid($input, '비밀번호를 입력해 주세요.');
    return false;
  }
  if (val.length < 8) {
    setInvalid($input, '비밀번호는 8자 이상이어야 합니다.');
    return false;
  }
  if (!/[A-Za-z]/.test(val) || !/[0-9]/.test(val)) {
    setInvalid($input, '영문자와 숫자를 포함해야 합니다.');
    return false;
  }
  setValid($input, '사용 가능한 비밀번호입니다.');
  return true;
}

function validatePasswordConfirm($input) {
  const val     = $input.val();
  const original = $('#password').val();
  if (!val) {
    setInvalid($input, '비밀번호 확인을 입력해 주세요.');
    return false;
  }
  if (val !== original) {
    setInvalid($input, '비밀번호가 일치하지 않습니다.');
    return false;
  }
  setValid($input, '비밀번호가 일치합니다.');
  return true;
}
```

---

### 이벤트 기반 실시간 검사

| 이벤트 | 검사 시점 | 적합한 상황 |
|--------|-----------|-------------|
| `input` | 글자 입력할 때마다 | 즉각 피드백 (길이, 형식) |
| `blur` | 포커스를 잃었을 때 | 입력 완료 후 검사 (덜 침습적) |
| `change` | 값이 변경되고 포커스를 잃을 때 | select, checkbox, radio |
| `keyup` | 키를 뗄 때마다 | 특정 키(Enter 등) 감지와 조합 |

```javascript
$(function() {

  // blur: 포커스를 잃을 때 검사 (덜 침습적, 권장)
  $('#username').on('blur', function() {
    validateUsername($(this));
  });

  $('#email').on('blur', function() {
    validateEmail($(this));
  });

  // input: 실시간 검사 (비밀번호 강도 등 즉각 피드백이 필요한 경우)
  $('#password').on('input', function() {
    validatePassword($(this));
    // 비밀번호가 바뀌면 확인 필드도 재검사
    if ($('#password-confirm').val()) {
      validatePasswordConfirm($('#password-confirm'));
    }
  });

  $('#password-confirm').on('input', function() {
    validatePasswordConfirm($(this));
  });

  // 포커스 진입 시 상태 초기화 (선택 사항)
  $('input').on('focus', function() {
    clearState($(this));
  });

});
```

---

### 비밀번호 강도 표시

```javascript
function getPasswordStrength(password) {
  let score = 0;
  if (password.length >= 8)           score++;
  if (password.length >= 12)          score++;
  if (/[A-Z]/.test(password))        score++;
  if (/[0-9]/.test(password))        score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;  // 특수문자
  return score;
}

$('#password').on('input', function() {
  const score = getPasswordStrength($(this).val());
  const labels = ['', '매우 약함', '약함', '보통', '강함', '매우 강함'];
  const colors = ['', '#e74c3c', '#e67e22', '#f1c40f', '#2ecc71', '#27ae60'];

  $('#strength-bar').css({
    width: (score * 20) + '%',
    background: colors[score]
  });
  $('#strength-label').text(labels[score]).css('color', colors[score]);
});
```

---

### 전체 폼 검사 함수

폼 제출 시 모든 필드를 한 번에 검사하고 결과를 종합한다.

```javascript
function validateForm() {
  const results = [
    validateUsername($('#username')),
    validateEmail($('#email')),
    validatePassword($('#password')),
    validatePasswordConfirm($('#password-confirm'))
  ];

  // 하나라도 false면 false 반환
  return results.every(function(result) { return result === true; });
}
```

---

### 체크박스·라디오 유효성 검사

```javascript
function validateAgree() {
  const checked = $('#agree').prop('checked');
  if (!checked) {
    $('#agree-msg').addClass('error').text('이용약관에 동의해야 합니다.');
    return false;
  }
  $('#agree-msg').removeClass('error').text('');
  return true;
}

function validateGender() {
  const selected = $('input[name="gender"]:checked').val();
  if (!selected) {
    $('#gender-msg').addClass('error').text('성별을 선택해 주세요.');
    return false;
  }
  $('#gender-msg').removeClass('error').text('');
  return true;
}
```

---

## 9.5 Ajax 폼 제출

유효성 검사를 통과한 폼 데이터를 Ajax로 서버에 전송하는 전체 흐름을 구성한다.

### 기본 구조

```javascript
$('#register-form').on('submit', function(e) {
  e.preventDefault();   // 기본 페이지 이동 차단

  // 1. 유효성 검사
  if (!validateForm()) {
    // 첫 번째 오류 필드로 스크롤
    const $firstError = $('.invalid').first();
    if ($firstError.length) {
      $('html, body').animate({
        scrollTop: $firstError.offset().top - 80
      }, 300);
    }
    return;
  }

  // 2. 데이터 수집
  const formData = formToObject($(this));

  // 3. 전송 상태 설정
  const $btn = $('#btn-submit');
  $btn.prop('disabled', true).text('처리 중...');

  // 4. Ajax 전송
  $.ajax({
    url: '/api/register',
    method: 'POST',
    contentType: 'application/json',
    data: JSON.stringify(formData)
  })
    .done(function(response) {
      showSuccessMessage('회원가입이 완료되었습니다!');
      $('#register-form')[0].reset();   // 폼 초기화
      $('input').each(function() { clearState($(this)); });
    })
    .fail(function(xhr) {
      if (xhr.status === 409) {
        setInvalid($('#email'), '이미 사용 중인 이메일입니다.');
      } else {
        showErrorMessage('오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
      }
    })
    .always(function() {
      $btn.prop('disabled', false).text('가입하기');
    });
});
```

---

### 서버 응답 오류 필드 처리

서버가 필드별 오류 정보를 반환하는 경우 해당 필드에 직접 오류를 표시한다.

```javascript
// 서버 응답 예시
// { "errors": { "email": "이미 사용 중인 이메일입니다.", "username": "사용 불가능한 이름입니다." } }

.fail(function(xhr) {
  if (xhr.status === 422) {
    const errors = JSON.parse(xhr.responseText).errors;
    Object.keys(errors).forEach(function(fieldName) {
      const $field = $('[name="' + fieldName + '"]');
      setInvalid($field, errors[fieldName]);
    });
  } else {
    showErrorMessage('서버 오류가 발생했습니다.');
  }
})
```

---

### `FormData`로 파일 업로드

파일을 포함한 폼을 Ajax로 전송할 때는 `serialize()` 대신 `FormData`를 사용한다.

```html
<form id="upload-form">
  <input type="text" name="title">
  <input type="file" name="image" id="image-file">
  <button type="submit">업로드</button>
</form>
```

```javascript
$('#upload-form').on('submit', function(e) {
  e.preventDefault();

  const formData = new FormData(this);   // 파일 포함 폼 데이터

  $.ajax({
    url: '/api/upload',
    method: 'POST',
    data: formData,
    processData: false,   // jQuery의 데이터 변환 비활성화 (필수)
    contentType: false,   // Content-Type 자동 설정 (boundary 포함, 필수)
  })
    .done(function(res) {
      console.log('업로드 완료:', res.fileUrl);
    })
    .fail(function() {
      console.error('업로드 실패');
    });
});
```

> `processData: false`와 `contentType: false`는 파일 업로드 시 반드시 설정해야 한다. 이 두 옵션이 없으면 jQuery가 `FormData`를 잘못 직렬화하여 파일이 전송되지 않는다.

---

## 9.6 실습: 회원가입 폼

이 장의 학습 내용을 통합한 완성 예제다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>회원가입 폼 실습</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', sans-serif; background: #f0f2f5;
           display: flex; justify-content: center; padding: 40px 16px; }

    #register-form {
      background: #fff; border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.1);
      padding: 36px; width: 100%; max-width: 460px;
    }
    h2 { text-align: center; margin-bottom: 28px; color: #333; }

    .field-group { margin-bottom: 20px; }
    label { display: block; font-size: 0.88rem;
            font-weight: 600; color: #555; margin-bottom: 6px; }

    .field-group input {
      width: 100%; padding: 10px 14px;
      border: 2px solid #ddd; border-radius: 8px;
      font-size: 0.95rem; outline: none;
      transition: border-color 0.2s;
    }
    .field-group input:focus  { border-color: #4f6ef7; }
    .field-group input.valid   { border-color: #27ae60; }
    .field-group input.invalid { border-color: #e74c3c; }

    .field-msg { display: block; font-size: 0.8rem; margin-top: 4px; min-height: 18px; }
    .field-msg.error   { color: #e74c3c; }
    .field-msg.success { color: #27ae60; }

    /* 비밀번호 강도 */
    #strength-wrap { margin-top: 6px; }
    #strength-bar-bg {
      height: 4px; background: #eee;
      border-radius: 2px; overflow: hidden;
    }
    #strength-bar { height: 100%; width: 0; transition: width 0.3s, background 0.3s; }
    #strength-label { font-size: 0.78rem; color: #999; margin-top: 3px; }

    /* 체크박스 */
    .checkbox-group { display: flex; align-items: center; gap: 8px;
                      margin-bottom: 6px; }
    .checkbox-group input { width: auto; }

    #btn-submit {
      width: 100%; padding: 12px;
      background: #4f6ef7; color: #fff;
      border: none; border-radius: 8px;
      font-size: 1rem; cursor: pointer;
      transition: background 0.2s;
      margin-top: 8px;
    }
    #btn-submit:hover:not(:disabled) { background: #3a5be0; }
    #btn-submit:disabled { opacity: 0.5; cursor: default; }

    /* 결과 메시지 */
    #result-msg {
      display: none; text-align: center;
      padding: 12px; border-radius: 8px;
      margin-top: 16px; font-size: 0.9rem;
    }
    #result-msg.success { background: #e8f5e9; color: #27ae60; }
    #result-msg.error   { background: #fff0f0; color: #e74c3c; }
  </style>
</head>
<body>

  <form id="register-form" novalidate>
    <h2>회원가입</h2>

    <div class="field-group">
      <label for="username">이름</label>
      <input type="text" id="username" name="username" placeholder="홍길동">
      <span class="field-msg"></span>
    </div>

    <div class="field-group">
      <label for="email">이메일</label>
      <input type="text" id="email" name="email" placeholder="example@email.com">
      <span class="field-msg"></span>
    </div>

    <div class="field-group">
      <label for="password">비밀번호</label>
      <input type="password" id="password" name="password" placeholder="8자 이상, 영문+숫자">
      <div id="strength-wrap">
        <div id="strength-bar-bg"><div id="strength-bar"></div></div>
        <span id="strength-label"></span>
      </div>
      <span class="field-msg"></span>
    </div>

    <div class="field-group">
      <label for="password-confirm">비밀번호 확인</label>
      <input type="password" id="password-confirm" name="passwordConfirm"
             placeholder="비밀번호를 다시 입력하세요">
      <span class="field-msg"></span>
    </div>

    <div class="field-group">
      <div class="checkbox-group">
        <input type="checkbox" id="agree" name="agree" value="Y">
        <label for="agree" style="margin:0; font-weight:normal;">
          이용약관 및 개인정보처리방침에 동의합니다.
        </label>
      </div>
      <span id="agree-msg" class="field-msg"></span>
    </div>

    <button type="submit" id="btn-submit">가입하기</button>
    <div id="result-msg"></div>
  </form>

  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
  <script>
  $(function () {

    /* ════════════════════════════════════
       상태 표시 헬퍼
    ════════════════════════════════════ */
    function setValid($input, msg) {
      $input.removeClass('invalid').addClass('valid')
        .siblings('.field-msg')
        .removeClass('error').addClass('success').text(msg || '');
    }

    function setInvalid($input, msg) {
      $input.removeClass('valid').addClass('invalid')
        .siblings('.field-msg')
        .removeClass('success').addClass('error').text(msg);
    }

    function clearState($input) {
      $input.removeClass('valid invalid')
        .siblings('.field-msg')
        .removeClass('error success').text('');
    }

    function showResult(msg, type) {
      $('#result-msg')
        .removeClass('success error').addClass(type)
        .text(msg).fadeIn(200);
    }

    /* ════════════════════════════════════
       유효성 검사 규칙
    ════════════════════════════════════ */
    function validateUsername($el) {
      const v = $el.val().trim();
      if (!v)          return setInvalid($el, '이름을 입력해 주세요.'), false;
      if (v.length < 2) return setInvalid($el, '이름은 2자 이상이어야 합니다.'), false;
      return setValid($el, ''), true;
    }

    function validateEmail($el) {
      const v = $el.val().trim();
      const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!v)       return setInvalid($el, '이메일을 입력해 주세요.'), false;
      if (!re.test(v)) return setInvalid($el, '올바른 이메일 형식이 아닙니다.'), false;
      return setValid($el, ''), true;
    }

    function validatePassword($el) {
      const v = $el.val();
      if (!v)          return setInvalid($el, '비밀번호를 입력해 주세요.'), false;
      if (v.length < 8) return setInvalid($el, '8자 이상 입력해 주세요.'), false;
      if (!/[A-Za-z]/.test(v) || !/[0-9]/.test(v))
        return setInvalid($el, '영문자와 숫자를 모두 포함해야 합니다.'), false;
      return setValid($el, '사용 가능한 비밀번호입니다.'), true;
    }

    function validateConfirm($el) {
      const v = $el.val();
      if (!v) return setInvalid($el, '비밀번호 확인을 입력해 주세요.'), false;
      if (v !== $('#password').val())
        return setInvalid($el, '비밀번호가 일치하지 않습니다.'), false;
      return setValid($el, '비밀번호가 일치합니다.'), true;
    }

    function validateAgree() {
      if (!$('#agree').prop('checked')) {
        $('#agree-msg').addClass('error').text('이용약관에 동의해야 합니다.');
        return false;
      }
      $('#agree-msg').removeClass('error').text('');
      return true;
    }

    function validateAll() {
      return [
        validateUsername($('#username')),
        validateEmail($('#email')),
        validatePassword($('#password')),
        validateConfirm($('#password-confirm')),
        validateAgree()
      ].every(Boolean);
    }

    /* ════════════════════════════════════
       비밀번호 강도 표시
    ════════════════════════════════════ */
    function updateStrength(pw) {
      let score = 0;
      if (pw.length >= 8)            score++;
      if (pw.length >= 12)           score++;
      if (/[A-Z]/.test(pw))         score++;
      if (/[0-9]/.test(pw))         score++;
      if (/[^A-Za-z0-9]/.test(pw))  score++;

      const colors = ['', '#e74c3c', '#e67e22', '#f1c40f', '#2ecc71', '#27ae60'];
      const labels = ['', '매우 약함', '약함', '보통', '강함', '매우 강함'];

      $('#strength-bar').css({ width: (score * 20) + '%', background: colors[score] });
      $('#strength-label').text(pw ? labels[score] : '').css('color', colors[score]);
    }

    /* ════════════════════════════════════
       실시간 이벤트 바인딩
    ════════════════════════════════════ */
    $('#username').on('blur', function () { validateUsername($(this)); });
    $('#email').on('blur',    function () { validateEmail($(this)); });

    $('#password').on('input', function () {
      updateStrength($(this).val());
      if ($(this).val()) validatePassword($(this));
      if ($('#password-confirm').val()) validateConfirm($('#password-confirm'));
    });

    $('#password-confirm').on('input', function () {
      if ($(this).val()) validateConfirm($(this));
    });

    $('#agree').on('change', validateAgree);

    $('input').on('focus', function () { clearState($(this)); });

    /* ════════════════════════════════════
       폼 제출 — Ajax 전송
    ════════════════════════════════════ */
    $('#register-form').on('submit', function (e) {
      e.preventDefault();
      $('#result-msg').hide();

      if (!validateAll()) {
        // 첫 번째 오류 필드로 스크롤
        const $firstError = $(this).find('.invalid').first();
        if ($firstError.length) {
          $('html, body').animate({ scrollTop: $firstError.offset().top - 100 }, 300);
        }
        return;
      }

      // 데이터 수집
      const payload = {};
      $(this).serializeArray().forEach(function (item) {
        payload[item.name] = item.value;
      });

      const $btn = $('#btn-submit');
      $btn.prop('disabled', true).text('처리 중...');

      // ── Ajax 전송 ──
      // 실습 환경에서는 jsonplaceholder로 대체
      $.ajax({
        url: 'https://jsonplaceholder.typicode.com/users',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        dataType: 'json'
      })
        .done(function (res) {
          showResult('✅ 회원가입이 완료되었습니다! (ID: ' + res.id + ')', 'success');
          $('#register-form')[0].reset();
          $('input').each(function () { clearState($(this)); });
          updateStrength('');
        })
        .fail(function (xhr) {
          if (xhr.status === 409) {
            setInvalid($('#email'), '이미 사용 중인 이메일입니다.');
          } else {
            showResult('❌ 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
          }
        })
        .always(function () {
          $btn.prop('disabled', false).text('가입하기');
        });
    });

  });
  </script>
</body>
</html>
```

**코드 포인트 분석:**

| 항목 | 구현 내용 |
|------|-----------|
| 데이터 수집 | `serializeArray()` → 객체 변환 후 JSON 전송 |
| 상태 표시 | `setValid()` / `setInvalid()` / `clearState()` 헬퍼 함수 분리 |
| 실시간 검사 | `blur`(이름·이메일) + `input`(비밀번호·확인) 혼용 |
| 비밀번호 강도 | 5가지 조건 점수화 → 색상 바 표시 |
| 전체 검사 | `validateAll()`에서 모든 규칙 결과를 `every(Boolean)`으로 집계 |
| 오류 스크롤 | 첫 번째 `.invalid` 필드로 부드럽게 이동 |
| Ajax 전송 | `done` / `fail` / `always` 패턴, 서버 응답 오류 필드 반영 |
| 폼 초기화 | `form.reset()` + 각 필드 상태 클래스 초기화 |

---

## 9.7 정리

### 데이터 수집 메서드

| 메서드 | 반환 형식 | 주요 용도 |
|--------|-----------|-----------|
| `val()` | 문자열 | 단일 필드 값 읽기 |
| `serialize()` | 쿼리스트링 | `$.post()`에 직접 전달 |
| `serializeArray()` | `[{name, value}]` 배열 | 객체 변환, 데이터 가공 |
| `FormData` (네이티브) | FormData 객체 | 파일 업로드 포함 전송 |

### 유효성 검사 이벤트

| 이벤트 | 검사 시점 | 권장 용도 |
|--------|-----------|-----------|
| `blur` | 포커스 이탈 | 일반 텍스트 필드 |
| `input` | 즉시 | 비밀번호 강도, 실시간 카운트 |
| `change` | 값 변경 + 포커스 이탈 | select, checkbox, radio |
| `submit` | 폼 제출 시 | 최종 전체 검사 |

### Ajax 폼 제출 흐름

```
submit 이벤트
  → e.preventDefault()
  → validateAll()  ─── false → 오류 필드 표시 + 스크롤, return
  → 데이터 수집 (serializeArray → 객체)
  → 버튼 비활성화
  → $.ajax() 전송
      .done()   → 성공 메시지, 폼 초기화
      .fail()   → 에러 분기 처리
      .always() → 버튼 복구
```

---

## 연습 문제

1. `#search-form`에 텍스트 입력 필드(`name="keyword"`)와 셀렉트박스(`name="category"`)가 있다. `serialize()`로 수집한 데이터를 `$.get()`으로 `/api/search`에 전송하고 결과를 `#result`에 렌더링하는 코드를 작성하라.

2. 이메일 입력 필드에 `blur` 이벤트를 사용하여 아래 조건을 모두 검사하는 `validateEmail()` 함수를 작성하라.
   - 빈 값이면 "이메일을 입력해 주세요."
   - `@` 포함 여부 검사 실패 시 "올바른 이메일 형식이 아닙니다."
   - 통과 시 입력 필드에 `.valid` 클래스 추가

3. 비밀번호 입력 시 `input` 이벤트로 아래 조건을 실시간으로 검사하여 각 항목을 체크리스트 UI(✓/✗)로 표시하는 코드를 작성하라.
   - 8자 이상
   - 영문자 포함
   - 숫자 포함
   - 특수문자 포함

4. `#register-form`의 `submit` 이벤트에서 `serializeArray()`로 데이터를 수집하여 객체로 변환한 뒤, 필수 필드(`name`, `email`, `password`) 중 빈 값이 있으면 해당 필드에 오류 메시지를 표시하고, 모두 채워진 경우에만 `$.ajax()`로 전송하는 코드를 작성하라.

5. 파일 첨부(`<input type="file" name="photo">`)와 텍스트 입력(`<input type="text" name="title">`)이 포함된 폼을 `FormData`와 `$.ajax()`를 사용하여 `/api/upload`로 전송하는 코드를 작성하라. `processData`와 `contentType` 옵션의 역할도 주석으로 설명하라.

---

> **다음 장 예고**  
> 10장에서는 **성능 최적화와 모범 사례**를 학습한다. 선택자 캐싱, 불필요한 DOM 접근 최소화, jQuery와 ES6+의 혼용 패턴 등 실무에서 바로 적용할 수 있는 최적화 기법을 익힌다.
