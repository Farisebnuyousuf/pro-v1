# Chapter 03 — Camera2/HAL3 architecture: the machine every pro camera app drives

## 3.1 The stack, one paragraph and one diagram

Android's camera stack is: **app → Camera2 API (java/Kotlin) → system service
(camera_provider binder) → HAL3 (Qualcomm/vivo vendor process) → sensor, ISP,
encoder hardware**. Camera2 is a message-queue system: the app submits
**CaptureRequests**, the HAL processesthem against **Surfaces** and returns
**CaptureResults** plus pixels to the surfaces. A modern pro video camera app is
an orchestra around this loop.

```
 ┌─────────────────────────── app process ───────────────────────────┐
 │ UI (Compose/View)   Controls (Knobs/Buttons)   Presets/Settings   │
 │        │                    │                       │             │
 │        ▼                    ▼                       ▼             │
 │ ┌──────────── ROM: CapturePipeline ─────────────────────────────┐  │
 │ │ CameraDevice  →  CameraCaptureSession                         │  │
 │ │  - repeating request (preview/YUV analysis)                   │  │
 │ │  - one-time requests (AE OFF, ISO, EV, meter)                 │  │
 │ │  - high-speed session (60–480 fps)                            │  │
 │ │  surfaces:  preview  →  SurfaceView/SurfaceTexture            │  │
 │ │             codec    →  MediaCodec.createInputSurface()       │  │
 │ │             image    →  YUV→scopes   or   ImageReader (DNG)   │  │
 │ │  GL path (optional): SurfaceTexture → EGL → shader → codec    │  │
 │ └───────────────┬───────────────────────────────────────────────┘  │
 └─────────────────┼──────────────────────────────────────────────────┘
                   ▼ binder
            CameraService + CameraProvider (AIDL/HIDL)
                   ▼
             HAL3 (Qualcomm qti / vivo vcf process)
                   ▼
        Sensor (externally-computed exposure) → ISP (3A, tonemap, NR)
                   ▼ outputs per surface: JPEG/DNG, YUV, PRIVATE, 10-bit
             MediaCodec (AVC/HEVC/APV) → MediaMuxer (MP4/MOV/…) → file
```

## 3.2 CaptureRequest anatomy (the actual "settings object")

Every frame's behavior is a field set on a repeating `CaptureRequest`:

| Concern | Key(s) | Type value |
|---|---|---|
| Exposure mode | `CONTROL_AE_MODE` | OFF(0)/ON(1)/flash variants |
| Gain | `SENSOR_SENSITIVITY` | ISO units |
| Shutter | `SENSOR_EXPOSURE_TIME` | nanoseconds |
| AE lock/comp | `CONTROL_AE_LOCK`, `CONTROL_AE_EXPOSURE_COMPENSATION` | bool / ×step |
| Metering | `CONTROL_AE_REGIONS` (5-tuples x,y,w,h,weight) | up to 1 here |
| FPS riding | `CONTROL_AE_TARGET_FPS_RANGE` | [min,max] of table |
| Focus mode | `CONTROL_AF_MODE` | OFF(0) for manual |
| Focus distance | `LENS_FOCUS_DISTANCE` | diopters |
| WB mode | `CONTROL_AWB_MODE` | OFF → manual gains |
| WB gains | `COLOR_CORRECTION_GAINS` | R/B float gains |
| Tone/gamma | `TONEMAP_MODE`, `TONEMAP_CURVE_RED/GREEN/BLUE` | mode + 64-pt curves |
| Crop/zoom | `SCALER_CROP_REGION` | Rect within active array |
| OIS | `LENS_OPTICAL_STABILIZATION_MODE` | OFF/ON (main lens) |
| Stabilization | `CONTROL_VIDEO_STABILIZATION_MODE` | OFF on this HAL |
| NR / edge | `NOISE_REDUCTION_MODE`, vendor sharpening | 0–4 / vendor |
| Color | `COLOR_CORRECTION_MODE`, `ABERRATION_MODE` | FAST/HIGH_QUALITY |
| Output ts | `SENSOR_TIMESTAMP (result)` | REALTIME ns clock |

A repeating request = your "preview mode". A capture request = stills, DNG, AE
measurement. Every UI knob is a small diff against the repeating request.

## 3.3 Sessions and surfaces (the "wiring")

