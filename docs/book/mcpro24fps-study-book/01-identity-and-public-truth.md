# Chapter 01 — Identity, ecosystem, and the public feature taxonomy

## 1.1 What the application is

mcpro24fps ("mcPro 24 fps") is a **professional manual video camera application for
Android only**. It packages a phone's Camera2/HAL3 hardware into a
cinematographer's tool: manual ISO/shutter/focus/WB, industry-oriented Log
capture, 10-bit HDR, RAW video tiers, professional framing/scoping aids, and a
dense preset/monitoring workflow. It positions itself against stock camera apps
("the native application") and against pro-camera apps on other platforms.

One-sentence identity, for a reader or chatbot:

> **mcpro24fps is a third-party Android camera app (Chantal Pro SIA, Latvia) that
> exposes the device's raw Camera2/HAL3 camera potential — manual controls, log
> gamma on the sensor path, 10-bit HDR, RAW tiers, and pro monitoring controls —
> wrapped in a videographer UI, with per-device capability honesty and a
> companion desktop ecosystem for pricing/free-demo and post formats.**

## 1.2 Publisher and distribution (verified public facts)

| Fact | Value |
|---|---|
| Legal publisher | Chantal Pro SIA, Bebru iela 16–34, Jēkabpils, LV-5201, Latvia (reg. 40103332030, VAT LV40103332030) |
| Contact | info@mcpro24fps.com; public support via website forum/feedback, Telegram (@mcpro24fps_en) |
| Android package | `lv.mcprotector.mcpro24fps` (full version); separate free package for the Demo |
| Stores | Google Play, Huawei AppGallery, Samsung Galaxy Store, RuStore |
| Price signals (public listings) | ≈ $17.99–$19.99 one-time for the full app; Demo free |
| Ecosystem (desktop) | mcpro24fps RAW Converter (Windows 10/11 x64, from their site), mcpro24fps Meta (Windows, Microsoft Store) |
| Companion assets | Public **Technical LUTs** library (free downloads) for interpreting their Log profiles in NLEs |
| Version policy | Frequent feature releases; digits-in-version scheme (installed here: `043dd`); long-running compatibility: minSdk 24 (Android 7.0), targetSdk 36 on this install |

Key product rule published by the developer, and respected in this book:

