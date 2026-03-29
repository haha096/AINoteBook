# app.py (새 파일)

import sys
from pathlib import Path
from flask import Flask, request, jsonify

import yt_dlp
import whisper

# --- 1. Whisper 모델을 전역 변수로 딱 한 번만 로드합니다. ---
# 이게 핵심입니다! 서버가 켜질 때 딱 한 번만 실행됩니다.

#.\.venv\Scripts\activate
#deactivate

print("[INFO] Whisper 모델을 로드합니다... (small)")
WHISPER_MODEL = None

def get_whisper_model():
    global WHISPER_MODEL
    if WHISPER_MODEL is None:
        WHISPER_MODEL = whisper.load_model("small", download_root="/root/.cache/whisper")
    return WHISPER_MODEL
print("[INFO] Whisper 모델 로드 완료.")

# --- 2. Flask 앱 생성 ---
app = Flask(__name__)

# --- 3. youtube_stt.py에 있던 함수들을 '그대로' 복사 ---

def download_audio(url: str, out_dir: Path) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": str(out_dir / "%(id)s.%(ext)s"),
        "noplaylist": True,
        "nocheckcertificate": True,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        filename = ydl.prepare_filename(info)
    return Path(filename)

# (수정!) transcribe_audio 함수 수정
# - 매번 모델을 로드하지 않고, 전역에 로드된 WHISPER_MODEL을 사용합니다.
# - (model_name 파라미터 제거)
def transcribe_audio(audio_path: Path) -> str:
    print(f"[INFO] 음성 인식 시작: {audio_path}")

    # 전역 모델을 사용하여 음성 인식
    result = get_whisper_model().transcribe(str(audio_path), verbose=True)
    text = result.get("text", "").strip()
    return text

# --- 4. API 엔드포인트 정의 ---
# Java(VideoController)가 호출할 API 주소입니다.
@app.route("/transcribe", methods=["POST"])
def run_transcription():
    # 1. 클라이언트(Java)가 보낸 JSON에서 URL을 받음
    data = request.json
    url = data.get("url")
    if not url:
        return jsonify({"error": "URL이 제공되지 않았습니다."}), 400

    downloads_dir = Path("/tmp/downloads")

    try:
        # 2. 오디오 다운로드 (youtube-stt.py의 main과 동일)
        print(f"[INFO] YouTube 오디오 다운로드 중...\n URL: {url}")
        audio_file = download_audio(url, downloads_dir)
        print(f"[INFO] 다운로드 완료: {audio_file}")

        # 3. 음성 인식 (youtube-stt.py의 main과 동일)
        text = transcribe_audio(audio_file)

        # (선택) 오디오 파일 삭제
        # audio_file.unlink()

        # 4. Java에게 자막 텍스트를 JSON으로 반환
        return jsonify({"text": text})

    except Exception as e:
        print(f"[ERROR] 처리 중 오류 발생: {e}")
        return jsonify({"error": str(e)}), 500

# --- 5. Flask 서버 실행 ---
if __name__ == "__main__":
    # 5001번 포트에서 서버 실행 (다른 번호도 가능)
    # debug=True는 개발 중 유용합니다.
    app.run(host='0.0.0.0', port=5001, debug=True)