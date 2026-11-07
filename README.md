# Pro V1

Professional-grade Android videography camera app — manual ISO/shutter/focus/WB,
real log profile, honest device capability reporting, and footage that grades like
it came off a real camera.

**How it is built (AI-first pair style):** an AI engineering agent writes and commits
everything; the owner — a non-coder serving as QA — installs and verifies every
milestone on real hardware before it counts. Nothing is "done" without phone proof.

## Milestone ladder

| M | Milestone | Status |
|---|-----------|--------|
| M0 | Camera2-direct foundation, aspect-correct preview (no stretching on rotation) | 🚧 in progress |
| M1 | Record & save with correct orientation metadata | ⏳ |
| M2 | Manual controls v1 (ISO, shutter, focus, WB) + capability report | ⏳ |
| M3 | Exposure engine (AE off/lock, tap-to-meter) | ⏳ |
| M4 | Quality & motion (fps lock, resolution, bitrate, zoom, stabilization) | ⏳ |
| M5 | Real log profile via tone-map gamma (+ native log where the device has it) | ⏳ |
| M6 | Pro UX polish (grid, histogram, level, landscape-first layout) | ⏳ |

Later phases: in-app editor (trim/merge + LUT grading for log clips), then a
per-frame MediaCodec/OpenGL encode pipeline for maximum control.

## Build

The repo documents a personal-machine pipeline: Gradle 8.7, JDK 21, Android SDK
Platform 35 + Build-Tools 35.0.0. Debug builds are installed straight to the
verification phone over USB via adb.
