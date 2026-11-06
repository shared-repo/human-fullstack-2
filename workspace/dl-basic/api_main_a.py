# main.py — 전체 서버 코드
from fastapi import FastAPI, File, UploadFile, HTTPException
from pydantic import BaseModel
from PIL import Image
import torch
import torch.nn as nn
import torchvision.transforms as transforms
import io

app = FastAPI(title="MNIST 숫자 분류 API", version="1.0.0") # API 서비스 객체 만들기

# ── 전역 설정 ──────────────────────────────────────────────
CLASSES = [str(i) for i in range(10)]  # '0'부터 '9'까지의 숫자 클래스

# 디바이스 설정 (GPU 사용 가능 시 CUDA, 아니면 CPU)
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 제공해주신 MNIST_CNN 모델 클래스 정의
class MNIST_CNN(nn.Module):
    def __init__(self, num_classes=10):
        super().__init__()

        self.features = nn.Sequential(
            # Block 1: (1, 28, 28) → (16, 14, 14)
            nn.Conv2d(1, 16, kernel_size=3, padding=1),
            nn.BatchNorm2d(16),
            nn.ReLU(),
            nn.MaxPool2d(2, 2),

            # Block 2: (16, 14, 14) → (32, 7, 7)
            nn.Conv2d(16, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.MaxPool2d(2, 2),
        )

        self.classifier = nn.Sequential(
            nn.Flatten(),
            nn.Linear(32 * 7 * 7, 128),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(128, num_classes)
        )

    def forward(self, x):
        x = self.features(x)
        x = self.classifier(x)
        return x

# 모델 로드 및 가중치(state_dict) 불러오기
model = MNIST_CNN(num_classes=10).to(device)

# 학습 시 저장해둔 PyTorch 모델 가중치 파일 경로 (예: 'mnist_model.pt')
try:
    model.load_state_dict(torch.load("mnist_cnn_best.pt", map_location=device))
except Exception as e:
    print(f"경고: 모델 가중치 파일을 불러오지 못했습니다. ({e})")

model.eval()  # 평가 모드 전환

# 전처리 파이프라인 (MNIST 표준 크기 및 정규화 적용)
preprocess = transforms.Compose([
    transforms.Resize((28, 28)),
    transforms.Grayscale(num_output_channels=1),  # 흑백으로 변환
    transforms.ToTensor(),
    transforms.Normalize(mean=(0.1307,), std=(0.3081,)),
])

# ── 응답 모델 ──────────────────────────────────────────────
class PredictResponse(BaseModel): # JSON으로 자동 변환
    class_name: str
    class_id:   int
    confidence: float
    all_scores: dict[str, float]

# ── 엔드포인트 ──────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok", "model": "MNIST CNN (PyTorch)"}

@app.post("/predict", response_model=PredictResponse)
async def predict(file: UploadFile = File(...)):
    # 이미지 형식 검증
    if file.content_type not in ["image/jpeg", "image/png", "image/webp"]:
        raise HTTPException(status_code=400, detail="JPG/PNG/WEBP 이미지만 허용됩니다.")

    # 이미지 로딩 및 전처리 (MNIST는 흑백이므로 'L' 모드로 변환)
    contents = await file.read()
    image    = Image.open(io.BytesIO(contents)).convert("L")
    tensor   = preprocess(image).unsqueeze(0).to(device)   # (1, 1, 28, 28) 형태의 텐서로 변환 후 디바이스 이동

    # PyTorch 추론
    with torch.no_grad():
        outputs = model(tensor)  # (1, 10) 로짓 출력
        
        # Softmax를 통해 확률 값으로 변환
        probs = torch.softmax(outputs, dim=1)[0].cpu().numpy()

    pred_id   = int(probs.argmax())
    all_scores = {CLASSES[i]: round(float(probs[i]), 4) for i in range(10)}

    return PredictResponse(
        class_name = CLASSES[pred_id],
        class_id   = pred_id,
        confidence = round(float(probs[pred_id]), 4),
        all_scores = all_scores,
    )