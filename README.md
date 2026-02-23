# Whisper.cpp Android Validation App

A focused, high-quality sample app for validating **Whisper.cpp** (OpenAI's Whisper model in C++) on Android. This app demonstrates high-performance, on-device speech-to-text with both real-time (live) and file-based transcription capabilities.

## Key Features

- **Live Transcribe**: Real-time microphone capture with lightweight VAD (Voice Activity Detection) and instant transcription via Whisper.cpp.
- **File Transcribe**: High-speed offline transcription of existing audio/media files.
- **JNI Integration**: Clean, documented bridge between Kotlin and the native C++ Whisper engine.
- **Model Management**: Automated downloader for Whisper models (`base`, `tiny`, `small`) optimized for Android storage.
- **Simple & Fast**: Removed legacy recorder overhead to provide a clear, developer-friendly validation target.

## Project Structure

- `app/src/main/cpp/`: The native Whisper.cpp engine and JNI bridge.
- `app/src/main/java/com/innosage/androidagentictemplate/whisper/`: Kotlin wrappers for the native Whisper context.
- `app/src/main/java/com/innosage/androidagentictemplate/TranscriptionEngine.kt`: Unified engine for handling transcription tasks.
- `app/src/main/java/com/innosage/androidagentictemplate/AudioRecordService.kt`: Lightweight foreground service for live capture.

## Getting Started

1. Clone the repository.
2. Open in Android Studio (Giraffe or newer recommended).
3. Connect an ARM64-v8a device (physical device required for best performance).
4. Build and Run.
5. The app will automatically prompt to download the Whisper `base` model on first launch.

## Agentic Engineering

This project is maintained using the **Agentic Engineering** methodology, ensuring high-quality, autonomous development and surgical focus on the Whisper.cpp implementation.

---
InnoSage ⚡️
Whisper.cpp by Georgi Gerganov.
