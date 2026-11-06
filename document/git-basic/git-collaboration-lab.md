# Git 협업 실습 가이드: 브랜치 병합과 Pull Request

## 실습 목표

- 브랜치를 만들어 작업하고, 작업 결과를 main 브랜치에 반영하는 전체 흐름을 이해합니다.
- 병합 과정에서 발생하는 **충돌(conflict)** 의 원인을 이해하고 직접 해결합니다.
- 현업 표준 협업 방식인 **Pull Request 기반 워크플로(GitHub Flow)** 를 경험합니다.

## 실습 구성

| 구분 | 시나리오 1: 로컬 병합 | 시나리오 2: Pull Request |
|---|---|---|
| 핵심 내용 | 로컬에서 브랜치 병합, 충돌 해결 후 main에 push | 원격 브랜치 push → PR → 리뷰 → 병합 |
| 진행 형태 | 개인 실습 | 2인 1조 실습 (개인 진행 방법도 안내) |
| 현업과의 관계 | 병합 원리 학습, 개인·소규모 프로젝트 방식 | 대부분의 팀에서 사용하는 표준 방식 |

> 시나리오 1에서 병합과 충돌 해결 원리를 먼저 익힌 뒤, 시나리오 2로 넘어가는 순서로 진행합니다.

---

## 0. 사전 준비

### 0-1. Git 설치와 버전 확인

이 실습에서는 `git switch` 명령어를 사용하므로 **Git 2.23 이상**이 필요합니다.

```bash
git --version
```

> 2.23 미만이라면 Git을 업데이트하거나, `git switch` 대신 `git checkout`을 사용합니다.
> - `git switch main` → `git checkout main`
> - `git switch -c feature/profile` → `git checkout -b feature/profile`

### 0-2. 사용자 정보와 기본 설정

커밋에 기록될 이름과 이메일을 설정합니다. 이메일은 GitHub 계정 이메일과 같게 설정하는 것이 좋습니다.

```bash
git config --global user.name "홍길동"
git config --global user.email "you@example.com"
```

실습을 원활하게 진행하기 위해 다음 설정도 함께 적용합니다.

```bash
# 새 저장소의 기본 브랜치 이름을 main으로 설정합니다.
git config --global init.defaultBranch main

# git pull 시 병합(merge) 방식으로 통합합니다.
# 이 설정이 없으면 "Need to specify how to reconcile divergent branches" 오류가 발생할 수 있습니다.
git config --global pull.rebase false
```

(선택) 병합 커밋 메시지 편집기를 VS Code로 지정하면 vim 사용에 익숙하지 않아도 진행할 수 있습니다.

```bash
git config --global core.editor "code --wait"
```

설정 내용을 확인합니다.

```bash
git config --global --list
```

### 0-3. GitHub 인증 준비

GitHub는 HTTPS로 push할 때 **계정 비밀번호를 받지 않습니다.** 다음 중 한 가지 방법을 미리 준비합니다.

| 방법 | 설명 |
|---|---|
| Git Credential Manager | Windows용 Git에 기본 포함되어 있습니다. 첫 push 때 브라우저 로그인 창이 열리며, 로그인하면 인증이 저장됩니다. |
| Personal Access Token(PAT) | GitHub → Settings → Developer settings → Personal access tokens에서 발급합니다. push 시 비밀번호 대신 토큰을 입력합니다. |
| SSH 키 | `ssh-keygen`으로 키를 만들고 공개 키를 GitHub → Settings → SSH and GPG keys에 등록합니다. clone 시 SSH 주소를 사용합니다. |

### 0-4. 실습 파일 내용

두 시나리오 모두 저장소에 다음 파일이 있다고 가정합니다. 저장소를 만들 때 이 파일을 함께 만들어 둡니다.

**`profile.md`**

```markdown
# 자기소개

- 이름: 홍길동
- 관심 분야: 웹 개발
- 오늘의 목표: Git 기초 익히기
```

> 충돌 실습은 **`오늘의 목표` 줄**을 서로 다르게 수정하는 방식으로 진행합니다.

---

## 시나리오 1: 로컬 브랜치 병합과 충돌 해결

### 전체 흐름

```
clone → 브랜치 생성 → 작업·commit → (동료가 main 수정) → main 최신화(pull)
      → 병합(merge) → 충돌 해결 → push → 브랜치 삭제
```

