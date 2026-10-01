import cv2
import numpy as np
import torch
import torch.nn as nn
import torchvision.transforms as transforms

# 1. Custom CNN Model Architecture
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

# Setup device and model
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
model = MNIST_CNN().to(device)

# Load your model weights
model_path = 'mnist_cnn_best.pt'  # Update with your .pth file path
model.load_state_dict(torch.load(model_path, map_location=device))
model.eval()  # Disables Dropout and freezes BatchNorm stats for inference

# Standard MNIST transform
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize((0.1307,), (0.3081,))
])

# 2. Initialize Webcam
cap = cv2.VideoCapture(0)

def preprocess_digit_roi(roi):
    """Pads and resizes digit ROI to centered 28x28 format."""
    h, w = roi.shape
    max_dim = max(h, w)
    pad_y = (max_dim - h) // 2
    pad_x = (max_dim - w) // 2
    
    # Square canvas to maintain aspect ratio
    padded = np.zeros((max_dim, max_dim), dtype=np.uint8)
    padded[pad_y:pad_y + h, pad_x:pad_x + w] = roi
    
    # Resize inner digit to 20x20 with 4px border margin
    resized = cv2.resize(padded, (20, 20), interpolation=cv2.INTER_AREA)
    final_canvas = np.zeros((28, 28), dtype=np.uint8)
    final_canvas[4:24, 4:24] = resized
    
    return final_canvas

while True:
    ret, frame = cap.read()
    if not ret:
        break

    # Grayscale conversion and Gaussian blur
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)

    # Invert binary: dark ink becomes white foreground, paper becomes black
    thresh = cv2.adaptiveThreshold(
        blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
        cv2.THRESH_BINARY_INV, 11, 2
    )

    # Detect contours of digits
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    for cnt in contours:
        x, y, w, h = cv2.boundingRect(cnt)

        # Discard tiny artifacts and overly large regions
        if 20 < w < 400 and 30 < h < 400:
            roi = thresh[y:y+h, x:x+w]
            
            # Format ROI to 28x28 MNIST style
            digit_input = preprocess_digit_roi(roi)
            
            # Prepare tensor shape: (1, 1, 28, 28)
            tensor_img = transform(digit_input).unsqueeze(0).to(device)

            # Inference
            with torch.no_grad():
                outputs = model(tensor_img)
                probabilities = torch.softmax(outputs, dim=1)
                confidence, predicted = torch.max(probabilities, 1)

            pred_digit = predicted.item()
            conf_val = confidence.item()

            # Render detection if confidence is high enough
            if conf_val > 0.70:
                cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 255, 0), 2)
                
                label = f"{pred_digit} ({conf_val*100:.0f}%)"
                label_y = max(y - 10, 20)
                cv2.putText(frame, label, (x, label_y),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)

    cv2.imshow('MNIST Webcam Reader', frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
