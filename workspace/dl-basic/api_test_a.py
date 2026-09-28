# Python으로 API 테스트
import requests # 웹서버에 요청을 보내는 도구

url   = "http://localhost:8000/predict"
files = {"file": ("test.jpg", open("test-7.jpg", "rb"), "image/jpeg")}
resp  = requests.post(url, files=files)

print(resp.json())
# {
#   "class_name": "cat",
#   "class_id": 3,
#   "confidence": 0.8731,
#   "all_scores": {"airplane": 0.01, "cat": 0.87, ...}
# }
