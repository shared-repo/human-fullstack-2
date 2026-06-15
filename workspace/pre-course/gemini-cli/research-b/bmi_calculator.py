print("=== BMI(체질량지수) 계산기 ===")

# 입력 받기
weight_input = input("몸무게(kg)를 입력하세요: ")
height_input = input("키(cm)를 입력하세요: ")

# input으로 받은 데이터는 모둔 문자열이므로 입력값을 숫자로 변환
try:
    weight = float(weight_input)
    height = float(height_input)

    # 0 이하의 값에 대한 처리
    if weight <= 0 or height <= 0:
        print("오류: 키와 몸무게는 0보다 커야 합니다.")
    else:
        # BMI 계산 (키는 cm이므로 m로 변환)
        height_m = height / 100
        bmi = weight / (height_m ** 2)

        # 결과 분류
        if bmi < 18.5:
            category = "저체중 (Underweight)"
        elif bmi < 25:
            category = "정상 (Normal)"
        elif bmi < 30:
            category = "과체중 (Overweight)"
        else:
            category = "비만 (Obesity)"

        # 결과 출력
        print(f"\n입력 정보: 키 {height}cm, 몸무게 {weight}kg")
        print(f"BMI 지수: {bmi:.2f}")
        print(f"결과: {category}")

except ValueError:
    print("오류: 숫자만 입력 가능합니다.")
except Exception as e:
    print(f"예상치 못한 오류가 발생했습니다: {e}")
