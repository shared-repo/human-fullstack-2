CREATE TABLE IF NOT EXISTS todos (
  id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  done  BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO todos (title) VALUES
  ('Docker 기초 학습'),
  ('Spring Boot 컨테이너화'),
  ('Docker Compose 실습');
