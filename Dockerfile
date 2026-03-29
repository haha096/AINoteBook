#FROM ubuntu:latest
#LABEL authors=""
#
#ENTRYPOINT ["top", "-b"]

# 1. 자바 17 환경 준비
FROM eclipse-temurin:17-jdk

# 2. 필수 패키지 및 Node.js(유튜브 암호 해독용) 설치
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    curl \
    && curl -sL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# 3. yt-dlp '최신 버전' 강제 설치 (매우 중요)
RUN pip3 install --no-cache-dir flask openai-whisper --break-system-packages

# 4. Whisper 모델 로드
RUN python3 -c "import whisper; whisper.load_model('small', download_root='/root/.cache/whisper')"

# 5. 작업 디렉토리 설정 및 파일 복사
WORKDIR /app
COPY build/libs/*.jar app.jar
COPY youtube-stt/app.py app.py
COPY youtube-stt/youtube-stt.py youtube-stt.py

# 6. 보안 권한 설정

# 7. 실행 (포트 5001 오픈 확인)
CMD python3 app.py & java -jar app.jar