> "The functionality of the features depends on the technical capabilities [of the
> device]." — i.e., the app never promises features a phone's HAL cannot
> express. It verifies capability per device (encouraged by their free Demo: "test
> on your model before purchasing"). This *honest capability reporting* is the
> single most important architectural idea to copy.

## 1.3 The public feature taxonomy (official full-specification page, paraphrased factually)

The official site organizes the app's surface into these categories. Each maps to
a mechanism section in Chapter 04 and a code module in Chapter 05:

**A. Cameras**
- Multiple camera modules; per-module settings persisted separately; module presets; camera orientation lock; duplicate-camera hiding.
- Mechanism: `CameraManager.getCameraIdList()`, `CameraCharacteristics`, logical+physical camera ids, per-id SharedPreferences namespaces.

**B. Video (recording core)**
- FPS presets 24/25/30/60 and more; resolutions "all defined by Camera2 API" up to 4K/8K where the device exposes them; containers MP4 and MOV with preferred selection; codecs AVC/HEVC (bit depths: AVC 8-bit; HEVC 8/10-bit), **APV codec support on Android 16+** (matches this phone, Android 16); RAW video tiers (16/14/12/10/8-bit, Linear/Log); bitrate modes variable/constant with values up to ~500 Mb/s where hardware allows; constant-FPS mode; "bitrate hack"-style extended bitrate options.
- Mechanism: `SurfaceView/ZaeSurface` + `MediaCodec` encoder config, `MediaMuxer` (MP4/MOV), `ConstrainedHighSpeed` sessions for non-30 fps locks, `DynamicRangeProfiles` (API 34+) for 10-bit, vendor/APV platform support check.

**C. HDR / color depth**
- HLG10 / HDR10 / Dolby-Vision-class modes where the device advertises them; 8/10-bit HEVC; the rule "GPU processing OFF required for 10-bit" (see D).
- Mechanism: `CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES` ⊇ `DYNAMIC_RANGE_TEN_BIT` (this phone: yes), `DynamicRangeProfiles` + `COLOR_SPACE_PROFILES`.

**D. GPU processing (their famous toggle)**
- Two capture paths: (1) **GPU/GL path** — preview frames pass through an
  OpenGL ES 3.1 pipeline (filters, LUTs, de-anamorphic, scale, reverse tone
  mapping) and the recorder may ingest the processed stream or skip it;
  (2) **No-GPU path** — recording wires the sensor output *directly* to the codec
  ("record Log without turning on the GPU, like on big cameras"), which is what
  enables 10-bit and maximizes fidelity. This dual-path bookkeeping (enable GPU ⇒
  lose 10-bit, per their own guide) is a professional-grade engineering decision.
- Mechanism: shared `SurfaceTexture`/`EGLSurface` chain vs. direct codec input
  surface; exact-encoder-format checks per path.

**E. Log ecosystem**
- Multiple Log profile presets per scenario + gamut selection; two display-LUT types for on-set monitoring; "camera curve" editor (sliders, points, save/import); HOMG-style per-sensor Log curve tuning; technical LUTs generator in-app; LUT embedding/extracting into files; universal META-LUT; custom GPU curve presets.
- Mechanism: `CaptureRequest.TONEMAP_MODE` CURVE with `ToneMapCurve` building (see 05), `tonemap.maxCurvePoints` (this phone: 64), fallback GPU-log via shader when the HAL curve path is absent; LUT export/import (".cube"), metadata blocks inside MP4/MOV.

**F. RAW video + RAW stills ecosystem**
- RAW video bit-depth tiers (16/14/12/10/8-bit Linear/Log) via vendor-supported paths; RAW still capture (DNG), vignette correction in RAW, black level lock, hot-pixel correction, sharpness/NR hardware controls; RAW file converter ecosystem on desktop.
- Mechanism: RAW/RAW_SENSOR stream formats + `DngCreator`, per-sensor white level (this phone main camera: 1023 → 10-bit), shading/lens-shading handling, vendor session parameters.

**G. Exposure engine**
- Manual ISO/shutter with step buttons (+/- per stop), auto⇄manual flip with two taps; 180° shutter-angle helper ("set manual exposure with the 180° rule" guide section); shorter/longer shutter nudges; AE lock vs autofollow; EV compensation slider; AE autolock; spectral & zebra exposure aids; flicker control via higher shutter.
- Mechanism: `CONTROL_AE_MODE` OFF + `SENSOR_SENSITIVITY`/`SENSOR_EXPOSURE_TIME` (ranges in Ch. 02), `AE_LOCK`, `AE_EXPOSURE_COMPENSATION` (±4 EV step 0.1 here), metering regions (1 here), per-axis UI math (gain dB, shutter ms, EV).

**H. Focus, zoom, pulls (their signature "control suite")**
- Continuous AF, tap-AF, manual focus with "own infinity point" calibration; programmable focus/zoom starts ("bottom limit"/"top limit", long-press to set) with smooth animated pull runs startable mid-recording; zoom-limited modes (to video size / ×1.5 / ×2.0 / camera max); focus/expo peaking; focus-zoom combined pulls.
- Mechanism: `CONTROL_AF_MODE` OFF + `LENS_FOCUS_DISTANCE` (calibration APPROXIMATE on this phone — 10 cm min focus on main), `SCALER_CROP_REGION`-based digital zoom ml math (+ `ZoomControl`-style smooth animation where offered), overlay shader for peaking.

**I. White balance & colour**
- Kelvin WB with nudge control (their Kelvin math curves per sensor), AWB single-shot "hold to auto", saved WB presets; colour gamut selection (P3, sRGB, 709-class), colour matrix control, colour range metadata (limited/full).
- Mechanism: `COLOR_CORRECTION_MODE`+`COLOR_CORRECTION_GAINS`/`AWB_MODE`+CT→Gains model (Kelvin → daylight RGB gains formula mapping, implemented in 05), `COLOR_CORRECTION_TRANSFORM`, per-stream colour space declaration.

**J. Stabilization & gyroscope**
- Optical (OIS toggle where advertised) + digital stabilization; commodity "stabilise in Gyroflow" path — i.e., embed unbiased gyro/meta data so Desktop Gyroflow can smooth footage; their own metadata embedding supports that workflow.
- Mechanism: OIS via `LENS_OPTICAL_STABILIZATION_MODE` (available here on main lens), EIS via vendor session parameters (vivo: `vivo.control.*`, `seamlesscontrol`) — HAL-standard EIS reported off; gyro via `Sensor.TYPE_GYRO` log + MXF/MP4 meta wiring (same philosophy as the owner's `gyro-companion` project).

**K. Audio**
- WAV and M4A; PCM 32-bit float / 16-bit; AAC up to 510 kb/s; external mics and Bluetooth devices; separate audio files (WAV to Music/) or embedded WAV in MP4; sample-rate/bitrate menus; audio VU indicators.
- Mechanism: `AudioRecord` (Float PCM stages), `AACEncoder` via MediaCodec + `MediaMuxer` tracks, BT/sco routing, ≥3 mic permissions usage.

**L. Monitoring & scopes**
- Histogram, spectral highlights, zebras (over-exposure paint), expo peaking, focus peaking, skin-tone check (marking skin/neutral areas), false-colour-class assists (spectral), framing grids (11 types), center marker/level with expanded variants, brightness modes (three), viefinder custom limits (Bottom limit, resolution caps in preview/recording), external monitor/recorder via clean-HDMI-style clean output.
- Mechanism: preview YUV/GL analysis pass (luma histograms, zebra/paint shader, grid draw), `VIEWFINDER` via `SurfaceTexture`; clean-signal output Uses a second source surface.

**M. Metadata & files**
- Advanced metadata embedding (colour profile description, LUT block, gyro meta, name/version into MP4/MOV), filename conventions (date/path templates), storage settings (DCIM/mcpro24fps, WAV files to Music/mcpro24fps on OTA≥10 per old docs; sandboxed on 9).
- Mechanism: `MediaMuxer` format-independent meta boxes / timed-metadata, 'udta'-like metadata blocks (implement detail in 05), `MediaScannerConnection` indexing.

**N. Control & UI system**
- Volume buttons as shutter/rec controls, Bluetooth remote (marker: "not shown in this version"), full icon set (documented publicly — orientation locks, flashlight, presets, exposure, focus, zoom panel, WB panel, curves panel, gamut panel, matrix panel, format panels, media manager, gyro marker, purchase-locked format markers, etc.).
- Mechanism: standard accessibility/click routes; deeper integration of `MediaRecorder`-unrelated custom pipeline (no `MediaRecorder`: they run their own MediaCodec chain, implied by continuous versatile metadata + codec marginals).

**O. Presets and per-camera sandboxes**
- Presets = camera + resolution + fps + codec + bitrate + colour profile + curves + WB LUT GPU sound metadata snapshot; ISO/shutter/focus/EV excluded (per their guide — "not saved"); per-camera separate settings sandboxes; quick-apply after confirm; preset backup/restore globally.
- Mechanism: JSON preset objects + `.getSharedPreferences` per camera id + versioned migration.

**P. Platform legacies and notes**
- Android 7.0 minSdk: chosen because CameraShim-era manual control APIs are dependable there; targetSdk 36 today (Android 16) for APV and dynamic formats; OpenGL ES 3.1 minimum for GPU path (float precision for curve/LUT math); storage rules across Scoped-Storage API 29+.

## 1.4 The published workflow doctrine (their guide's "How to" spine)

Their user guide organizes practice around these recipes — each is a workflow a
pro-app must support end to end (all covered again as mechanisms in Ch. 04):

1. Set up before the first shoot (permissions: **camera, microphone, storage**; GPU review)
2. Set manual **exposure** with the 180° rule (shutter ≈ 2×fps)
3. Shoot **Log with a monitoring LUT** (LUT on preview, "clean" Log into the file)
4. **Remove flicker** from lights (shutter/ISO sync to mains Hz)
5. **Pull focus and zoom smoothly** (limits + pull runner)
6. Shoot **slow motion**
7. **Record RAW** (RAW bundle + desktop converter pipeline)
8. Record **good sound** (PCM 32-float, external BT)
9. Use an **external monitor or recorder** (clean signal port)
10. **Stabilize in Gyroflow** (gyro metadata for desktop smoothing)
11. Shoot with an **anamorphic lens** (de-squeeze preview; file stays anamorphic)
12. **Switch between setups with presets**

## 1.5 Why this app matters to pro-v1 (book purpose, once plainly)

pro-v1's README milestone ladder (M0 camera-foundation → M6 Pro UX) is the same
feature surface, organized in build order. This book's job: hand pro-v1's
(non-coder owner + AI builder) the complete, legitimate, citable knowledge layer
for M2–M6 — device-verified limits, mechanisms, and code — so the builder never
needs to touch anyone else's binaries, and every design decision cites the
reference class (mcpro24fps as study material) without copying it.

## 1.6 Chapter source set

- Official site: mcpro24fps.com — full-specification, guide index, icons pages (fetched 2026-11-07)
- Galaxy Store and Google Play listings (identical public feature digests, price/copy signals)
- Profile Fippleman Apps listing (third-party industry directory entry summarizing the same digest)
- Installed-package introspection (public metadata only): `dumpsys package lv.mcprotector.mcpro24fps`
- Device capabilities: `dumpsys media.camera` (Ch. 02)
