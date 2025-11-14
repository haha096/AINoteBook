import sys
from pathlib import Path

import yt_dlp
import whisper


def download_audio(url: str, out_dir: Path) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)

    # yt-dlp 옵션: 오디오만 다운로드
    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": str(out_dir / "%(id)s.%(ext)s"),
        "noplaylist": True,
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        filename = ydl.prepare_filename(info)

    return Path(filename)


def transcribe_audio(audio_path: Path, model_name: str = "small") -> str:
    print(f"[INFO] Whisper 모델 로딩 중... ({model_name})")
    model = whisper.load_model(model_name)

    print(f"[INFO] 음성 인식 시작: {audio_path}")
    result = model.transcribe(str(audio_path), verbose=True)
    text = result.get("text", "").strip()
    return text


def main():
    if len(sys.argv) < 2:
        print("사용법: python youtube_stt.py <YouTube URL>")
        sys.exit(1)

    url = sys.argv[1]
    base_dir = Path(__file__).parent
    downloads_dir = base_dir / "downloads"
    outputs_dir = base_dir / "outputs"

    print(f"[INFO] YouTube 오디오 다운로드 중...\n URL: {url}")
    audio_file = download_audio(url, downloads_dir)
    print(f"[INFO] 다운로드 완료: {audio_file}")

    text = transcribe_audio(audio_file, model_name="small")

    outputs_dir.mkdir(parents=True, exist_ok=True)
    out_txt = outputs_dir / f"{audio_file.stem}.txt"
    out_txt.write_text(text, encoding="utf-8")

    print("\n[RESULT] ===== 인식된 텍스트 일부 =====")
    print(text[:500], "...\n")
    print(f"[INFO] 전체 자막 저장 위치: {out_txt}")


if __name__ == "__main__":
    main()