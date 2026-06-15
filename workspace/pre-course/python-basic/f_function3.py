# 성적 관리 프로그램
# - 국어, 영어, 수학 점수
# - 점수는 0 ~ 100 범위
# - 세 점수 입력 받아서 총점, 평균, 등급 계산
# - 등급 기준 : 90 ~ 100 -> A, 80 ~ 89 : B, 70 ~ 79 : C, 60 ~ 69 : D, 0 ~ 59 : F
# - 평균, 등급 출력
# - 메뉴 시스템을 사용해서 사용자가 원할 때까지 반복

def select_task():
    print("*" * 30) # 문자열에서 *는 반복 -> *을 30개 출력 
    print("* 1. 성적 처리")
    print("* 2. 종료")
    print("*" * 30)
    task = input("원하는 작업을 선택하세요 : ")
    return task

def input_score(message, min = 0, max = 100):
    while True:
        score = int( input(message) )
        if min <= score <= max:
            return score
        else:
            print(f"점수는 반드시 {min} ~ {max}으로 입력하세요")

def handle_score():
    kor = input_score('국어 점수(0 ~ 100) : ') # input 함수로 입력받는 데이터는 모두 문자열
    eng = input_score('영어 점수(0 ~ 100) : ')
    math = input_score('수학 점수(0 ~ 100) : ',)
    avg, grade = calculate_score(kor, eng, math)

    print(f'[평균 : {avg:.2f}][등급 : {grade}]')

def calculate_score(*scores):
    tot = sum(scores)
    avg = tot / len(scores)
    if 90 <= avg:
        grade = 'A'
    elif 80 <= avg:
        grade = 'B'
    elif 70 <= avg:
        grade = 'C'
    elif 60 <= avg:
        grade = 'D'
    else:
        grade = 'F'
    return avg,grade

while True:
    # 메뉴 처리
    task = select_task()

    print() # 줄바꿈

    if task == '1':
        handle_score()
    elif task == '2':
        print("프로그램을 종료합니다.")
        break # 반복문을 즉시 종료
    else:
        print('지원하지 않는 작업입니다.')

    print()