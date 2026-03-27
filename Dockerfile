#FROM ubuntu:latest
#LABEL authors=""
#
#ENTRYPOINT ["top", "-b"]

# 1. 자바 17 환경 준비
FROM eclipse-temurin:17-jdk

# 2. 파이썬과 음성 분석 도구(ffmpeg) 설치
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 3. 파이토치를 'CPU 전용'으로 아주 가볍게 설치
RUN pip3 install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu --break-system-packages

# 4. 나머지 파이썬 라이브러리 설치
RUN pip3 install --no-cache-dir flask yt-dlp openai-whisper --break-system-packages

# 5. Whisper 모델 미리 다운로드 (빌드할 때 한 번만!)
RUN python3 -c "import whisper; whisper.load_model('small', download_root='/root/.cache/whisper')"

# 6. 파일들 복사
COPY build/libs/*.jar app.jar
COPY youtube-stt/app.py app.py
COPY youtube-stt/youtube-stt.py youtube-stt.py

# 7. 자바와 파이썬 동시에 실행
CMD python3 app.py & java -jar /app.jar