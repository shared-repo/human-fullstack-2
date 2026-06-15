import random

print("=== 로또 당첨 예상번호 추출기 ===")

running = True
while running:
    # 조건을 만족하는 번호 세트를 찾을 때까지 반복
    finding = True
    while finding:
        # 1~45 범위의 중복되지 않는 숫자 6개 뽑기
        numbers = random.sample(range(1, 46), 6) # sample은 한 번에 여러 개의 난수 발생
        
        # 평균 계산
        total = 0
        for num in numbers:
            total += num
        avg = total / 6
        
        # 평균이 20 ~ 26 범위에 포함되는지 확인
        if 20 <= avg <= 26:
            # 정렬하여 출력
            numbers.sort() # numbers 리스트를 오름차순으로 정렬
            print(f"추출된 번호: {numbers}, 평균: {avg:.2f}")
            finding = False
    
    # 계속 여부 확인
    choice = input("번호를 다시 추출할까요? (y/n): ").lower()
    if choice != 'y':
        running = False

print("프로그램을 종료합니다.")