### 실습 저장소 준비

1. GitHub에서 **New repository**를 선택하여 `git-lab-merge` 저장소를 만듭니다. **Add a README file**을 체크합니다.
2. 저장소 화면에서 **Add file → Create new file**을 선택하고, 파일 이름에 `profile.md`, 내용에 [0-4](#0-4-실습-파일-내용)의 내용을 입력한 뒤 **Commit changes**를 누릅니다.

---

### 1단계. 원격 저장소 복제(clone)

원격 저장소의 전체 이력을 내 컴퓨터로 복사합니다. clone하면 원격 저장소가 `origin`이라는 이름으로 자동 등록됩니다.

```bash
git clone https://github.com/<내-계정>/git-lab-merge.git
cd git-lab-merge
```

결과를 확인합니다.

```bash
git remote -v          # origin 주소 확인
git branch -a          # 로컬 브랜치(main)와 원격 추적 브랜치(remotes/origin/main) 확인
git log --oneline      # 커밋 이력 확인
```

---

### 2단계. 작업 브랜치 생성

main에서 직접 작업하지 않고, 기능 단위로 브랜치를 만들어 작업합니다. 브랜치 이름은 `feature/작업내용` 형식을 사용합니다.

```bash
git switch -c feature/profile
```

현재 브랜치를 확인합니다. `*` 표시가 현재 브랜치입니다.

```bash
git branch
```

```
* feature/profile
  main
```

---

### 3단계. 작업 수행 후 commit

`profile.md`의 **`오늘의 목표` 줄**을 다음과 같이 수정합니다.

```markdown
- 오늘의 목표: 브랜치 병합 완벽 이해하기
```

변경 내용을 확인하고 커밋합니다.

```bash
git status                    # 변경된 파일 확인
git diff                      # 변경된 내용 확인
git add profile.md            # 스테이징
git commit -m "Update today's goal in profile"
```

커밋 이력을 그래프로 확인합니다.

```bash
git log --oneline --graph --all
```

> `feature/profile`이 `main`보다 커밋 1개 앞서 있는 것을 확인합니다.

---

### 4단계. [상황 만들기] 동료의 main 변경 재현

> 실제 현업에서는 내가 작업하는 동안 **다른 팀원이 main에 변경 사항을 반영하는 일**이 자주 일어납니다. 이 단계에서는 GitHub 웹에서 main을 직접 수정하여 그 상황을 재현합니다.

1. GitHub 저장소 화면에서 브랜치가 **main**인지 확인합니다.
2. `profile.md`를 열고 연필 아이콘(Edit)을 누릅니다.
3. **같은 줄**인 `오늘의 목표`를 다음과 같이 수정합니다.

   ```markdown
   - 오늘의 목표: Git 충돌 해결 연습하기
   ```

4. **Commit changes**를 누릅니다.

이제 원격 main과 로컬 feature 브랜치가 **같은 줄을 서로 다르게** 수정한 상태입니다.

---

### 5단계. main 브랜치를 최신 상태로 갱신(pull)

병합하기 전에 반드시 원격의 최신 main을 받아옵니다. 이 단계를 생략하면 다음과 같은 문제가 생깁니다.

- 동료의 변경 사항을 모른 채 병합하게 됩니다.
- 7단계 push가 `rejected (non-fast-forward)` 오류로 거부됩니다.

```bash
git switch main
git pull origin main
```

최신 커밋이 반영되었는지 확인합니다.

```bash
git log --oneline --graph --all
```

> main과 feature/profile이 서로 다른 방향으로 갈라진(diverged) 그래프를 확인합니다.

---

### 6단계. 작업 브랜치를 main에 병합(merge)

현재 브랜치(main)에 `feature/profile`의 변경 사항을 병합합니다.

```bash
git merge feature/profile
```

같은 줄을 서로 다르게 수정했으므로 충돌이 발생합니다.

```
Auto-merging profile.md
CONFLICT (content): Merge conflict in profile.md
Automatic merge failed; fix conflicts and then commit the result.
```

#### 6-1. 충돌 상태 확인

```bash
git status
```

```
You have unmerged paths.
  (fix conflicts and run "git commit")
  (use "git merge --abort" to abort the merge)

Unmerged paths:
        both modified:   profile.md
```

#### 6-2. 충돌 내용 확인

`profile.md`를 열면 Git이 충돌 구간에 **충돌 표시(conflict marker)** 를 넣어 둔 것을 볼 수 있습니다.

```markdown
# 자기소개

- 이름: 홍길동
- 관심 분야: 웹 개발
<<<<<<< HEAD
- 오늘의 목표: Git 충돌 해결 연습하기
=======
- 오늘의 목표: 브랜치 병합 완벽 이해하기
>>>>>>> feature/profile
```

| 표시 | 의미 |
|---|---|
| `<<<<<<< HEAD` ~ `=======` | 현재 브랜치(main)의 내용 |
| `=======` ~ `>>>>>>> feature/profile` | 병합하려는 브랜치의 내용 |

#### 6-3. 충돌 해결

어떤 내용을 남길지 결정하여 파일을 수정합니다. 한쪽만 남기거나 두 내용을 합칠 수 있습니다. **충돌 표시 3종(`<<<<<<<`, `=======`, `>>>>>>>`)은 반드시 모두 삭제합니다.**

```markdown
# 자기소개

- 이름: 홍길동
- 관심 분야: 웹 개발
- 오늘의 목표: 브랜치 병합 이해하고 충돌 해결 연습하기
```

> VS Code를 사용한다면 충돌 구간 위에 표시되는 **Accept Current Change / Accept Incoming Change / Accept Both Changes** 버튼을 활용할 수 있습니다.

충돌 표시가 남아 있지 않은지 확인합니다.

```bash
git diff --check
```

#### 6-4. 해결 결과를 스테이징하고 병합 커밋 생성

`git add`는 Git에게 **"이 파일의 충돌을 해결했다"** 고 알리는 역할도 합니다. 이 단계를 빠뜨리지 않도록 주의합니다.

```bash
git add profile.md
git commit
```

편집기가 열리면 기본 메시지(`Merge branch 'feature/profile'`)를 그대로 저장하고 닫습니다.

> - vim이 열린 경우: `:wq` 입력 후 Enter
> - 메시지를 직접 지정하려면: `git commit -m "Merge feature/profile into main"`

병합 결과를 확인합니다.

```bash
git log --oneline --graph --all
```

> 두 갈래가 하나로 합쳐진 **병합 커밋**을 확인합니다.

#### 참고: 병합을 취소하고 싶을 때

충돌 해결 도중 처음부터 다시 하고 싶다면 병합 이전 상태로 되돌립니다.

```bash
git merge --abort
```

#### 참고: 충돌이 없는 경우

main에 새로운 커밋이 없었다면 충돌 없이 **Fast-forward** 방식으로 병합됩니다. 이 경우 병합 커밋이 만들어지지 않고 main이 feature 브랜치 위치로 이동만 합니다.

```
Updating a1b2c3d..e4f5g6h
Fast-forward
 profile.md | 2 +-
```

---

### 7단계. 원격 저장소로 push

병합 결과를 원격 main에 반영합니다.

```bash
git push origin main
```

GitHub 저장소 화면에서 `profile.md`의 내용과 커밋 이력(Commits)을 확인합니다.

> **push가 거부된 경우**: 5단계 이후 다른 사람이 main에 다시 push한 것입니다. `git pull origin main`으로 다시 받아오고(필요하면 충돌 해결), 다시 push합니다.

---

### 8단계. 작업 브랜치 삭제

병합이 끝난 브랜치는 삭제하여 저장소를 깔끔하게 유지합니다. `-d` 옵션은 **병합이 완료된 브랜치만** 삭제하므로 안전합니다.

```bash
git branch -d feature/profile
git branch
```

```
* main
```

> 이 시나리오에서는 `feature/profile`을 원격에 push하지 않았으므로 로컬 브랜치만 삭제하면 됩니다.

---

### 시나리오 1 확인 체크리스트

- [ ] `git log --oneline --graph`에서 병합 커밋을 확인했습니다.
- [ ] GitHub의 `profile.md`에 충돌 해결 결과가 반영되어 있습니다.
- [ ] `git branch` 결과에 `main`만 남아 있습니다.
- [ ] 병합 전에 pull이 필요한 이유를 설명할 수 있습니다.

### 현업과의 차이점

대부분의 팀은 main 브랜치에 **직접 push하는 것을 금지(Branch protection)** 하고, 반드시 Pull Request를 거쳐 병합하도록 설정합니다. 시나리오 1의 방식은 개인 프로젝트나 소규모 작업에서 사용하며, 병합과 충돌의 원리를 이해하는 데 목적이 있습니다. 시나리오 2에서 현업 방식을 실습합니다.

---

## 시나리오 2: Pull Request 기반 협업 (GitHub Flow)

### 전체 흐름

```
clone → 브랜치 생성 → 작업·commit → 원격 브랜치 push → PR 생성
      → 코드 리뷰·수정 → (충돌 시 해결 후 재push) → 승인·병합
      → main 최신화(pull) → 로컬·원격 브랜치 삭제
```

### 역할 분담 (2인 1조)

| 역할 | 담당 |
|---|---|
| **A** | 저장소 소유자, PR #1 작성자, PR #2 리뷰어 |
| **B** | 협업자(Collaborator), PR #2 작성자, PR #1 리뷰어 |

> 두 사람이 **같은 줄을 각자 수정**하여 PR을 올리므로, 먼저 병합된 PR 이후에 나머지 PR에서 자연스럽게 충돌이 발생합니다. 현업에서 가장 흔히 겪는 상황입니다.

### 실습 저장소 준비 (A가 진행)

1. **저장소 생성**: GitHub에서 `git-lab-pr` 저장소를 **Public**으로 만들고(Add a README file 체크), [0-4](#0-4-실습-파일-내용)의 `profile.md`를 추가합니다.
2. **협업자 초대**: Settings → Collaborators → **Add people**에서 B의 GitHub 계정을 초대합니다. B는 이메일이나 GitHub 알림에서 초대를 **수락**합니다.
3. **main 브랜치 보호 규칙 설정 (권장)**: Settings → Branches → **Add branch protection rule**(또는 Add classic branch protection rule)을 선택하고 다음과 같이 설정합니다.
   - Branch name pattern: `main`
   - ☑ **Require a pull request before merging**
   - ☑ **Require approvals** (1명)

   > 이 설정을 적용하면 main에 직접 push할 수 없고 반드시 PR과 승인을 거쳐야 합니다. 비공개(Private) 저장소는 무료 플랜에서 이 기능이 제한될 수 있으므로 실습 저장소는 Public으로 만듭니다.

4. **병합 방식 설정**: Settings → General → Pull Requests에서 **Allow merge commits**가 체크되어 있는지 확인합니다. 브랜치 삭제를 직접 실습하기 위해 **Automatically delete head branches**는 체크하지 않습니다.

> **혼자 진행하는 경우**: 브랜치 보호 규칙의 Require approvals는 체크하지 않습니다(본인 PR은 본인이 승인할 수 없습니다). 7단계의 충돌 상황은 시나리오 1의 4단계처럼 GitHub 웹에서 main의 같은 줄을 직접 수정하여 만듭니다.

---

### 1단계. 원격 저장소 복제(clone) — A, B 모두

```bash
git clone https://github.com/<A의-계정>/git-lab-pr.git
cd git-lab-pr
git log --oneline
```

---

### 2단계. 작업 브랜치 생성 후 commit — A, B 각자

작업 전에 main이 최신 상태인지 확인하는 습관을 들입니다. clone 직후라면 이미 최신 상태입니다.

```bash
git switch main
git pull origin main
```

**A**의 작업:

```bash
git switch -c feature/a-goal
```

`profile.md`의 `이름`과 `오늘의 목표` 줄을 수정합니다.

```markdown
- 이름: 학습자 A
- 오늘의 목표: Pull Request 만들기
```

```bash
git add profile.md
git commit -m "Update profile by A"
```

**B**의 작업:

```bash
git switch -c feature/b-goal
```

`profile.md`의 `오늘의 목표` 줄을 수정합니다.

```markdown
- 오늘의 목표: 코드 리뷰 해보기
```

```bash
git add profile.md
git commit -m "Update today's goal by B"
```

---

### 3단계. 작업 브랜치를 원격 저장소로 push — A, B 각자

main이 아닌 **작업 브랜치**를 원격에 올립니다. `-u` 옵션은 로컬 브랜치와 원격 브랜치를 연결(upstream 설정)하여, 이후에는 `git push`만 입력해도 되도록 합니다.

**A**:

```bash
git push -u origin feature/a-goal
```

**B**:

```bash
git push -u origin feature/b-goal
```

원격 브랜치 목록을 확인합니다.

```bash
git branch -a
```

---

### 4단계. Pull Request 생성 — A, B 각자

1. GitHub 저장소 화면에 나타나는 **Compare & pull request** 버튼을 누릅니다. 버튼이 보이지 않으면 **Pull requests → New pull request**를 선택합니다.
2. 병합 방향을 확인합니다.
   - **base: `main`** ← **compare: `feature/a-goal`** (B는 `feature/b-goal`)
3. 제목과 설명을 작성합니다. 리뷰어가 이해하기 쉽도록 **무엇을, 왜** 변경했는지 작성합니다.

   ```markdown
   ## 변경 내용
   - profile.md의 이름과 오늘의 목표 수정

   ## 확인 사항
   - [ ] 오탈자 없음
   ```

4. 오른쪽의 **Reviewers**에서 상대방을 지정합니다. (A의 PR → 리뷰어 B, B의 PR → 리뷰어 A)
5. **Create pull request**를 누릅니다.

---

### 5단계. 코드 리뷰와 수정

PR의 핵심 목적은 **병합 전에 다른 사람이 변경 내용을 검토하는 것**입니다.

#### 5-1. 리뷰 작성 — 리뷰어

1. 상대방의 PR에서 **Files changed** 탭을 엽니다.
2. 수정할 줄의 왼쪽 **`+`** 버튼을 눌러 코멘트를 남깁니다.
   - 예: "목표를 조금 더 구체적으로 적어 주시면 좋겠습니다."
3. 오른쪽 위의 **Review changes**를 누르고 **Request changes**를 선택한 뒤 **Submit review**를 누릅니다.

#### 5-2. 리뷰 반영 — PR 작성자

리뷰 내용을 반영하여 수정한 뒤 **같은 브랜치에** 커밋하고 push합니다. 새로운 PR을 만들 필요가 없습니다.

**A**의 예:

```bash
# feature/a-goal 브랜치에 있는지 확인
git branch

# profile.md 수정: - 오늘의 목표: Pull Request 만들고 리뷰 반영하기
git add profile.md
git commit -m "Apply review: make goal more specific"
git push
```

> PR 화면을 새로고침하면 추가한 커밋이 **자동으로 PR에 반영**된 것을 확인할 수 있습니다. 리뷰 코멘트에는 답글을 달고 **Resolve conversation**을 누릅니다.

#### 5-3. 승인 — 리뷰어

수정 사항을 확인한 뒤 **Review changes → Approve → Submit review**를 누릅니다.

---

### 6단계. 첫 번째 PR 병합 — B가 A의 PR을 병합

PR은 원칙적으로 작성자가 아니라 **리뷰어나 저장소 관리자가 병합**합니다.

1. A의 PR 화면 하단에서 병합 버튼 옆 ▼를 눌러 **Create a merge commit**을 선택합니다.
2. **Merge pull request → Confirm merge**를 누릅니다.

> **병합 방식 비교**
>
> | 방식 | 설명 |
> |---|---|
> | Create a merge commit | 브랜치의 모든 커밋을 유지하고 병합 커밋을 추가합니다. **이 실습에서 사용합니다.** |
> | Squash and merge | 브랜치의 커밋을 하나로 합쳐 main에 추가합니다. |
> | Rebase and merge | 브랜치의 커밋을 main 끝에 하나씩 다시 적용합니다. |
>
> Squash나 Rebase 방식으로 병합하면 10단계에서 `git branch -d`가 실패할 수 있습니다. 10단계의 안내를 참고합니다.

---

### 7단계. 두 번째 PR의 충돌 해결 — B

A의 PR이 먼저 병합되면서 main의 `오늘의 목표` 줄이 바뀌었으므로, B의 PR에는 다음과 같은 메시지가 표시됩니다.

```
This branch has conflicts that must be resolved
Conflicting files: profile.md
```

현업 표준 방식인 **로컬에서 main을 작업 브랜치에 병합하여 해결**하는 방법으로 진행합니다.

#### 7-1. 최신 main 받아오기

```bash
git switch main
git pull origin main
```

#### 7-2. 작업 브랜치로 이동하여 main 병합

시나리오 1과 방향이 반대인 점에 주목합니다. 이번에는 **작업 브랜치에 main을 병합**합니다. 이렇게 하면 충돌을 내 브랜치 안에서 해결하므로 main은 항상 깨끗한 상태로 유지됩니다.

```bash
git switch feature/b-goal
git merge main
```

```
CONFLICT (content): Merge conflict in profile.md
Automatic merge failed; fix conflicts and then commit the result.
```

#### 7-3. 충돌 해결 후 커밋

`profile.md`를 열어 충돌 표시를 확인하고 해결합니다.

```markdown
<<<<<<< HEAD
- 오늘의 목표: 코드 리뷰 해보기
=======
- 오늘의 목표: Pull Request 만들고 리뷰 반영하기
>>>>>>> main
```

> 이번에는 `HEAD`가 **feature/b-goal**, 아래쪽이 **main**입니다. 현재 어느 브랜치에 있는지에 따라 위아래 내용이 바뀝니다.

두 사람의 목표를 모두 살리는 방향으로 수정합니다.

```markdown
- 오늘의 목표: Pull Request 만들기, 리뷰 반영하기, 코드 리뷰 해보기
```

```bash
git diff --check
git add profile.md
git commit -m "Merge main into feature/b-goal and resolve conflict"
```

#### 7-4. 다시 push

```bash
git push
```

PR 화면을 새로고침하면 충돌 메시지가 사라지고 **This branch has no conflicts with the base branch**가 표시됩니다.

> **참고: GitHub 웹에서 해결하는 방법**
> PR 화면의 **Resolve conflicts** 버튼을 누르면 웹 편집기에서 직접 충돌을 해결할 수 있습니다. 간단한 충돌에는 편리하지만, 코드를 실행해 확인할 수 없으므로 현업에서는 로컬에서 해결하는 방식을 주로 사용합니다.

---

### 8단계. 두 번째 PR 리뷰와 병합 — A

1. B의 PR에서 **Files changed**를 확인합니다. 필요하면 5단계처럼 리뷰를 진행합니다.
2. **Approve** 후 **Create a merge commit** 방식으로 병합합니다.

GitHub의 **Insights → Network** 화면에서 브랜치가 갈라지고 합쳐진 이력을 확인합니다.

---

### 9단계. main 브랜치에서 최신 버전 pull — A, B 모두

GitHub에서 병합된 결과는 아직 로컬 main에 반영되지 않았습니다. 로컬 main을 최신 상태로 갱신합니다.

```bash
git switch main
git pull origin main
git log --oneline --graph
```

`profile.md`에 두 사람의 변경 사항이 모두 반영되어 있는지 확인합니다.

> 반드시 **pull을 먼저 하고 10단계에서 브랜치를 삭제**합니다. pull하기 전에는 로컬 main이 병합 결과를 모르기 때문에 `git branch -d`가 "병합되지 않은 브랜치"로 판단할 수 있습니다.

---

### 10단계. 작업 브랜치 삭제 — 로컬과 원격 모두

시나리오 2에서는 브랜치가 **로컬과 원격 두 곳**에 있으므로 양쪽 모두 정리합니다.

#### 10-1. 원격 브랜치 삭제

둘 중 한 가지 방법을 사용합니다.

- **GitHub 웹**: 병합된 PR 화면의 **Delete branch** 버튼을 누릅니다.
- **명령어**:

  ```bash
  # A
  git push origin --delete feature/a-goal
  # B
  git push origin --delete feature/b-goal
  ```

#### 10-2. 로컬 브랜치 삭제

```bash
# A
git branch -d feature/a-goal
# B
git branch -d feature/b-goal
```

> **`error: The branch '...' is not fully merged.` 오류가 발생하는 경우**
> PR을 **Squash and merge** 또는 **Rebase and merge** 방식으로 병합하면, 로컬 브랜치의 커밋이 main에 그대로 들어가지 않고 새로운 커밋으로 만들어지므로 Git이 병합되지 않은 브랜치로 판단합니다. GitHub에서 병합이 완료된 것을 확인했다면 강제 삭제 옵션을 사용합니다.
>
> ```bash
> git branch -D feature/a-goal
> ```

#### 10-3. 원격 추적 브랜치 정리

원격에서 삭제된 브랜치의 흔적(`remotes/origin/feature/...`)이 로컬에 남아 있을 수 있습니다. 다음 명령어로 정리합니다.

```bash
git fetch --prune
git branch -a
```

```
* main
  remotes/origin/HEAD -> origin/main
  remotes/origin/main
```

---

### 시나리오 2 확인 체크리스트

- [ ] PR에서 리뷰 코멘트를 남기고, 수정 커밋이 PR에 자동 반영되는 것을 확인했습니다.
- [ ] PR 충돌을 로컬에서 해결하고 다시 push하여 충돌을 해소했습니다.
- [ ] 로컬 main에 두 사람의 변경 사항이 모두 반영되어 있습니다.
- [ ] 로컬 브랜치, 원격 브랜치, 원격 추적 브랜치가 모두 정리되었습니다.
- [ ] 시나리오 1과 2에서 충돌을 해결한 위치(main / 작업 브랜치)의 차이를 설명할 수 있습니다.

---

## 부록 1. 자주 발생하는 오류와 해결 방법

| 오류 메시지 | 원인 | 해결 방법 |
|---|---|---|
| `Support for password authentication was removed` | HTTPS push에 계정 비밀번호를 사용했습니다. | [0-3](#0-3-github-인증-준비)의 PAT 또는 SSH 키를 사용합니다. |
| `! [rejected] main -> main (non-fast-forward)` | 원격에 내 로컬에 없는 커밋이 있습니다. | `git pull origin main` 후 다시 push합니다. |
| `! [remote rejected] main -> main (protected branch hook declined)` | 보호된 main에 직접 push했습니다. | 작업 브랜치를 push하고 PR을 생성합니다. |
| `fatal: Need to specify how to reconcile divergent branches` | pull 통합 방식이 설정되지 않았습니다. | `git config --global pull.rebase false` |
| `error: Your local changes ... would be overwritten by checkout` | 커밋하지 않은 변경이 있는 상태에서 브랜치를 이동했습니다. | 먼저 커밋하거나 `git stash`로 임시 저장합니다. |
| `error: The branch '...' is not fully merged` | 병합되지 않았거나, Squash/Rebase 방식으로 병합되었습니다. | 병합 여부를 확인한 뒤 `git branch -D`를 사용합니다. |
| `git: 'switch' is not a git command` | Git 버전이 2.23 미만입니다. | Git을 업데이트하거나 `git checkout`을 사용합니다. |
| 병합 커밋 시 vim 화면에서 빠져나오지 못함 | 기본 편집기가 vim입니다. | `:wq` 입력 후 Enter, 또는 `core.editor`를 변경합니다. |
| 충돌 표시(`<<<<<<<`)가 포함된 채 커밋됨 | 충돌 표시를 지우지 않고 `git add` 했습니다. | 파일을 수정한 뒤 다시 커밋합니다. 커밋 전 `git diff --check`로 확인하는 습관을 들입니다. |

---

## 부록 2. 명령어 요약

| 명령어 | 설명 |
|---|---|
| `git clone <url>` | 원격 저장소를 복제합니다. |
| `git switch -c <브랜치>` | 새 브랜치를 만들고 이동합니다. |
| `git switch <브랜치>` | 브랜치를 이동합니다. |
| `git status` | 작업 디렉터리와 스테이징 상태를 확인합니다. |
| `git diff` / `git diff --check` | 변경 내용 / 충돌 표시 및 공백 오류를 확인합니다. |
| `git add <파일>` | 변경 사항을 스테이징합니다. 충돌 해결 완료 표시에도 사용합니다. |
| `git commit -m "<메시지>"` | 커밋을 생성합니다. |
| `git log --oneline --graph --all` | 전체 브랜치의 커밋 이력을 그래프로 확인합니다. |
| `git pull origin main` | 원격 main을 받아와 현재 브랜치에 병합합니다. |
| `git merge <브랜치>` | 지정한 브랜치를 현재 브랜치에 병합합니다. |
| `git merge --abort` | 진행 중인 병합을 취소합니다. |
| `git push origin main` | 로컬 main을 원격에 반영합니다. |
| `git push -u origin <브랜치>` | 작업 브랜치를 원격에 올리고 upstream을 설정합니다. |
| `git branch -d <브랜치>` | 병합된 로컬 브랜치를 삭제합니다. |
| `git branch -D <브랜치>` | 로컬 브랜치를 강제로 삭제합니다. |
| `git push origin --delete <브랜치>` | 원격 브랜치를 삭제합니다. |
| `git fetch --prune` | 삭제된 원격 브랜치의 추적 정보를 정리합니다. |
