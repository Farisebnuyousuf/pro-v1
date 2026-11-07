# Chapter 07 — Glossary and sources

## 7.1 Glossary (as used by person or chatbot readers of this book)

- **AE (auto-exposure)** — HAL-measured exposure; its OFF state is the manual path.
- **AWB / Kelvin WB** — auto white balance vs temperature-anchored manual gains.
- **AF (diopter)** — focus strength in inverse meters; 0 = infinity.
- **Anamorphic** — horizontally squeezed optics (×1.3×/×1.5/×2); de-squeezing is a preview-only restretch; the recorded frame stays squeezed.
- **APV** — "Advanced Professional Video" codec added in Android 16 (this phone's OS).
- **Bit depth (video)** — 8-bit (AVC/HEVC SDR-class) vs 10-bit (HEVC + HLG path) pipeline depth.
- **BTC (BT.2020)** — wide-gamut primaries; colour-volume partner of HLG.
- **CFA (RGGB/BGGR)** — Bayer colour filter pattern of the sensor.
- **CONSTRAINED_HIGH_SPEED_VIDEO** — HAL capability; the session class that owns 60+ fps.
- **Contrast curve (tonemap)** — user curve mode with N waypoints (≤64 here); the substrate of in-app Log.
- **CROP/zoom** — digital zoom by sub-crop of the sensor array; density-driven.
- **DNG** — stills RAW container; carries white level/black levels/shading references.
- **Dynamic range profiles** — Camera2 API-34+ profile classes for 10-bit SDR/HDR (HLG10, HDR10-class vendor presets).
- **EIS/OIS** — electronic vs optical stabilization; distinct controls in the HAL.
- **EV (exposure value)** — exposure compensation in stops; device step 0.1 EV.
- **Gyroflow** — gyro-data-driven desktop video stabilizer; app-side task is metadata, not stabilization math.
- **HAL3** — Android's hardware abstraction layer interface for camera triplets.
- **High-speed session** — `CameraConstrainedHighSpeedCaptureSession` (see above).
- **HLG (Hybrid Log-Gamma)** — BT.2100 HDR transfer; Android-native 10-bit partner.
- **Log (curve)** — low-contrast, wide-dynamic-range transfer to preserve grade room; implemented as camera curve (ISP path) or GPU LUT; consumed in post via its paired technical LUT.
- **MediaCodec / MediaMuxer** — encoder and container (MP4) writer; MOV is container/addressing bookkeeping around the same tracks.
- **Metering region** — weight-scaled window for 3A algorithms; count is device-bound (1 here).
- **Mired** — 1,000,000/Kelvin; interpolation axis behind WB curves.
- **PCM 32-float** — uncompressed float audio; WAV-representable.
- **RAW10** — packed 10-bit Bayer; white level 1023 on this main sensor.
- **Session surface** — image sink bound at session-config time (preview/record/analysis/RAW).
- **S-Log-class curves** — industry standard log family names (generic here).
- **TAP-AF / tap-meter** — windowed AF/AE triggered from the viewfinder.
- **TARGET_FPS_RANGE** — AE stepping table; constant-fps mode = min==max rows.
- **Technical LUT** — post-forward LUT that inverts an app Log to universal grading space.
- **Tonemap** — the ISP's tone transfer stage; curve points bound by `maxCurvePoints`.
- **VCF (vendorVcf)** — vivo's computational camera provider (front/extra pipelines).
- **vendor tags** — OEM extension keys (`org.codeaurora.*`, `org.quic.*`, `vivo.*`); probed, never assumed.
- **White level** — RAW max code value (1023 here → 10-bit).

## 7.2 Sources (every citation used)

**Measured (device):**
1. `adb shell dumpsys media.camera`, iQOO I2221, 2026-11-07 — raw: `pro-v1/docs/raw/camera_dump_2026-11-07.txt` (30,267 lines)
2. `adb shell dumpsys package lv.mcprotector.mcpro24fps` — public package metadata (version 043dd / 430037, minSdk 24, targetSdk 36)

**Official (developer-published, factual digests):**
3. mcpro24fps.com — **Full Specification** page (Cameras/Video/Log/RAW/Audio categories; HLG10/HDR10/APV/500 Mb/s/LUTs/HOMG mentions)
4. mcpro24fps.com — **User Guide** index (workflow recipe list, settings structure, camera/version notes; PDF source 2021 editions)
5. mcpro24fps.com — **Icons** page (full UI icon semantics list)
6. Google Play listing — package id, developer replies, feature digest ("Perhaps the most powerful…", 10-bit/HLG/Log/LUTs/de-anamorphic/pulls/peaking/zebra/Kelvin)
7. Samsung Galaxy Store listing — publisher name "Chantal Pro SIA", price ($19.99) signal, same digest
8. profilmmakerapps.com third-party directory entry — cross-confirm publisher/price ($17.99)/category ("Cinematographers, Directors" tags)

**Platform (learned-in-book, applied as engineering):**
9. Android Camera2 API / `CameraCharacteristics` / `DynamicRangeProfiles` / `CameraConstrainedHighSpeedCaptureSession` / `DngCreator` platform reference (>API 24..36)
10. MediaCodec/MediaMuxer/AudioRecord platform references
11. OpenGL ES 3.1 (float precision baseline for the GPU path)
12. ITU-R BT.2100 (HLG), BT.2020 (wide gamut), CIE daylight/CBT-blue entitlement (WB curves)

**Boundary record (why "no decompile" is not a tension):**
13. Apple EULA/licensed-application clause noted in past sessions; Android-equivalent stack: the app's own license terms + copyright law; ownership of a copy is a platform-consumer license, not a source-code license. The study book is built from 1–12 only.
