#FROM ubuntu:latest
#LABEL authors=""
#
#ENTRYPOINT ["top", "-b"]

# 1. 자바와 파이썬이 모두 살 수 있는 환경 준비
FROM openjdk:17-jdk-slim

# 2. 파이썬과 음성 분석 도구(ffmpeg) 설치
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 3. 은주 님이 사용하는 파이썬 라이브러리 싹 다 설치
RUN pip3 install --no-cache-dir flask yt-dlp openai-whisper

# 4. 파일들 "그대로" 복사 (수정 없이!)
COPY build/libs/*.jar app.jar
COPY app.py app.py
COPY youtube_stt.py youtube_stt.py

# 5. [비기] 자바와 파이썬을 동시에 실행하기
# 파이썬(Flask)을 백그라운드(&)로 먼저 띄우고, 자바를 실행합니다.
CMD python3 app.py & java -jar /app.jar