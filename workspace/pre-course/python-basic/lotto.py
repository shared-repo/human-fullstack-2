# 로또 번호 추출기

# 동작방법
# 1. 1 ~ 45 범위의 중복되지 않는 난수를 6개 뽑기 -> 변수에 저장
# 2. 뽑힌 숫자의 평균 구하기
# 3-1. 평균이 20 ~ 26이면 결과 출력
# 3-2. 평균이 20 미만 또는 26 초과이면 1부터 다시
# 4. 사용자가 원할 때까지 반복

import random # 미리 작성해 둔 코드 (모듈) 가져오기

while True:
    # 메뉴 표시
    print("*" * 50)
    print("* 1. 당첨 예상 번호 뽑기")
    print("* 2. 종료")
    print("*" * 50)
    number = input("원하는 작업을 선택하세요 : ") # input : 사용자로부터 키보드 입력 받기

    print() # 줄바꿈 (enter 입력 역할)
    if number == '1':
        numbers = [] # 리스트형 변수 만들기 (여러 개의 데이터 저장)
        while True: # 평균 조건을 만족하는 반복
            while len(numbers) < 6: # numbers 목록에 숫자가 6개 미만인지 확인
                n = random.randint(1, 45)
                if n not in numbers: # numbers 목록에 n이 없다면 (중복되지 않는다면)
                    numbers.append(n) # numbers 변수에 n을 추가

            avg = sum(numbers) / len(numbers) # 평균 계산
            if 20 <= avg <= 26: # 평균이 지정한 범위에 포함되면
                print(f"당첨 예상 번호 : {numbers} [{avg}]")
                break
            else:
                numbers = [] # 이미 뽑은 번호 초기화

    elif number == '2':
        print("행운을 빕니다.")
        print("프로그램을 종료합니다.")
        break # 반복문을 중단하는 명령
    else:
        print("지원하지 않는 작업")

    print() # 줄바꿈 (enter 입력 역할)

    pass # 실행의 의미는 없고 형태만 구성하는 구문