- `CameraCaptureSession` binds one request pattern to a fixed surface set.
- On this phone: max 1 raw, 3 processed, 2 miscellaneous-output streams
  (`maxNumOutputStreams [1 3 2]`) → canonical pro wiring on device 0:
  **1 preview `PRIVATE` + 1 record `YUV/PRIVATE` to codec + 1 `YUV` analysis** —
  and DNG stills need their one-side RAW slot (stop or swap accordingly).
- **Constrained high-speed session**: a special
  `createConstrainedHighSpeedCaptureSession` where the request must be queued as
  pairs (`fps range min==max` ≥ 60) — this is where 60/120/240/480 fps live on
  phones without full-manual AE fps tables.
- **Dynamic-range applications (10-bit)**: session must be created with a
  `DYNAMIC_RANGE_TEN_BIT (HLG)` profile (`DynamicRangeProfiles`); PRIVATE
  recording surface → codec advertises 10-bit formamarketability.

## 3.4 Results and statistics (what the app can learn per frame)

`CaptureResult` returns live values: `SENSOR_SENSITIVITY`/`EXPOSURE_TIME`
(what AE actually used — shown in the app's information area), AE/AF/WB states
(`CONTROL_AE_STATE: SEARCHING/CONVERGED/FLASH_REQUIRED...`) for word-accurate
"locked" UI, plus `F` statistics like LSC maps (for RAW correction), 1-region
metering data, real timestamps (REALTIME clock → used to align audio/gyro
metadata and Gyroflow logs).

## 3.5 Concurrency budget & failover

- A pro app must budget: preview (continuous) + encoder (record) + analysis
  (i.e. low-freq YUV copies) + occasional DNG still. On device budget:
  1 raw + 3 processed + 2 misc — fits exactly.
- High-speed sessions forbid multi-purpose request tartgets; release/rebuild
  strategy is the source of every "menu locked while recording" UI state — the
  mcpro24fps guide's rule "presets list doesn't open during recording" is the
  logical consequence, not a whim.
- Rebuild pipeline whenever: profile (HLG/SDR) changes, lens changes, fps tier
  changes (standard←→high-speed), resolution changes, bistream adaptation.
  Each rebuild = `close()` → configure → `setRepeatingRequest`.

## 3.6 Where the two GPU philosophies hook in

- **No-GPU path (HAL-direct):** sensor → format-declared output → codec input
  surface; tonemap/WB decide curve/gamut inside the ISP. This is their "Log
  without GPU = like big cameras" — cleanest data, 10-bit possible.
- **GPU path:** sensor → SurfaceTexture (PRIVATE) → GL texture → fragment
  shader chain (curves, LUT (.cube 3D or 1D), peaking/zebra/grid overlays,
  de-anamorphic squeeze, sharpness/NR graders) → screen and/or →
  `Surface` re-bound to codec. 10-bit UN-possible in this path (byte streams in
  GL ES), matching their published rule: GPU ON ⇒ 8-bit ceiling.
  Also enables features ISP can't: on-set LUT preview for any curve, false
  colours, de-squeeze anamorphic preview (file stays squeezed).

## 3.7 File containers, codecs, audio, metadata (the output plane)

- **Video:** `MediaCodec` (H.264 avc1 / H.265 hev1-hvc1 / APV on Android 16+ when
  shipped) fed by an input `Surface`; `MediaMuxer` for **MP4** (mp42) and
  **MOV** (via custom muxer or format trick — mp4 demuxer recognizes MOV-class
  tags; apps implement MOV by muxing mp4 and tweaking FTYP).
- **Audio:** `AudioRecord` PCM 16-bit/float, feed into `MediaCodec` AAC encoder
  track (510 kb/s class) → mux with video; or write standalone WAV (32-float)
  for old-school sync. External/BT mic = routing pane + `AudioRecord` inside.
- **Metadata plane:** muxer-level metadata (colour primaries/transfer/matrix
  NCL SELECT / CICP, colour pre-set names, vendor LUT blocks, gyro logs),
  file naming rules, and the desktop side (`mcpro24fps RAW Converter`-class tools
  consuming `.RAW`/DNG + `mcpro24fps Meta`).
- **Orientation:** `MediaFormat.setInteger("rotation")` (0/90/180/270) is the
  canonical set, plus per-surface `SOURCE` transform flags for preview.

## 3.8 The app-shaped storage & persistence layers

per-camera sandbox prefs (module settings per camera id, their "settings saved
per camera"), preset store (JSON serialization; bypass volatile per their guide:
ISO/shutter/focus/EV live outside presets), UI state advices, LUT library
(opts `.cube` and in-file metadata), gyro log store for Gyroflow-format
companion files, and capability-report cache (see `G4` honest-report module).
