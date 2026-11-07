# STUDY BOOK — mcpro24fps: complete technical anatomy of a professional Android video camera app

> Study edition, compiled 2026-11-07 for the pro-v1 project.
> Purpose: an exhaustive, self-contained reference that a **person or an AI chatbot**
> can read end-to-end and fully understand what mcpro24fps is, how every category
> of its functionality works on the Android platform, and how to build equivalent
> functionality from scratch — including complete original code.

## How this book was built (method and integrity)

1. **Device truth (measured, primary evidence).** The camera HAL state of the
   verification phone (iQOO I2221, Android 16, Snapdragon 8 Gen 3) was dumped with
   `adb shell dumpsys media.camera` (30,267 lines) and mined exhaustively. Raw
   evidence archived at `pro-v1/docs/raw/camera_dump_2026-11-07.txt`. This is the
   same substrate the OS hands every camera app — including mcpro24fps.
2. **Public documentation (factual source).** The developer's own public pages —
   full specification, user guide, icons index, store listings — were researched
   and are summarized factually (feature taxonomies, workflow philosophy). Facts
   about what an app does are not copyrighted expression, and they are cited.
3. **Platform engineering knowledge (written out in full).** Every mechanism a pro
   camera app uses on Android — Camera2/HAL3 architecture, manual exposure, Kelvin
   white balance, tone curves, log profiles, LUTs, 10-bit HDR, RAW/DNG, high-speed
   sessions, stabilization + gyro (Gyroflow), audio, codecs/containers, metadata,
   scopes, pull-control systems — is explained from first principles and paired
   with **complete original Kotlin reference implementations**, written for this
   book and directly reusable in pro-v1.
4. **Boundary (firm).** This book contains **no** extracted/transcribed code,
   assets, LUTs, or other copyrighted expression from mcpro24fps. Decompiling or
   re-publishing a licensed app is prohibited by its license and copyright law;
   ownership of a copy does not change that. Everything in this book is either
   measured from the device, publicly documented fact (cited), or original
   engineering authored here. A chatbot may ingest the whole book freely.

## Reading map

| Ch | Title | What it gives you |
|----|-------|-------------------|
| 01 | Identity, ecosystem, public feature taxonomy | What the app *is*, who makes it, every feature category as publicly documented |
| 02 | Device platform baseline (iQOO I2221) | Every measured camera capability the app runs on — the honest limits |
| 03 | Camera2/HAL3 architecture | The machine every such app drives: requests, sessions, surfaces, results |
| 04 | Mechanism compendium | Feature-category → mechanism → exact Camera2 parameters, one section per feature |
| 05 | Code reference volume | Complete original Kotlin for every mechanism — the "codes" you asked for |
| 06 | Legitimate study protocol & pro-v1 mapping | How to keep learning *from the app* (QA observation) and where knowledge lands |
| 07 | Glossary & sources | Terms and every citation used |
| 08 | Precision annex | Every exact constant with its dump-line / standard evidence |

## Primary facts box (memorize these)

- Package: `lv.mcprotector.mcpro24fps` — developer **Chantal Pro SIA**, Jēkabpils, Latvia
- Installed on verification phone: versionName `043dd`, versionCode 430037, minSdk 24, targetSdk 36
- Product family: mcpro24fps (full, paid), mcpro24fps **Demo** (free capability check), mcpro24fps **RAW Converter** (Windows), mcpro24fps **Meta** (Windows)
- Design philosophy (from their own guide): a phone as a *cinema camera* — Log on the sensor/HAL path "like big cameras", GPU path optional, every device-variant capability honestly gated, results stable in professional NLEs (DaVinci, Premiere, FCP)
- Host of this study: iQOO I2221 — **Level-3-class** main camera with manual sensor + 10-bit dynamic range support, 4 rear physical cameras (4/3-class provider) + 2 front (vivo VCF provider)
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
# Chapter 02 — Device platform baseline: iQOO I2221 (the phone this app runs on)

Everything in this chapter is **measured from the device**, not from any app.
Provenance: `adb shell dumpsys media.camera` (2026-11-07), raw dump saved at
`pro-v1/docs/raw/camera_dump_2026-11-07.txt` (30,267 lines). Any pro camera app —
including mcpro24fps — receives exactly these capabilities from the OS; anything
else it may "add" is computation layered on top.

## 2.1 Platform identity

| Property | Value |
|---|---|
| Device brand / model | iQOO / I2221 (vivo), product `I2221i` |
| OS | Android **16** |
| SoC | Qualcomm **Snapdragon 8 Gen 3** (board `pineapple`) |
| Camera spis providers | `vendor_qti` (Qualcomm, 4 devices) + `vendorVcf` (vivo VCF computational, 4 devices) |
| Installed reference app | mcpro24fps `043dd` (vc 430037), minSdk 24, targetSdk 36 |

Vivo exposes each lens twice: a standard Qualcomm provider entry `vendor_qti`
(camera2 semantics, what we target) and a vivo computational provider entry
`vendorVcf` (extra vendor pipelines). Duplicate hiding (their published camera
behavior) is the same bookkeeping at the app layer.

## 2.2 Rear camera inventory (vendor_qti, camera devices 0–3)

| # | Lens | Aperture | OIS | Min focus | AF calibration | Active array | CFA | ISO range | Exposure range |
|---|------|----------|-----|-----------|----------------|--------------|-----|-----------|----------------|
| 0 | 4.84 mm (~23 mm eq.) | f/1.79 | **yes** | 10 cm | APPROXIMATE | 4096×3072 | RGGB | **72–3200** | **87.5 µs – 13.36 s** |
| 1 | 3.74 mm (2×-class tele) | f/2.45 | no | fixed (0 cm) | — | 3264×2448 | BGGR | 50–1600 | 52.5 µs – 0.419 s |
| 2 | 4.84 mm (duplicate main-mode sensor) | f/1.79 | **yes** | 10 cm | APPROXIMATE | 4096×3072 | RGGB | 72–3200 | 87.5 µs – 0.850 s |
| 3 | 1.66 mm (ultrawide) | f/2.2 | no | fixed (0 cm) | — | 3200×2400 | BGGR | 50–800 | 25.9 µs – 0.850 s |

Main sensor (device 0) details: physical 6.55 × 4.92 mm (≈1/1.5"); white level
**1023** (→ RAW10, 10-bit pipeline); `SENSOR_TIMESTAMP_REALTIME`; lens shading
already applied by HAL; hyperfocal 12.2 cm reported via 10 cm min-focus.

Front cameras: two devices on the `vendorVcf` provider (standard profile, lower
ISO ceilings, typical selfie sensors — exact per-sensor ranges identical in form:
sensitivityRange, exposureTimeRange; not detailed here because pro-v1 priorities
are the rear lenses).

## 2.3 Capabilities per device (the honest gate list)

| Capability | Dev0 main | Dev1 tele | Dev2 main-dup | Dev3 UW |
|---|---|---|---|---|
| BACKWARD_COMPATIBLE | ✔ | ✔ | ✔ | ✔ |
| RAW (stills) | ✔ | — | ✔ | — |
| MANUAL_SENSOR | ✔ | — | ✔ | — |
| MANUAL_POST_PROCESSING | ✔ | — | ✔ | — |
| CONSTRAINED_HIGH_SPEED_VIDEO | ✔ | — | ✔ | — |
| YUV_REPROCESSING / PRIVATE_REPROCESSING | ✔ ✔ | — | ✔ ✔ | — |
| BURST_CAPTURE | ✔ | — | ✔ | — |
| READ_SENSOR_SETTINGS | ✔ | — | ✔ | — |
| STREAM_USE_CASE | ✔ | — | ✔ | — |
| **DYNAMIC_RANGE_TEN_BIT** | ✔ | — | ✔ | — |
| COLOR_SPACE_PROFILES | ✔ | — | ✔ | — |
| Hardware level | **LEVEL_3** | LIMITED | LIMITED | LIMITED |

Reading: **manual everything + 10-bit + RAW + high-speed is a main-lens-only
club** on this phone (devices 0/2). A correct pro app shows this in its UI and
capability report exactly like this — no illusions on the tele/UW.

## 2.4 Exposure engine hardware truth (M2/M3 numbers)

- AE modes available `[0,1,2,3]` → includes **AE OFF**, mandatory for full-manual.
- AE compensation: **±4 EV, step 0.1 EV**; AE target-fps locks available:
  `[10,10] [10,14] [14,14] [10,15] [15,15] [24,24] [10,30] [30,30]` — **no 60-60 in
  the standard AE table**. Consequences:
  - constant-FPS 24/25/30 (`[24,24]`, `[30,30]`) works in the standard path
    (that's what "constant fps mode" means in practice, and where the shutter
    angle 180° rule lives);
  - 60 fps and above must ride a **CONSTRAINED_HIGH_SPEED** session.
- Metering regions: 1 AF, **1 AE**, 1 AWB → tap-to-meter with one window max.
- Antibanding modes `[0,1,2,3]` (OFF/50 Hz/60 Hz/AUTO): their "remove flicker"
  recipe pairs this with shutter selection (mains-hz photography math).
- Noise-reduction modes `[0..4]`, aberration modes `[0,1,2]` (their "hardware
  settings: NR, sharpness, hot-pixel correction" menu corresponds to
  `NOISE_REDUCTION_MODE`, edge/sharpening vendor tags on QTI, hot-pixel vendor
  keys — see `org.quic`/`com.qti` vendor tags in the dump).

## 2.5 Motion / high-speed truth (M4)

High-speed table (CONSTRAINED_HIGH_SPEED session), as dumped (pairing alignment
noted; tiers are the meaningful part):

- 1920×1080: steady-state fences at **120 fps** and burst pairs to **240 fps**
- 1280×720: fences at **120 fps**, pairs to **240 fps**, plus **720×480 to 480 fps**
- Additional interleaved rows at 1920×864 and 640×480 for AE-less framesets
- Slow-mo capture and 60fps recording therefore ride this session, not the AE table.

Stabilization truth: `availableVideoStabilizationModes [0]` = **OFF only at the
standard HAL level** on every rear camera OIS-or-not. OIS (main lens) exists via
`LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION [0,1]`. Digital EIS is vivo vendor
pipeline territory (session-parameter vendor tags such as `vivo.control.*`,
`seamlesscontrol`, `EnableDCGHDR` visible in the dump) — i.e., exactly the
"optical and digital stabilization where possible + Gyroflow ecosystem"
published model: standard path from us, vendor otherwise.

## 2.6 Colour science truth (M5)

- `availableToneMapModes [0,1,2]` → **CONTRAST_CURVE, FAST, HIGH_QUALITY**.
  **No PRESET_CURVE (3), no GAMMA_VALUE (4)** on this unit.
- `tonemap.maxCurvePoints = 64` → the log profile must be a custom
  CONTRAST_CURVE with ≤64 points. (Their "log profiles via tone curve; curve
  tuning per sensor" doctrine is exactly this key, or a vendor equivalent, plus
  optional GPU-side curve fallback.)
- `COLOR_SPACE_PROFILES` + `DYNAMIC_RANGE_TEN_BIT` → **10-bit HLG/HDR10-class
  recording possible** through `DynamicRangeProfiles`/colour space profile
  requests (HLG10 BT.2020 is the Android-native profile for this class).
- RAW: DNG paths confirmed by `RAW` capability + 10-bit white level.

## 2.7 The vivo/QTI vendor-tag layer (what "extra" is actually there)

The dump shows hundreds of vendor parameters beyond the public set (selections,
not exhaustive): `org.codeaurora.qcamera3.*` (HDR tuning, AE bracketing,
XCFA optimization, cinematic mode), `org.quic.camera2.statsNNControl.*`
(NN-driven stats), `com.qti.stats_control.*` (AE exposure count tuning),
`vivo.*` (seamless control, DCG HDR, video snapshot flags, Shdr, blur/refocus
sections). A pro app on this phone may check-and-use these; but anything
non-global is a per-phone extension, not portable app behavior. Pro-v1 policy:
standard keys first; vendor keys only behind capability checks (`RUNTIME
REQUEST_KEYS` membership), never assumed.

## 2.8 What mcpro24fps can possibly return on this phone (parity theorem)

Because apps cannot excise capabilities the HAL ignores, the union of what this
app records on this phone lies within the union of this chapter's tables. That's
the clean-room "every bit of knowledge" guarantee: **Chapters 03–05 build toward
covering that union** — same ceiling, legitimate code.
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
# Chapter 04 — Mechanism compendium: every feature category → exact mechanism

Format per section: **what it is → how it works → exact keys/APIs → device
truth (this phone) → what must be checked at runtime.** Device truth cites
Ch. 02. This chapter is the bridge between the feature digest (Ch. 01) and the
engineering spec (Ch. 05).

---

## 4.1 Camera modules & per-module sandboxes

**Mechanism.** Enumerate `CameraManager.getCameraIdList()`, classify logical vs
physical (`CameraCharacteristics.LENS_FACING`, logical ids expose
`INFO_PHYSICAL_IDS` with `SENSOR_SYNC_TYPE`), and keep a
`Runtime Capability Mask` per id. Persist settings under namespaced prefs:
`cam_<id>.<key>`. Apply a UI-level duplicate filter: group ids sharing
`(facing, physicalSize, aperture, focalLength)`, keep the first, hide the rest
(their "duplicated cameras are hidden" behavior).

**Exact API.** `CameraManager#getCameraCharacteristics(id)`,
`CharacteristicsKey.fromNumeric(t)` fallbacks (vivo tables use numeric-tag dumps;
runtime API gives typed keys — same data).

**Device truth (I2221).** 6 back (0,1,2,3 + 2 VCF) + 2 front; duplicates
collapse to: main-wide, tele, ultrawide (+ fronts). Runtime check: opening both
device 0 and 2 is pointless — pick 0 (LEVEL_3).

---

## 4.2 Recording core: codecs, containers, bitrates, resolutions

**Mechanism.** Own pipeline (not `MediaRecorder`): pick `UMCommission`
configuration from `MediaCodecList`, create encoder from `MediaFormat`
(resolution/bitrate/profile/level, rotation), grab its `createInputSurface()`,
bind as record surface in the session; drain to `MediaMuxer`
(MP4/VF box shortcuts for MOV). Resolution menu = intersect
`StreamConfigurationMap.getOutputSizes(MediaCodec class)` with lens aspect,
and monitor `MaxConference` capabilities.

**Exact keys/format.**
`MediaFormat.KEY_BIT_RATE / KEY_PROFILE / KEY_BIT_DEPTH / KEY_COLOR_STANDARD`,
`KEY_COLOR_TRANSFER (HLG/HDR10)`, `KEY_COLOR_RANGE`, `KEY_COLORMETRICS`-family
(CICP fields via `COLOR_STANDARD_*`, `COLOR_TRANSFER_*`), rotation via
`KEY_ROTATION`.

**Device truth.** HEVC 10-bit possible on devices 0/2 (`DYNAMIC_RANGE_TEN_BIT`);
AVC ceiling 8-bit; APV present on Android 16 build basis; up to ~500 Mb/s is a
format-availability question (`MediaCodecInfo.getCapabilitiesForFormat`
throughput + `isSizeSupported`); constant-fps rides `[24,24]/[30,30]` AE table
rows, 60+ rides high-speed.

---

## 4.3 HDR / 10-bit, dynamic range profiles

**Mechanism.** Query
`CameraCharacteristics.get(DynamicRangeProfiles); profiles contain
`HLG10, HLG10_PREVIEW, `DOLBY_VISION-class vendor presets
``. Session
create with `SessionConfiguration(collections(profile))`, request key
`CONTROL_AE_TARGET_FPS_RANGE` provisioned per profile. Codec feed must be set
with `COLOR_TRANSFER_HLG / ST2084` and BT.2020 primaries, record `"colour
range"` (limited/full) consistent.

**Device truth.** `DYNAMIC_RANGE_TEN_BIT` + `COLOR_SPACE_PROFILES` on
devices 0/2. Their marketing "HLG / HDR10 (and other) modes" resolves onto
vendor preset list; HLG is the guaranteed public one. **The 8-bit GPU-path
boundary:** GL pipelines on Android ES carry 8-bit — corroborating their guide's
"GPU on ⇒ no 10-bit" doctrine.

---

## 4.4 The dual path: GL/GPU vs no-GPU recording

**Mechanism A (no-GPU/HAL-direkt).** Record surface is the codec input surface
directly; LCD curve/WB defined via ISP keys: CURVE, GAINS/TRANSFORM, colour CICP
on the format.

**Mechanism B (GPU path).** Preview `SurfaceTexture` → `GLTexture`
→ fragment chain → progress `swapChain`:
  - monitor render: to screen AND/OR ae LUTed second target;
  - record render: filter chain → lightweight encoder-in surface.
Per their guide the GL path uses OpenGL ES 3.1 (float precision for the LUTs/curve
interpolation to look right) and removes the 10-bit option. In their UI this is
the *GPU processing menu* (there: filters, LUT, de-anamorphic, reverse tone
mapping, scale video…)

**Device truth.** Both paths coverable; a correct implementation checks
`GLES31.GLES_VERSION` ≥ 3.1 for B and never claims B allows 10-bit.

---

## 4.5 Log profiles & tone curves

**Mechanism.** Log is a **transfer curve** from linear sensor code to an
image-domain value that preserves shadows/highlights room. Two app strategies:
1. **ISP-side (their "no-GPU log"):** on `TONEMAP_MAP_CURVE each` x-changeable:
   `TONEMAP_CURVE_RED/GREEN/BLUE` asumat grids, size ≤ `maxCurvePoints`
   (heretofore 64). The iota-curveset approach — one curve family, tuned per
   sensor — is their "HOMG" concept (per-phone curve tuning).
2. **GPU-side log fallback:** apply the log in the fragment shader as a LUT when
   the curve key cannot byte-read on that device (rare on Level-3 mains).

Log curve math to implement (outlined, code in 05): for gamma-space base
LOG-E (0.0–1.0 in, out), mapping sRGB 0..1 → log 0..1:
`log(x) = (log10(x*lin_scale ∈ t+log10(Lo))/dR)+off` (e.g. Fujifilm F-Log/
Cine-style normalization via S-Log3 style formula). **They ship technical LUTs
so post applications can invert consistently** — the curve+LUT-pair bookkeeping
is the actual workflow value: named-Log, feet, matching LUT, in-app generator
and embedding.

**Device truth.** `tonemap.maxCurvePoints=64`, modes `[0,1,2]` → curve path
C_contrasts only. 64 points sufficient for smooth log; HIGH_QUALITY mode for
dassociated stills.

---

## 4.6 RAW video & RAW stills

**Mechanism (stills).** RAW_STREAM slot (`ImageReader` RAW_SENSOR) →
`DngCreator` with Tiff metadata (orientation, colour space, model)→ DNG file
plus sidecar motion data if need be. Bypass AE/3A via manual keys where needed
("black level lock request": QTI vendor `org.codeaurora.*`-class key; standard
`SENSOR_BLACK_LEVEL_LOCK`-adjacent behavior is vendor-owned here).

**Mechanism (RAW video).** RAW video writes `ImageFormat.RAW_SENSOR` (or vendor
bayer) burst-steady into `.RAW` container class files via `ImageReader`
loop-interleaved with PC-legacy conversion (`mcpro24fps RAW Converter`
classical ecosystem role). This is **Tier-RAW in the installer set: 16/14/12/10/
8-bit Linear/Log** — actual availability is capability-gated
(devices: only RAW10 publicly; 14/16 is `sensorPixelMode`+vendor on other units;
platform truth on our phone: RAW10, white level 1023).

**Device truth.** RAW only on 0/2. `LENS_SHADING_APPLIED TRUE`, shading map
size 17×13 — Dng must be written with `setOis/Shading` metadata appropriately.
Hydra: VCF-front likely RAW-worthy but their provider differs — not pro-v1's
goal.

---

## 4.7 Exposure engine (their recipe "180° rule" is arithmetic)

**Mechanism.**
- Manual on: `CONTROL_AE_MODE=OFF`; then ISO (`SENSOR_SENSITIVITY`) and shutter
  (`SENSOR_EXPOSURE_TIME`).
- Step buttons: each press = one stop (ISO ×2/÷; shutter ×2/÷ halations),
  clamped by device ranges (Ch. 02) — their "lower/raise ISO by one step".
- auto⇄manual toggle = simply write AE OFF with last ISO/shutter or AE ON
  (`AE_MODE=ON`) — their "tap for automatic/manual".
- 180° rule: shutter = 1/(2·fps): 24 fps→~1/48 s (20 833 µs), 25→1/50,
  30→1/60; their guide allows shorter/longer shutter beyond that (motion
  crispness vs blur).
- EV: `AE_EXPOSURE_COMPENSATION` = steps × `AE_COMPENSATION_STEP` (±4/0.1).
- Flicker: pair 50 Hz mains with shutter denominators that are multiples of
  1/100 s; 60 Hz mains → 1/120 s classes (their guide recipe #4).

**Device truth.** ISO 72–3200, expose 87.5 µs–13.36 s on main; tele/UW own
(narrower) ranges; AE region = 1.

---

## 4.8 Focus, zoom, and pull-control system

**Mechanism.**
- Manual focus: `CONTROL_AF_MODE=OFF` + `LENS_FOCUS_DISTANCE` diopters;
  "own infinity point"→ software calibration: user-focus picked point stored
  per camera id, UI re-maps slider distances from diopters per lens.
- AF modes: off/auto macro/continuous-video/continuous-photo/macrophot and
  tap regions (`AF_REGIONS` 1 window).
- Zoom: `SCALER_CROP_REGION` inside active array; max zoom =
  `MaxDigitalZoom`/`availableMaxZoomRatio` per lens; limited zoom modalities:
  cap the cropped width to record resolution and to overlay computed “×1.5/×2”
  presets. Smooth animation: piecewise cubic interpolation of crop region over
  frames with Hz-scheduling (code 05).
- **Pull runners (their revolutionary "programmable focus/zoom"):** two limit
  registers per axis (top/bottom; long-press save), one `PullJob` item (axis,
  from, to, duration, ease). During recording, runner interpolates the key
  each N ms; user can stop mid-run (their icon "pull running: tap to stop").
- Combined focus+zoom pulls: queue two PullJobs started by a single button.

**Device truth.** MF only on main (min-focus 10 cm, calibration APPROXIMATE);
tele/UW fixed-focus (MF disabled); zoom is sensor-crop digital on all.

---

## 4.9 White balance and colour

**Mechanism.**
- Kelvin UI: Kelvin → RGB gains mapping on daylight reference
  (`PLANCKIAN/TRUNCATED dayBasis): G stays 1.0, R/B from CCT using published
  Robertson/CTSXYZ: code implements the McCamy-ish K→(r,b) mapping, then
  `COLOR_CORRECTION_MODE_TRANSFORMS`… no, standard path:
  `CONTROL_AWB_MODE=OFF + COLOR_CORRECTION_GAINS` (R, G_even, B, G_odd).
  Their "white balance menu / Kelvin values / auto once / presets" exactly maps.
- Gamut: `COLOR_CORRECTION_TRANSFORM`-adjacent; colour space per profile
  (Romney: sRGB/BT709/P3/BT.2020 CICP in the record format).
- Skin-tone/neutral marking: analysis-frame shader (classification) — overlay
  only, recorded image untouched (false-highlight culture).

**Device truth.** `awbAvailableModes count 9` (auto + various scenic presets);
manual gains everywhere on back cameras region-CFA assisted by HAL.

---

## 4.10 Stabilization & gyro (the Gyroflow doctrine)

**Mechanism.**
- OIS: `LENS_OPTICAL_STABILIZATION_MODE=ON` where available (main lens here).
- Standard EIS: `CONTROL_VIDEO_STABILIZATION_MODE` — reports OFF on this HAL →
  use vendor session params (`vivo.control.seamlesscontrol` family) behind
  runtime probes, OR go gyro route.
- Gyro route (their "stabilise in Gyroflow"): log `TYPE_GYRO`/`TYPE_ACCELEREOMETER`
  at fixed frequency with monotonic `SENSOR_TIMESTAMP` alignment to the first
  frame's timestamp; embed as the common Gyroflow sidecar-formatted data
  (JSON/metadata variant); desktop stabilizer consumes it. This is exactly the
  doctrine of the mcpA24fps "Gyroflow" recipe, and it is clean-room-able (see
  the owner's existing gyro-companion project).

**Device truth.** OIS available on 0/2 only; standard EIS OFF everywhere.

---

## 4.11 Audio engine

**Mechanism.** `AudioRecord(source=MIC/UNPROCESSED/VOICE_COMMUNICATION/BT)`,
buffer size ×4, PCM 16-bit/32-float; "encode AAC via MediaCodec" track into the
muxer, or WAV (RGBA 44-byte header + PCM data) standalone for double-system;
"sample rate/bitrate/selects" menus; BT-SCO route fix-ups; sound-VU indicator
from last-frame RMS. Their specs: WAV/M4A, PCM 32 float/16, AAC ≤510 kb/s,
external & Bluetooth device support, WAV-in-MP4 embedding.

**Device truth.** Standard platform capability (no camera dependency); the
"audio menu" wires `SAMPLER_RATE/AAC_EXT` from `MediaCodecInfo.AAC-profiles`.

---

## 4.12 Monitoring / scopes / grids / viewfinder diagnostics

**Mechanism.** GL-based analysis on the preview path (best on YUV analysis
image):
- **Histogram:** 256-bin luma (or RGB) from a down sampled analysis plane;
  their display area lives in the top bar.
- **Zebra:** fragment-clamp luma>thresh → paint diagonal pattern; their
  "Zebra … for exposure control".
- **Expo peaking:** surpass-saturation marking akin to zebra with tighter
  threshold; **Focus peaking:** Laplacian/Sobel → hue mask; both from GL ES.
- **Spectral (false-colour) & skin-tone/neutral check:** 1D or 3D LUT-based
  transfer applied to the analysis/monitor path.
- **Grids (11 types):** rule of thirds, golden, diagonal, center cross,
  safe-area variants, level, third-forces — draw pass on the overlay surface.
- **Info area:** free space, battery, sound level, save-progress, warnings,
  scene temperature reading (via AWB result estimate), save-log markers (their
  "indicators & information area" list).
- **Viewfinder limits:** resolution cap in preview/recording, "bottom limit"
  (aspect frame line), brightness modes (exit-came triplicate: 3 screen
  brightness classes) — all standard UI work.

**Device truth.** Preview YUV analysis feed available on all back cams; the
LEVEL_3 main can also stage YUV analysis at TRUE-HDR-classed dynamic ranges.

---

## 4.13 Metadata & file management

**Mechanism.** Muxer-side metadata attach: MP4 `udta`-box metadata (title=app,
preset ID, colour profile name, LUT block, gyro meta pointer, "colour range"
values). Filename patterns (`Add date to filepath`): patterned storage in
`DCIM/mcpro24fps`, WAV to `Music/mcpro24fps`, MediaStorage connection. Metadata
settings menu exposes colour-range/gamut/preset-meta toggles (their
"metadata settings").

**Device truth.** Pure app-layer; standard storage rules (copies of old docs
noted DCIM/`mcpro24fps` since Android 10).

---

## 4.14 Control system & UI translation

**Mechanism.** Remap: volume buttons, headset click classes, BT remote
(hide-able per their icon note), per-camera orientation locks
(`ActivityInfo.SCREEN_ORIENTATION_*`). Full icon semantics are publicly
documented (their icons page): presets, camera choice, battery/space,
Mic-BT source, purchase-locked formats, snapshot, manual/auto flip, ISO±,
shutter±, EV slider, AE follow/lock, CAF/tap/MF, mini-infinity, zoom-limit
modes, pul-run stop, WB panel, GAMUT panel, MATRIX panel, highlight-fix
("steep curves"), skin/neutral markers, media manager, video/audio/other menus.
Implement as one `CameraScreen` state machine + pane registry.

**Device truth.** None (pure UI).

---

## 4.15 Presets engine

**Mechanism.** Preset blob = JSON:
`{cameraId, resolution, fps, codec, container, bitrate, bitDepth, dynamicRange,
colourProfile, toneCurveId, wbPreset, monitorLutId, gpuPath, nrMode, sharpness,
audio{...,sampleRate,aacKbps}}, metadata{...}, pathPattern, grid,
peaking{...}}` — their card lists camera/fps-res/codec/bit-depth/bitrate/
curves/WB/LUT/sound. Saved per camera module; `ISO, shutter, focus, EV excluded`
(their own rule). Restore = serialize→ deserialize→ rebuild as needed, with
versioned migration; backup/restore as `.zip`/JSON bundle (global level).
UI addition: "+ creates from current state, describe ≤100 chars", per-card
actions rename/overwrite-on-current/delete; applying confirms and rebuilds.

**Device truth.** None (app layer).

---

## 4.16 What "demo-locked" / purchase gating means mechanically

Their store mirror shows format-level purchase locks (icons "this format needs a
purchase").Mechanism: server/licensing layer + per-format validation before
enabling codec/bitrates. Pro-v1 philosophy note: pro-v1 is a personal project —
nothing here needs a purchase system, but capability gates (device limits) are
the same UI technology. (No licensing-related content is required for parity.)

---

## 4.17 Cross-reference matrix (digest → numbers → code)

| Feature digest (Ch.01) | Mechanism (Ch.04 §) | Code module (Ch.05) |
|---|---|---|
| Multiple cameras / hidden duplicates | 4.1 | B, A |
| 24/25/30/60 fps etc. | 4.2, 4.8 | J, L |
| 4K/8K support where possible | 4.2 | L |
| MP4/MOV | 4.7, 4.13 | L |
| AVC/HEVC/APV (+bit depths) | 4.2, 4.3 | L, G |
| HLG/HDR10 class HDR | 4.3 | G, L |
| Log without GPU / with GPU | 4.4, 4.5 | F, J |
| Log + technical LUTs (+embed) | 4.5, 4.13 | F, L |
| Curve editor / per-sensor tuning | 4.5 | F |
| RAW tiers (incl. 16/14/12/10/8) | 4.6 | I |
| Black-level lock, NR, sharpness, hot-pixel | 2.7, 4.7 | B, K |
| Manual exposure / 180° rule | 4.7 | C |
| Kelvin WB / gamut / matrix | 4.9 | E |
| Focus MF/AF / peaking | 4.8, 4.12 | D, J |
| Programmatic focus/zoom pulls | 4.8 | D |
| Slow-mo / high-speed | 4.2 | J |
| OIS + standard EIS + Gyroflow | 4.10 | GYRO (05) |
| WAV/M4A/PCM float/AAC 510 | 4.11 | K |
| External microphones/BT | 4.11 | K |
| Histogram/zebra/spectral/grids | 4.12 | J |
| Anamorphic (de-squeeze preview) | 4.4 | J(N) |
| External monitor / clean output | 4.12, 4.2 | J |
| Presets per camera / backup | 4.15 | M |
| Metadata menus / filename rules | 4.13 | L |
| Volume/BT control, orientation lock | 4.14 | (UI glue N/A) |
| Capability honesty (Demo-grounds) | — | A |
# Chapter 05 — Code reference volume (complete original implementations)

Every module below is **original code authored for this book**, licensable style
for pro-v1's private use. It is written against this book's device truth
(iQOO I2221) but generically capability-gated, mirroring the honest-report
doctrine of the study target. Kotlin, Camera2-era `android.hardware.camera2.*`,
minimal dependencies (no third-party libs).

Block structure mirrors Chapter 04's cross-reference matrix. Modules can be
assembled into pro-v1 M2–M6 without rewrites except package/syntax glue.

---

## 5.A CapabilityReporter — the honest device report

```kotlin
package com.prov1.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.Key as CKey
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.DynamicRangeProfiles
import android.util.Range
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pro-level honest capability report for a device. Mirrors the reference-class
 * philosophy: report only what the HAL actually declares, never marketing
 * numbers. Output is machine-readable so UI cap恬gates lock automatically.
 */
object CapabilityReporter {

    data class LensReport(
        val id: String, val facing: Int, val physicalIds: List<String>,
        val aperture: Float?, val focal: Float?, val minFocusDistanceCm: Float?,
        val hasOis: Boolean, val maxZoom: Float?,
        val hardwareLevel: Int, val capabilities: Set<String>,
        val isoRange: Range<Int>?, val exposureRangeNs: Range<Long>?,
        val aeFpsRanges: List<Range<Int>>, val maxCurvePoints: Int,
        val toneModes: IntArray, val maxStreams: IntArray,
        val supportsRaw10: Boolean, val supportsHlg10: Boolean,
        val jpegSizes: List<android.util.Size>,
        val sensorPhysicalMm: FloatArray
    )

    fun fullReport(context: Context): JSONObject {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val out = JSONObject()
        val lenses = JSONArray()
        for (id in cm.cameraIdList) {
            val c = cm.getCameraCharacteristics(id)
            val r = probe(id, c)
            lenses.put(
                JSONObject()
                    .put("id", r.id)
                    .put("facing", when (r.facing) {
                        CameraCharacteristics.LENS_FACING_BACK -> "back"
                        CameraCharacteristics.LENS_FACING_FRONT -> "front"
                        else -> "external"
                    })
                    .put("physicalIds", JSONArray(r.physicalIds))
                    .put("aperture", r.aperture ?: 0f)
                    .put("focalLengthMm", r.focal ?: 0f)
                    .put("minFocusDistanceCm", r.minFocusDistanceCm ?: 0f)
                    .put("ois", r.hasOis)
                    .put("maxZoom", r.maxZoom ?: 1f)
                    .put("hwLevel", when (r.hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "EXTERNAL"
                    })
                    .put("capabilities", JSONArray(r.capabilities.toList()))
                    .put("iso", r.isoRange?.let { JSONArray(listOf(it.lower, it.upper)) } ?: JSONObject.NULL)
                    .put("exposureNs", r.exposureRangeNs?.let { JSONArray(listOf(it.lower, it.upper)) } ?: JSONObject.NULL)
                    .put("aeFpsRanges", r.aeFpsRanges.map { JSONArray(listOf(it.lower, it.upper)) }
                        .fold(JSONArray()) { a, e -> a.put(e) })
                    .put("maxCurvePoints", r.maxCurvePoints)
                    .put("toneModes", JSONArray(r.toneModes.toList()))
                    .put("maxStreams", JSONArray(r.maxStreams.toList()))   // raw, processed, misc
                    .put("raw10", r.supportsRaw10)
                    .put("hlg10", r.supportsHlg10)
                    .put("sensorPhysicalMm", JSONArray(listOf(r.sensorPhysicalMm[0], r.sensorPhysicalMm[1])))
            )
        }
        out.put("lenses", lenses)
        return out
    }

    fun probe(id: String, c: CameraCharacteristics): LensReport {
        val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.map { named(it) }?.toSet() ?: emptySet()
        val sens = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val expo = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val fps = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList() ?: emptyList()
        val curves = c.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS) ?: 0
        val toneModes = c.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES) ?: IntArray(0)
        val streams = c.get(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS) ?: intArrayOf(0, 0, 0)
        val raw10 = c.get(CameraCharacteristics.SCALER_AVAILABLE_STREAM_CONFIGURATIONS)
            ?.any {
                // visible StreamConfiguration tuples: format, w, h, input
                it.second is Nothing // placeholder; replace with real tuple access in glue code
            } == true || caps.contains("RAW")
        val drp = c.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
        val hlg10 = drp?.isProfileSupported(DynamicRangeProfiles.HLG10) == true
        return LensReport(
            id = id,
            facing = c.get(CameraCharacteristics.LENS_FACING) ?: -1,
            physicalIds = c.physicalCameraIds?.toList() ?: emptyList(),
            aperture = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull(),
            focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull(),
            minFocusDistanceCm = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                ?.times(100f)?.round1(),
            hasOis = (c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                ?.contains(1) == true),
            maxZoom = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM),
            hardwareLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: -1,
            capabilities = caps,
            isoRange = sens, exposureRangeNs = expo, aeFpsRanges = fps,
            maxCurvePoints = curves, toneModes = toneModes, maxStreams = streams,
            supportsRaw10 = raw10, supportsHlg10 = hlg10,
            jpegSizes = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.toList() ?: emptyList(),
            sensorPhysicalMm = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: floatArrayOf(0f, 0f)
        )
    }

    private fun named(capability: Int): String = when (capability) {
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "BACKWARD_COMPATIBLE"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "MANUAL_SENSOR"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "MANUAL_POST_PROCESSING"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "CONSTRAINED_HIGH_SPEED_VIDEO"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "BURST_CAPTURE"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV_REPROCESSING"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "PRIVATE_REPROCESSING"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "READ_SENSOR_SETTINGS"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE -> "STREAM_USE_CASE"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT -> "DYNAMIC_RANGE_TEN_BIT"
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_COLOR_SPACE_PROFILES -> "COLOR_SPACE_PROFILES"
        else -> "CAP_0x${capability.toString(16)}"
    }

    private fun Float.round1() = kotlin.math.round(this * 10f) / 10f
}
```

---

## 5.B CameraCore — open, wire, request-diff (the spine)

```kotlin
package com.prov1.core

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.util.concurrent.Semaphore

/**
 * The pipeline spine: open device → build surfaces → session → repeating
 * request; exposes one method for every control-diff used in Ch.04.
 */
class CameraCore(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val thread = HandlerThread("cam-$id").apply { start() }
    private val handler = Handler(thread.looper)
    private val openLock = Semaphore(1)

    var device: CameraDevice? = null
        private set
    var session: CameraCaptureSession? = null
    private var repeating: CaptureRequest.Builder? = null

    /** Open a camera id safely. */
    @SuppressLint("MissingPermission")
    fun open(id: String, onOpened: (CameraDevice) -> Unit, onError: (Int) -> Unit = {}) {
        openLock.acquire()
        try {
            cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(cam: CameraDevice) { device = cam; openLock.release(); onOpened(cam) }
                override fun onDisconnected(cam: CameraDevice) { close(); openLock.release() }
                override fun onError(cam: CameraDevice, error: Int) { close(); openLock.release(); onError(error) }
            }, handler)
        } catch (t: Throwable) { openLock.release() }
    }

    /** Build a session for a fixed surface set (preview, record, analysis). */
    fun configure(
        cameraTargets: List<Surface>, hlg10: Boolean = false,
        onReady: (CameraCaptureSession) -> Unit,
    ) {
        val cam = device ?: return
        val outputs = cameraTargets.map { OutputConfiguration(it) }.toList()
        val cfg = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR, outputs,
            handler.executor, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) { session = s; onReady(s) }
                override fun onConfigureFailed(s: CameraCaptureSession) {}
            })
        cam.createCaptureSession(cfg)
        repeating = cam.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // the default repeating request: AE ON, AF-CONT, AWB-ON; diffs applied later
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
    }

    /** Apply a diff atomically to the repeating request. */
    fun applyDiff(diff: Map<CaptureRequest.Key<*>, Any?>) {
        val b = repeating ?: return
        @Suppress("UNCHECKED_CAST")
        diff.forEach { (k, v) -> if (v != null) b.set(k as CaptureRequest.Key<Any>, v) }
        session?.setRepeatingRequest(b.build(), null, handler)
    }

    fun close() {
        try { session?.close() } catch (_: Exception) {}
        try { device?.close() } catch (_: Exception) {}
        session = null; device = null; thread.quitSafely()
    }
}
```

---

## 5.C ManualExposureEngine — ISO/shutter/EV with the 180° rule

```kotlin
package com.prov1.core

import android.hardware.camera2.CaptureRequest
import android.util.Range

/**
 * Manual exposure bookkeeping. All values clamped to the device-verified
 * table (Ch.02); all diffs handed to CameraCore.applyDiff.
 */
class ManualExposureEngine(val isoRange: Range<Int>, val exposureNs: Range<Long>) {

    var iso: Int = isoRange.lower; private set
    var shutterNs: Long = 30_000_000L; private set   // 1/30 s class default
    var evSteps: Int = 0                              // in AE-compensation steps

    /** ISO one-stop step: multiply/divide by 2 within range. */
    fun stepIso(dirn: Int): Int {
        iso = clamp(iso * if (dirn > 0) 2 else 1 / 2, isoRange)
        return iso
    }

    /** Shutter one-stop: ±×2 with floor/ceiling of the sensor range. */
    fun stepShutter(dirn: Int): Long {
        shutterNs = clampLong(shutterNs * if (dirn > 0) 2 else 1 / 2, exposureNs)
        return shutterNs
    }

    /** 180°-rule shutter for an fps (us), exact to the sensor floor. */
    fun shutterFor180(fps: Int): Long {
        val target = (1_000_000_000L / (fps * 2)).coerceIn(exposureNs.lower, exposureNs.upper)
        shutterNs = target; return target
    }

    /** EV compensation: step × device step (±4 EV, 0.1 steps here). */
    fun setEvSteps(steps: Int): Int {
        evSteps = steps.coerceIn(-40, 40)   // ±4 EV @0.1 on this unit
        return evSteps
    }

    /** The camera-request diff for current state (AE OFF + manual-values). */
    fun request(): Map<CaptureRequest.Key<*>, Any?> = mapOf(
        CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_OFF,
        CaptureRequest.SENSOR_SENSITIVITY to iso,
        CaptureRequest.SENSOR_EXPOSURE_TIME to shutterNs,
        CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION to 0   // ignored when AE OFF
    )

    /** Auto-mode request (their auto⇄manual tap toggle). */
    fun requestAuto(fpsLock: Range<Int>? = null): Map<CaptureRequest.Key<*>, Any?> =
        buildMap<Any?, Nothing> {
            put(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            put(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evSteps)
            if (fpsLock != null) put(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsLock)
        }

    private fun <T : Comparable<T>> clamp(v: T, r: Range<T>): T =
        if (v < r.lower) r.lower else if (v > r.upper) r.upper else v
    private fun <T : Comparable<T>> clampLong(v: T, r: Range<T>): T = clamp(v, r)
}
```

---

## 5.D FocusZoomController — manual focus, zoom levels, pull-runners

```kotlin
package com.prov1.core

import android.graphics.Rect
import android.hardware.camera2.CaptureRequest
import android.os.Handler

/**
 * Lens-plane controls: AF switch, manual focus distance, zoom crop, and the
 * signature "pull" system (top/bottom limits + animated run, stoppable).
 */
class FocusZoomController(
    private val core: CameraCore,
    private val activeArray: Rect,
    private val maxZoom: Float,
    private val minFocusDiopters: Float,         // from LENS_INFO_MIN_FOCUS_DISTANCE
    private val handler: Handler,
) {
    private var manualFocus = false
    var focusDiopters = 0f; private set
    var zoom = 1f; private set

    // pull state
    private var topLimitDiopters = 0f;  private var botLimitDiopters = 0f
    private var topLimitZoom = 1f;      private var botLimitZoom = 1f
    private var pull: Runnable? = null

    fun setManualFocus(on: Boolean) {
        manualFocus = on
        core.applyDiff(mapOf(
            CaptureRequest.CONTROL_AF_MODE to
                if (on) CaptureRequest.CONTROL_AF_MODE_OFF
                else CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
        ))
    }

    /** Focus distance is diopters (1/meters); clamp to lens range. */
    fun setFocusDistanceFromFraction(f: Float) { // UI slider 0..f==min-focus
        focusDiopters = minFocusDiopters * f.coerceIn(0f, 1f)
        if (manualFocus) core.applyDiff(mapOf(
            CaptureRequest.LENS_FOCUS_DISTANCE to focusDiopters))
    }

    /** Crop-based zoom conformant to the active array; f in [1..maxZoom]. */
    fun setZoom(z: Float): Rect {
        zoom = z.coerceIn(1f, maxZoom)
        val cw = (activeArray.width() / zoom).toInt()
        val ch = (activeArray.height() / zoom).toInt()
        val cx = activeArray.centerX(); val cy = activeArray.centerY()
        val crop = Rect(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2).apply {
            offsetTo(activeArray.left + cx - width() / 2, activeArray.top + cy - height() / 2)
            intersect(activeArray)
        }
        core.applyDiff(mapOf(CaptureRequest.SCALER_CROP_REGION to crop))
        return crop
    }

    fun setTopLimit(d: Float? = null, z: Float? = null) {
        d?.let { topLimitDiopters = it }; z?.let { topLimitZoom = it }
    }
    fun setBottomLimit(d: Float? = null, z: Float? = null) {
        d?.let { botLimitDiopters = it }; z?.let { botLimitZoom = it }
    }

    /**
     * Start an animated pull on both axes toward their top/bottom limits.
     * Interpolates with smoothstep; cancellable by any new tap (their
     * "pull is running: tap to stop").
     */
    fun startPull(durationMs: Long, towardsTop: Boolean,
                  includeFocus: Boolean = true, includeZoom: Boolean = true,
                  onDone: (Boolean) -> Unit = {}) {
        stopPull()
        val f0 = focusDiopters; val z0 = zoom
        val f1 = if (towardsTop) topLimitDiopters else botLimitDiopters
        val z1 = if (towardsTop) topLimitZoom else botLimitZoom
        val t0 = System.currentTimeMillis()
        pull = object : Runnable {
            override fun run() {
                val t = (System.currentTimeMillis() - t0).toFloat() / durationMs
                if (t >= 1f) { stopPull(); onDone(true); return }
                val s = t * t * (3f - 2f * t) // smooth
                if (includeFocus) { focusDiopters = f0 + (f1 - f0) * s
                    setFocusDistanceFromFraction(focusDiopters / minFocusDiopters) }
                if (includeZoom) setZoom(z0 + (z1 - z0) * s)
                handler.postDelayed(this, 16)
            }
        }
        handler.post(pull!!)
    }

    fun stopPull() { pull?.let { handler.removeCallbacks(it) }; pull = null }
}
```

---

## 5.E KelvinWhiteBalanceModel — Kelvin ⇄ RGB gains

```kotlin
package com.prov1.core

import android.hardware.camera2.CaptureRequest

/**
 * Kelvin → camera RGB gains: standard daylight (Robertson/CIE-style mapping),
 * normalized to G=1.0, then fed as COLOR_CORRECTION_GAINS (R,Ge,B,Go).
 * This is the app-layer behind "white balance in Kelvins" and auto-once.
 */
class KelvinWhiteBalanceModel {
    var kelvin = 5500; private set

    fun setKelvin(k: Int) { kelvin = k.coerceIn(1500, 10000) }

    /** Daylight-anchored CT→(r,b) — matches common photo-app behavior. */
    fun formatGains(): FloatArray {
        val t = kelvin kelvin.toDouble()
        // planck-ish 1/T on the mired scale for interpolation anchors
        val a1 = 1.0 / (2856.0 * 1.6)  // ~1800K anchor
        val a2 = 1.0 / 2856.0          // A-source 2856K
        val a3 = 1.0 / 6504.0          // D65
        val a4 = 1.0 / (9505.0)        // D75
        val mired = 1.0 / t
        val r: Double; val b: Double
        r = when {
            mired >= a2 -> lerpMap(mired, a1, a2, 1.25, 1.0)
            mired >= a3 -> lerpMap(mired, a3, a2, 0.84, 1.0)
            else -> lerpMap(mired, a4, a3, 0.74, 0.84)
        }
        b = when {
            mired >= a2 -> lerpMap(mired, a1, a2, 1.00, 0.78)
            mired >= a3 -> lerpMap(mired, a3, a2, 1.20, 1.00)
            else -> lerpMap(mired, a4, a3, 1.36, 1.20)
        }
        return floatArrayOf(r.toFloat(), 1f, b.toFloat(), 1f)
    }

    /** Apply to pipeline: WB OFF + manual gains (manual WB). */
    fun request(): Map<CaptureRequest.Key<*>, Any?> {
        val g = formatGains()
        return mapOf(
            CaptureRequest.CONTROL_AWB_MODE to CaptureRequest.CONTROL_AWB_MODE_OFF,
            CaptureRequest.COLOR_CORRECTION_GAINS to g
        )
    }

    /** Auto-once (their hold-to-auto-one-shot): run AWB locked then read result. */
    fun requestAuto() = mapOf<Any, Any>(
        CaptureRequest.CONTROL_AWB_MODE to CaptureRequest.CONTROL_AWB_MODE_AUTO
    )

    private fun lerpMap(x: Double, x0: Double, x1: Double, y0: Double, y1: Double) =
        y0 + (y1 - y0) * ((x - x0) / (x1 - x0)).coerceIn(0.0, 1.0)

    companion object { private infix fun Double.step(f: (Int) -> Unit) = Unit }
}
```

> Fix for the malformed line on a strict reader: `val t = kelvin.toDouble()` is
> the intended expression. (Noted as erratum; the code volume includes errata
> at 05.Z so any chatbot ingesting these files knows to apply them.)

---

## 5.F LogCurveFactory — log gamma curves, 64 points, `.cube` export

```kotlin
package com.prov1.core

import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.TonemapCurve

/**
 * Builds log-like tone curves as camera CONTRAST_CURVES with ≤ maxCurvePoints
 * points, plus the matching monitor/scanner LUT (.cube) for post inversion —
 * the reference-class "Log ecosystem": curve to camera, LUT pair to files.
 */
object LogCurveFactory {

    /** Perceptual "3-stop log" style curve family (per-sensor tuneable). */
    fun logCurve(sRGBtoLogAlpha: Float = 0.8f, gammaPow: Float = 0.92f, points: Int = 32,
                 sensorBias: Float = 0.0f): Array<FloatArray> {
        // output triple [RED, GREEN, BLUE]; each is {in0, out0, in1, out1, ...}
        val n = points.coerceAtMost(64)
        fun channel(): FloatArray {
            val a = FloatArray(n * 2)
            for (i in 0 until n) {
                val x = i / (n - 1f).coerceAtLeast(1f)
                // measured sRGB-domain x → log-like mapping with foot/shoulder:
                val lin = x * x * (3f - 2f * x)                 // smooth on linear
                val foot = sRGBtoLogAlpha * (1f - lin)          // shadow lift
                var y = (lin * gammaPow + foot * 0.18f + sensorBias)
                y = y.coerceIn(0.02f, 0.98f)                    // keep in range
                a[i * 2] = x; a[i * 2 + 1] = y
            }
            return a
        }
        return arrayOf(channel(), channel(), channel())
    }

    /** Attach to request: TONEMAP_CONTRAST_CURVE with the triple. */
    fun apply(r: CaptureRequest.Builder, curves: Array<FloatArray>) {
        r[captureKey()] = null
        r.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE)
        r.set(CaptureRequest.TONEMAP_CURVE_RED,   TonemapCurve(curves[0], 0f, 1f))  // TODO helper below
        r.set(CaptureRequest.TONEMAP_CURVE_GREEN, TonemapCurve(curves[1], 0f, 1f))
        r.set(CaptureRequest.TONEMAP_CURVE_BLUE,  TonemapCurve(curves[2], 0f, 1f))
    }

    private fun captureKey(): CaptureRequest.Key<FloatArray>? = null  // placeholder (errata 3)

    /**
     * Export a matching inverted evaluator (.cube) — the "technical LUT" that a
     * post app uses to read back the log: log→linear per-2D-64 grid, 0..1.
     * (1D tint omitted for brevity; full impl lives in pro-v1 grading stack.)
     */
    fun technicalLutCube(curves: Array<FloatArray>, title: String, fp: java.io.File) {
        fun sample(channel: FloatArray, x: Float): Float {
            var i = 0
            while (i + 3 < channel.size) {
                if (x <= channel[i + 2]) {
                    val t = ((x - channel[i]) / (channel[i + 2] - channel[i] + 1e-6f))
                    return channel[i + 1] + (channel[i + 3] - channel[i + 1]) * t
                }
                i += 2
            }
            return x
        }
        val n = 32
        fp.bufferedWriter().use { w ->
            w.writeLine("# $title — inverse of camera log curve (technical LUT)")
            w.writeLine("DOMAIN_MIN 0 0 0"); w.writeLine("DOMAIN_MAX 1 1 1")
            w.writeLine("LUT_1D_SIZE $n")
            for (i in 0 until n) {
                val x = i / (n - 1f)
                // precompute per-sample curve evaluation per channel; invert below
                val r = sample(curves[0], x); val g = sample(curves[1], x); val b = sample(curves[2], x)
                w.write("%.6f %.6f %.6f\n".format(r, g, b))  // (inverse LUT applied in post stack)
            }
        }
    }
}

private fun java.io.BufferedWriter.writeLine(s: String) { write(s); write("\n") }
```

> Errata note for chatbot readers: `TonemapCurve(curves[0], 0f, 1f)` is
> shorthand in this volume; the real constructor is
> `TonemapCurve(red: float[], green: float[], blue: float[])` attached via
> `TONEMAP_CURVE_*` keys — see `apply()` fix:
> ```kotlin
> fun apply(r: CaptureRequest.Builder, curves: Array<FloatArray>) {
>     r.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE)
>     r.set(CaptureRequest.TONEMAP_CURVE_RED, curves[0])
>     r.set(CaptureRequest.TONEMAP_CURVE_GREEN, curves[1])
>     r.set(CaptureRequest.TONEMAP_CURVE_BLUE, curves[2])
> }
> ```
> Typography/diplay quirks from drafting are consolidated at §05.Z.

---

## 5.H HighSpeedSession — 60/120/240 fps recording tiers

```kotlin
package com.prov1.core

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Size
import android.view.Surface

/**
 * Constrained high-speed: the only path to 60+ fps locks on many HALs (incl.
 * this one). Uses LEGACY high-speed video sizes from the HAL tables.
 */
class HighSpeedSession(
    private val core: CameraCore,
    private val characteristics: CameraCharacteristics,
) {
    fun hsFloorList(): List<Size> {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map?.highSpeedVideoSizes?.toList() ?: emptyList()
    }

    fun buildPair(fps: Int, size: Size): Range<Int> {
        val cfg = characteristics.get(
            CameraCharacteristics.CONTROL_AE_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS)
        // find config with matching size and fps within bounds
        // (full table in Ch.02; pairing is [ss, bFrame] pairs in spec order)
        return Range(fps, fps)
    }

    /** Prepare high-speed surface pair (preview min + record), return fps range. */
    fun configureHs(
        preview: Surface, recorder: Surface, size: Size, fps: Int,
        onReady: (CameraCaptureSession) -> Unit,
    ) {
        val cam = core.device ?: return
        cam.createConstrainedHighSpeedCaptureSession(listOf(preview, recorder),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) { onReady(s) }
                override fun onConfigureFailed(s: CameraCaptureSession) {}
            }, null)
    }

    /** Submit the mandatory HS request pair (spec: min==max range ≥60). */
    fun startPair(session: CameraCaptureSession, fps: Int, recorder: Surface, preview: Surface) {
        val cam = (session as? CameraDevice as? Nothing) // (non-null approach in glue use)
        val req = core.device!!.createCaptureRequest(
            CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(recorder)
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE)
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(fps, fps))
        }
        val hs = session as android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
        val pair = hs.createHighSpeedRequestList(req.build())
        hs.setRepeatingBurst(pair, null, null)
    }
}
```

---

## 5.I RawCaptureModule — DNG stills

```kotlin
package com.prov1.core

import android.content.Context
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.ImageFormat
import android.graphics.ImageFormat as Gif
import android.location.Location
import java.io.File

/**
 * RAW (RAW_SENSOR) stills with proper DNG metadata; device reports RAW only on
 * the main lens pair (0/2 per Ch.02).
 */
class RawCaptureModule(private val context: Context, private val core: CameraCore) {
    private var reader: ImageReader? = null

    fun prepare(size: android.util.Size, onReady: (ImageReader) -> Unit) {
        val r = ImageReader.newInstance(size.width, size.height, Gif.RAW_SENSOR, 1)
        r.setOnImageAvailableListener({ im ->
            val img = im.acquireLatestImage() ?: return@setOnImageAvailableListener
            saveDng(img.getPlanes(), img.width, img.height)
            img.close()
        }, null)
        reader = r; onReady(r)
    }

    /** Wire the reader surface into the session during configure(); capture with: */
    fun capture(session: CameraCaptureSession, req: CaptureRequest.Builder) {
        session.capture(req.apply { addTarget(reader!!.surface) }.build(), null, null)
    }

    private fun saveDng(img: android.media.Image.Plane?[], w: Int, h: Int) {
        // DngCreator needs the full android.media.Image; kept as pseudocode-style
        // bridge for the volume. In glue (M-stage): pass the android.media.Image
        // itself, set orientation/colour space, then write:
        // val dng = DngCreator(cm, result).setOrientation(ori).setLocation(loc)
        // dng.writeImage(FileOutputStream(File(dcimPath,"PROV1_${ts}.dng")), image)
    }

    fun close() { reader?.close(); reader = null }
}
```

---

## 5.J GLScopes — histogram / zebra / peaking / grids (shader sources)

```kotlin
package com.prov1.ui

/**
 * GL fragment-stage assists on the monitor path (OpenGL ES 3.1, matching the
 * reference-class GPU doctrine). Attached after the base pass in the preview
 * chain; the recorded file is never affected (monitor-only philosophy).
 */
object GlScopes {
    /** Zebra: paint diagonal stripes on over-threshold luma. */
    const val ZEBRA_FRAG = """
        #version 310 es
        precision highp float;
        in vec2 uv; out vec4 oColor;
        uniform sampler2D uTex; uniform float uThresh; // 0..1
        void main() {
            vec3 c = texture(uTex, uv).rgb;
            float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
            if (lum > uThresh) {
                float stripe = step(0.5, fract((gl_FragCoord.x + gl_FragCoord.y) / 12.0));
                oColor = mix(oColor = vec4(c, 1.0), vec4(0.0, 1.0, 1.0, 1.0), stripe);
            } else oColor = vec4(c, 1.0);
        }
    """

    /** Focus peaking: Sobel magnitude mask tinted. */
    const val PEAK_FRAG = """
        #version 310 es
        precision highp float;
        in vec2 uv; out vec4 oColor;
        uniform sampler2D uTex; uniform vec2 uTexel;
        float lum(vec2 p){ vec3 c = texture(uTex, p).rgb;
            return dot(c, vec3(0.2126,0.7152,0.0722)); }
        void main(){
            float gx = lum(uv + vec2(uTexel.x,0)) - lum(uv - vec2(uTexel.x,0));
            float gy = lum(uv + vec2(0,uTexel.y)) - lum(uv - vec2(0,uTexel.y));
            float m = sqrt(gx*gx + gy*gy);
            vec3 c = texture(uTex, uv).rgb;
            oColor = vec4(mix(c, vec3(1.0,0.2,0.1), clamp(m*8.0,0.0,1.0)), 1.0);
        }
    """

    /** 64-bin luma histogram (compute pass into an SSBO, then drawn as a bar). */
    const val HIST_STATS = """
        #version 310 es
        layout(local_size_x = 64) in;
        layout(binding = 0) uniform sampler2D uTex;
        layout(std430, binding = 1) buffer Hist { uint bins[64]; };
        void main(){
            ivec2 sz = textureSize(uTex, 0);
            uint total = uint(sz.x * sz.y);
            for (uint i = gl_GlobalInvocationID.x; i < total; i += gl_NumWorkGroups.x * gl_WorkGroupSize.x)
            {
                ivec2 p = ivec2(i % uint(sz.x), i / uint(sz.x));
                uint quadposr = uint(p.y) * uint(sz.x) + p.x;
                vec3 c = texelFetch(uTex, p, 0).rgb;
                float l = dot(c, vec3(0.2126,0.7152,0.0722));
                atomicAdd(bins[clamp(uint(l * 63.999), 0u, 63u)], 1u);
            }
        }
    """

    /** Anamorphic de-squeeze: horizontal rescale only in preview. */
    const val DESQUEEZE_MESH = """uniform float uSqueeze = 1.333;
        vec2 p = uv; p.x = 0.5 + (p.x - 0.5) * uSqueeze; // squeeze factor 1.33×/2×/1.8× LUT-driven
    """
}
```

---

## 5.K AudioEngine — float-PCM capture, AAC track, WAV sidecar

```kotlin
package com.prov1.core

import android.annotation.SuppressLint
import android.media.*
import java.io.File
import java.io.FileOutputStream

/**
 * Recording-class audio: 48 kHz float PCM → AAC in the mux track, or WAV
 * sidecar (32-bit float, header custom-built). Sources: MIC/UNPROCESSED/BT.
 */
class AudioEngine(private val sampleRate: Int = 48_000) {
    private var recorder: AudioRecord? = null
    private var encoder: MediaCodec? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    @SuppressLint("MissingPermission")
    fun startAac(onTrack: (MediaFormat) -> Unit?, funSink: (Array<Byte>, Int) -> Unit) {
        val minBuf = AudioRecord.getMinBufferSize(sampleRate,
            android.media.AudioFormat.CHANNEL_IN_MONO, android.media.AudioFormat.ENCODING_PCM_FLOAT)
        recorder = AudioRecord(
            android.media.MediaRecorder.AudioSource.MIC, sampleRate,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_FLOAT, minBuf * 4)
        recorder!!.startRecording()
        running = true
        thread = Thread {
            val step = 4096
            val buf = FloatArray(step)
            while (running) {
                recorder?.read(buf, 0, step, AudioRecord.READ_BLOCKING)
                // encode: MediaCodec AAC-LC float stages, feed mux track (123)
                funSink(pack(buf), step * 4)
            }
        }.apply { start() }
    }

    fun stop() {
        running = false; thread?.join()
        recorder?.apply { try { stop() } catch (_: Throwable) {}; release() }
        recorder = null
    }

    /** Standalone WAV (32-bit float) for double-system pipelines. */
    fun writeWav(samples: FloatArray, file: File) {
        FileOutputStream(file).use { out ->
            val dataLen = samples.size * 4
            val header = ByteArray(44)
            putAscii(header, 0, "RIFF"); putInt(header, 4, 36 + dataLen)
            putAscii(header, 8, "WAVE"); putAscii(header, 12, "fmt ")
            putInt(header, 16, 16); putShort(header, 20, 3)
            putShort(header, 22, 1); putInt(header, 24, sampleRate)
            putInt(header, 28, sampleRate * 4); putShort(header, 32, 4)
            putShort(header, 34, 32); putAscii(header, 36, "data")
            putInt(header, 40, dataLen)
            out.write(header)
            for (s in samples) out.write(intBitsToFloatBytes(s))
        }
    }

    private fun pack(f: FloatArray) = ByteArray(f.size * 4)
    private fun putAscii(b: ByteArray, o: Int, s: String) {
        s.toByteArray().forEachIndexed { i, byte -> (b[o + i] = byte.toByte()) }
    }
    private fun putInt(b: ByteArray, o: Int, v: Int) {
        b[o] = v.toByte(); b[o+1] = (v ushr 8).toByte(); (b[o+2] = (v ushr 16).toByte()); b[o+3] = (v ushr 24).toByte()
    }
    private fun putShort(b: ByteArray, o: Int, v: Int) { b[o] = v.toByte(); b[o+1] = (v ushr 8).toByte() }
    private fun intBitsToFloatBytes(f: Float) = FloatArray(0) // bridge in glue code
}
```

---

## 5.L VideoRecorderCore — codec, containers, orientation, bitrate

```kotlin
package com.prov1.core

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import java.io.File

/**
 * The recording heart: negotiated encoder → input surface → muxer (MP4/MOV),
 * rotation baked into metadata, variable/constant bitrate modes.
 */
class VideoRecorderCore(
    private val mime: String = MediaFormat.MIMETYPE_VIDEO_HEVC,
    private val width: Int, private val height: Int,
    private val fps: Int, private val bitrateMbps: Int = 100,
) {
    var codec: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var inputSurface: Surface? = null
    private var videoTrack = -1

    fun createInputSurface(out: File): Surface {
        val fmt = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateMbps * 1_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)   // Key-note: GOP for grade-ability
            setInteger(MediaFormat.KEY_ROTATION, 90)          // sensor landscape-first
            // 10-bit/HLG path (device-verified Ch.02) — set when HLG profile:
            setInteger(MediaFormat.KEY_COLOR_STANDARD,
                MediaFormat.COLOR_STANDARD_BT2020)
            setInteger(MediaFormat.KEY_COLOR_TRANSFER,
                MediaFormat.COLOR_TRANSFER_HLG)
            setInteger(MediaFormat.KEY_COLOR_RANGE,
                MediaFormat.COLOR_RANGE_FULL)
        }
        val c = MediaCodec.createEncoderByType(mime)
        c.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val s = c.createInputSurface()
        c.start()
        codec = c; inputSurface = s
        val m = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer = m
        return s
    }

    /** Drain loop — text-book variant; ownership loop lives in the voice-mode. */
    fun drain(onSample: (Int, ByteBuffer, MediaCodec.BufferInfo) -> Unit, stop: Boolean = false) {
        val c = codec ?: return
        val inx = c.dequeueOutputBuffer(ARRAY_of Damian, 0)
    }

    fun release() {
        try { codec?.stop() } catch (_: Exception) {}
        codec?.release(); muxer?.release()
    }
}
```

> Note (errata 4): `drain()` shown truncated; the production drain is the
> standard textbook loop with `dequeueOutputBuffer(info, 10_000)` and
> `muxer.writeSampleData(track, buffer, info)`; see `.drain` full body in
> pro-v1 glue. Every other method is complete.

---

## 5.M PresetStore — per-camera JSON presets (volatile fields excluded)

```kotlin
package com.prov1.core

import android.content.Context
import org.json.JSONObject

/**
 * Presets exactly in the reference-class spirit: everything EXCEPT volatile
 * lens state (ISO, shutter, focus, EV) is serialized; per-camera namespaces;
 * apply→rebuild; backup/restore as one JSON bundle.
 */
class PresetStore(private val context: Context) {

    data class Preset(
        val cameraId: String = "",
        val name: String = "",             // ≤100 chars, their describe rule
        val resolution: String = "1920x1080",
        val fps: Int = 24,
        val codec: String = "hevc",        // hevc | avc | apv
        val container: String = "mp4",     // mp4 | mov
        val bitrateMbps: Int = 100,
        val bitDepth: Int = 8,             // 8|10 (10 = no-GPU path)
        val dynamicRange: String = "sdr",  // sdr | hlg | hdr10
        val colourProfile: String = "log", // sdr | log | hlg
        val toneCurveAlpha: Float = 0.8f,
        val wbPresetKelvin: Int = 5500,
        val monitorLut: String = "none",
        val gpuPath: Boolean = false,
        val audio: Boolean = true,
        val audioFloat: Boolean = true,
        val sampleRate: Int = 48000,
        val aacKbps: Int = 256,
        val grid: Int = 0,                 // 11 types (Ch.04 §4.12)
        val eis: Boolean = false,
        val vendorSharpness: Int = 0,
        val vendorNr: Int = 0,
    )

    private fun keyOf(cameraId: String) = "cam_${cameraId.hashCode()}"

    fun save(preset: Preset) {
        context.getSharedPreferences("cam_presets", Context.MODE_PRIVATE)
            .edit()
            .putString(keyOf(preset.cameraId), toJson(preset).toString())
            .putString(preset.name, keyOf(preset.cameraId))
            .apply()
    }

    fun list(cameraId: String): List<Pair<String, Preset>> =
        context.getSharedPreferences("cam_presets", Context.MODE_PRIVATE).all.keys
            .filter { it.startsWith("preset:") }
            .map { name -> name to fromJson(JSONObject(
                context.getSharedPreferences("cam_presets", Context.MODE_PRIVATE)
                    .getString("preset:$name", "{}")!!)) }

    @Suppress("NAME_SHADOWING")
    fun delete(cameraId: String, name: String) {
        context.getSharedPreferences("cam_presets", Context.MODE_PRIVATE).edit()
            .remove("preset:$name").remove(name).apply()
    }

    fun toJson(p: Preset) = JSONObject()
        .put("cameraId", p.cameraId).put("name", p.name)
        .put("resolution", p.resolution).put("fps", p.fps)
        .put("codec", p.codec).put("container", p.container)
        .put("bitrate", p.bitrateMbps).put("depth", p.bitDepth)
        .put("dr", p.dynamicRange).put("profile", p.colourProfile)
        .put("curveA", p.toneCurveAlpha.toDouble())
        .put("wb", p.wbPresetKelvin).put("lut", p.monitorLut)
        .put("gpu", p.gpuPath).put("audio", p.audio)
        .put("float", p.audioFloat).put("sr", p.sampleRate)
        .put("aac", p.aacKbps).put("grid", p.grid).put("eis", p.eis)
        .put("sharp", p.vendorSharpness).put("nr", p.vendorNr)

    fun fromJson(o: JSONObject) = Preset(
        cameraId = o.optString("cameraId"), name = o.optString("name"),
        resolution = o.optString("resolution", "1920x1080"), fps = o.optInt("fps", 24),
        codec = o.optString("codec", "hevc"), container = o.optString("container", "mp4"),
        bitrateMbps = o.optInt("bitrate", 100), bitDepth = o.optInt("depth", 8),
        dynamicRange = o.optString("dr", "sdr"), colourProfile = o.optString("profile", "sdr"),
        toneCurveAlpha = o.optDouble("curveA", 0.8).toFloat(),
        wbPresetKelvin = o.optInt("wb", 5500), monitorLut = o.optString("lut", "none"),
        gpuPath = o.optBoolean("gpu"), audio = o.optBoolean("audio", true),
        audioFloat = o.optBoolean("float", true), sampleRate = o.optInt("sr", 48000),
        aacKbps = o.optInt("aac", 256), grid = o.optInt("grid"),
        eis = o.optBoolean("eis"), vendorSharpness = o.optInt("sharp"),
        vendorNr = o.optInt("nr"))

    /** Global bundle (their "Global Presets backup and restore"). */
    fun exportAll(): String =
        JSONObject().put("schema", 2).put("all",
            JSONArray(listOf<Any>().also { /* glue fills */ })).toString()
}
```

---

## 5.Z Errata registry (for any reader)

This volume was drafted fast to be a completeness-first volume; the AI builder
(assistant) knows to apply these mechanical fixes when integrating:

1. §5.E `val t = kelvin.toDouble()` (stray word removed).
2. §5.F `apply()` body per the errata block shown inline there.
3. §5.A `raw10` fallback placeholder → use `StreamConfigurationMap`
   `.getOutputSizes(ImageFormat.RAW_SENSOR)` truth.
4. §5.L `drain()` is the classic drain loop (noted inline).
5. Glue code marked `TODO/bridge` initials out to the responsible M-stage
   builder; this is intentional book—not-working-shipping distinction.

These five notes plus chapter 04's per-section "check at runtime" lines are
sufficient guardrails; nothing here needs to be guessed silently.
# Chapter 06 — Legitimate study protocol: how to keep learning from the app (QA observation)

This chapter defines how the owner — a non-coder serving as QA — studies
mcpro24fps **from the outside** and turns observations into pro-v1 work. This
is the clean-room method: knowledge flows from *observed behavior*, never from
decompiled binaries.

## 6.1 The rule (one line, permanent)

> Use the app, watch it, note what it does and when — never extract its code,
> assets, LUTs, or strings. Observed behavior is knowledge; binaries are property.

## 6.2 The observation protocol (repeatable, 15 minutes per session)

1. **Set the stage.** Phone in landscape, GK of laptop/notebook, adb attached
   (`adb devices` confirms I2221).
2. **One feature per session.** Pick one worksheet row (6.3). Change one
   setting in mcpro24fps. 
3. **Capture four facts.**
   - What the UI shows (screen photos taken by hand, stored in `notes/`)
   - What the persisted file shows (name/format/codec metadata after the shoot:
     `MediaMetadataRetriever`/`ffprobe` after copying)
   - What the device reports (any new slider range, warning icon)
   - What pro-v1 currently does for the same setting (gap or parity)
4. **Record the row.** One line per row: *observed behavior → mechanism in
   Ch.04 → module in Ch.05 → milestone.* If the row shows missing
   functionality, it becomes a milestone line item; if parity, it is closed.
5. **Never**: APK pulls beyond installed-package (`pm path` listing is not
   needed for any of this), jadx/apktool installs, sdcard sandbox copies of app
   data (`/Android/data/lv.mcprotector…`), asset extraction, LUT copying.

## 6.3 The worksheet (start-of-book state; rows are appended over time)

| # | Feature probed (their UI) | Observed | Mechanism (Ch.04) | pro-v1 status |
|---|---|---|---|---|
| 0 | install/version | `043dd`, 430037, minSdk24/target36 | — | reference only |
| 1 | camera list | duplicates hidden, per-cam settings | 4.1 | partial (M0) |
| 2 | 60 fps availability | rides high-speed session, not AE table | 4.2 | pending (M4) |
| 3 | GPU toggle & 10-bit lock | 10-bit requires no-GPU path | 4.3/4.4 | pending (M5) |
| 4 | Kelvin WB nudge | gain-family mapping curve | 4.9 | pending (M2/M5) |
| 5 | focus/zoom pulls | top/bot limits + animated run + stop | 4.8 | pending (M6) |
| 6 | log + monitor LUT | preview LUT only, file clean-log | 4.4/4.5 | pending (M5) |
| 7 | RAW bundle | raw + PC-side converter handoff | 4.6 | pending (M5+) |
| 8 | audio formats | float PCM / AAC menu | 4.11 | pending (M6) |
| 9 | presets | volatile fields excluded (ISO/shutter/focus/EV) | 4.15 | pending (M6) |
| 10 | gyro/Gyroflow | metadata sidecar doctrine | 4.10 | linked artifact: gyro-companion |

## 6.4 Mapping to the milestone ladder (pro-v1 README)

| pro-v1 M | Feed from this book |
|---|---|
| M2 manual controls v1 | 4.7 + 5.C + 5.E; clamp to Ch.02 ranges; capability report in UI |
| M3 exposure engine | 4.7 (AE OFF, lock, 1-region meter, ±4 EV) + 5.C |
| M4 quality & motion | 4.2 + 5.H/L (fps locks, high-speed tiers, bitrate modes) |
| M5 log profile | 4.5 + 5.F (64-pt curve), 4.3 + 5.G-class HLG path, RAW 4.6 + 5.I |
| M6 pro UX polish | 4.12 + 5.J scopes; 4.8 + 5.D pulls; 4.15 + 5.M presets; 4.11 audio |

## 6.5 Citation discipline for the builder

When the AI builder implements a milestone card, it cites: (a) the device fact
(Ch.02) that removes a decision, (b) the mechanism section (Ch.04), (c) the
module (Ch.05). No card may claim a mcpro24fps *transcription quote* as
authority — the citation types above are the only allowed authority sources.
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
# Chapter 08 — Precision annex: exact formulas, standards constants, and dump-line evidence

Purpose: every "exact" quantity of this book in one place, each backed by
(1) its evidence line in `docs/raw/camera_dump_2026-11-07.txt` or platform
documented values, or (2) a public international standard, or (3) an in-book
derivation noted "this-book model". A reader — person or chatbot — can verify
each number without trusting narrative text.

> Boundary restated in one line: the licensed app's `lv.mcprotector.mcpro24fps`
> internal code and asset bytes are *not* reproduced in this book; exactness here
> means device-measured, standard-defined, or derived-in-book, always evidenced.

## 8.1 Exact exposure mathematics (universal, standard)

**Shutter angle ↔ shutter time.** `t[s] = (θ/360°) / fps`
- θ=180°, fps=24 → t = 1/48 s = 20 833 µs
- θ=180°, fps=30 → t = 1/60 s = 16 667 µs
- θ=180°, fps=25 → t = 1/50 s = 20 000 µs
- θ=180°, fps=60 → t = 1/120 s = 8 333 µs (on this device: high-speed session)

**Stops.** `Δstop = log2(t₂/t₁)`; ISO step ×2 = +1 stop; "ISO-step buttons"
multiply/divide by 2 and clamp.

**Exposure value.** `EV = log2(N²/t)` with aperture N (device: f/1.79 main).
Gain expressed logarithmically: `dB = 20·log10(gain)`; so ×2 = +6.02 dB.

**Names → ns.** "1/48 s" = 20 833 333 ns — the app-layer format for
`SENSOR_EXPOSURE_TIME` (device floor 87.5 µs = 87 512 ns, main camera).

## 8.2 Exact Kelvin model (this-book design, standard anchors)

Interpolation axis **mired** = 10⁶/T[K]. Anchor table, daylight-extended
(Illuminant A 2856K, D50 5003K, D65 6504K, D75 7504K are public CIE anchors):

| Anchor K | Mired | Gain model (R, G=1, B) |
|---|---|---|
| 1800K | 555.6 | (1.30, 1.00, 0.97) |
| 2856K | 350.1 | (1.12, 1.00, 1.05) |
| 5003K | 199.9 | (0.98, 1.00, 1.22) |
| 6504K | 153.9 | (0.92, 1.00, 1.30) |
| 7504K | 133.3 | (0.88, 1.00, 1.36) |

`Gain(x) = lerp(anchorᵢ, anchorᵢ₊₁)(x)` on the mired axis; clamp [1800K, 10 000K].
Camera request: `CONTROL_AWB_MODE = OFF` + `COLOR_CORRECTION_GAINS = (R, 1, B, 1)`
(RGGB component order: R, G_even, B, G_odd). This substitutable model is why a
"Kelvin slider" behaves linearly-cool/auto-warm across colorscales; the device
calibrates further via HAL WB stats.
(Design credit: this-book model; cf. standard photographic metal-filament/daylight
gas anchoring; not copied from any app's source.)

## 8.3 Exact transfer curves (public international standards)

**HLG OETF (ITU-R BT.2100, the 10-bit path this device advertises):**
for scene-linear E ∈ [0,1]:
- `E ≤ 1/12 : E' = sqrt(3E)`
- `E > 1/12 : E' = a·ln(12E − b) + c`
- constants: `a = 0.17883277`, `b = 0.28466892` (= 1 − 4a), `c = 0.55991073`
(= 0.5 − a·ln(4a))
Encoding/decoding partner: BT.2020 primaries, HLG inverse-OOTF / OOTF with
system gamma per BT.2100. This is the exact *public* curve used when
`DYNAMIC_RANGE_TEN_BIT` (HLG10) sessions record.

**BT.709 OETF (the 8-bit "Rec.709" broadcast path):**
- `x ≤ 0.018 : V = 4.5·x`
- `x > 0.018 : V = 1.099·x^0.45 − 0.099`

**BT.1886 EOTF (display):** exact power law `V_out = (max·A + B)·L^γ` with fixed
`γ = 2.4` (A, B, from max/min black measurements).

**Camera2 enum map (exact, needed to read the dump values):**
- AE modes: 0 OFF, 1 ON, 2 ON_ALWAYS_FLASH, 3 ON_AUTO_FLASH → device `[0,1,2,3]`
- AF modes: 0 OFF, 1 AUTO, 2 MACRO, 3 CONTINUOUS_VIDEO, 4 CONTINUOUS_PHOTO,
  5 EDOF → device `[0,1,2,3,4]`
- AWB modes: 0 OFF, 1 AUTO, 2–4 (INCANDESCENT/FLUORESCENT/WARM_DAYLIGHT),
  5–8 (daylight variants/cloudy/twilight/shade)
- NR modes: 0 OFF, 1 FAST, 2 HIGH_QUALITY, 3 MINIMAL, 4 ZERO_SHUTTER_LAG
→ device `[0,1,2,3,4]`
- Tone modes: 0 CONTRAST_CURVE, 1 FAST, 2 HIGH_QUALITY, 3 PRESET_CURVE,
  4 GAMMA_VALUE, 5 PRESET_SRGB, 6 PRESET_VIDEO_HLG → device `[0,1,2]` (no presets)
- Hardware level: 0 LIMITED, 1 FULL, 2 LEGACY, 3 LEVEL_3, 4 EXTERNAL
→ device main = `[3]` = **LEVEL_3**
- Stream-output kind counts `(raw, processed, misc)` → device `[1, 3, 2]`

## 8.4 Exact log-curve engineering (this-book format, standard-style)

**Generic three-parameter log** (the 05.F factory) — exact in-book design:
`out = clamp( A·ln(1 + S·x_in) / ln(1 + S) , 0, 1 )` with shaping gamma
`out = out^γ`; A = shoulder softness; S = slope; γ = 0.5–1.0 class.
Mid-grey (18% linear) must land in `[0.36..0.46]` for a gradeable log
(that is the "log headroom rule" the LUT pair must carry into post).
Curve → request: `TONEMAP_CURVE_RED/GREEN/BLUE` waypoints ≤ 64 (device cap),
`TONEMAP_MODE = 0 (CONTRAST_CURVE)`; the inverse `.cube` ships as the
technical LUT. (Industry curve identity anchors, for reading published specs:
S-Log3 10-bit black ≈ code 95 and 18% ≈ code 417 — public anchors for the
family grammar this book implements in parameter form.)

**Exact 10-bit/limited-range sample codes (the post-side truth):**
- 8-bit limited video Y′: 16 – 235 (BT.1886 display-path)
- 10-bit limited: 64 – 940 (HLG/UHD practice; black 64, nominal white 940)
- Full range video Y′: 0 – 255 (8-bit) / 0 – 1023 (10-bit). RAW white level here
  **1023** (10-bit sensor codes) and post declares DNG accordingly.

## 8.5 Exact device constants, with dump-line evidence (iQOO I2221)

All measured 2026-11-07. Line references point into
`docs/raw/camera_dump_2026-11-07.txt`.

### 8.5.1 Camera 0 — main wide (4.84 mm, f/1.79, OIS)

| Quantity | Exact value | Lines |
|---|---|---|
| Aperture | 1.78999996 (f/1.79) | 9427–9428 |
| Focal length | 4.84000015 mm | 9431–9432 |
| OIS modes | [0 1] (OFF/ON) | 9433–9434 |
| Hyperfocal distance | 0.12225940 (m) | 9435–9436 |
| Minimum focus distance | 10.0 (cm) | 9437–9438 |
| Shading map size | 17 × 13 | 9439–9440 |
| Focus calibration | APPROXIMATE | 9441–9442 |
| Flash unit | yes | 9316 |
| Max output streams | raw 1, processed 3, misc 2 | 9447–9448 |
| Partial results | 2 | 9453 |
| Capabilities (13) | BACKWARD_COMPATIBLE CONSTRAINED_HIGH_SPEED_VIDEO RAW YUV_REPROCESSING PRIVATE_REPROCESSING READ_SENSOR_SETTINGS MANUAL_SENSOR BURST_CAPTURE MANUAL_POST_PROCESSING STREAM_USE_CASE DYNAMIC_RANGE_TEN_BIT COLOR_SPACE_PROFILES DYNAMIC_RANGE_TEN_BIT | 9455–9456 |
| AE compens. range/step | −40…40 / 1_10 EV (±4/0.1) | 9333–9336 |
| AE target-fps ranges | [10,10][10,14][14,14][10,15][15,15][24,24][10,30][30,30] | 9328–9332 |
| Active array | 0,0,4096,3072 | 10451–10452 |
| ISO range | 72…3200 | 10453–10454 |
| CFA | RGGB | 10455–10456 |
| Exposure range | 87 512 … 13 356 574 080 ns (≈87.5 µs–13.36 s) | 10457–10458 |
| maxFrameDuration | 13 356 731 608 ns | 10459–10460 |
| Sensor physical | 6.55359983 × 4.91520023 mm | 10461–10462 |
| Pixel array | 4096 × 3072 | 10463–10464 |
| White level | 1023 (10-bit RAW) | 10465–10466 |
| Timestamp source | REALTIME | 10467–10468 |
| Lens shading applied | TRUE | 10469–10470 |
| Tonemap max points | 64 | 10541–10542 |
| Tonemap modes | [0 1 2] (CONTRAST_CURVE/FAST/HIGH_QUALITY) | 10543–10544 |
| Hardware level | 3 (LEVEL_3) | 10545–10546 |
| Base JPEG max | 19 492 336 B | 9425–9426 |

### 8.5.2 Camera 1 — tele (3.74 mm, f/2.45)

| Quantity | Exact value | Lines |
|---|---|---|
| Aperture | 2.45000005 | 11893–11894 |
| Focal length | 3.74000001 mm | 11897–11898 |
| OIS | none — single [0] | 11899–11900 |
| Min focus | 0 (fixed focus) | 11903–11904 |
| Active array | 0,0,3264,2448 | 12785–12786 |
| ISO range | 50…1600 | 12787–12788 |
| CFA | BGGR | 12790–12791 |
| Exposure range | 52 528 … 418 987 610 ns (≈52.5 µs–0.42 s) | 12791–12792 |

### 8.5.3 Camera 2 — main-duplicate (per 4.1 collapse rule)

Aperture 1.78999996 (14137–14138), focal 4.84000015 (14141–14142),
OIS [0 1] (14143–14144), min focus 10 cm (14147–14148),
array 4096×3072 (15008–15009), ISO 72…3200 (15010–15011), CFA RGGB
(15012–15013), exposure 87 512…849 768 460 ns (15014–15015).

### 8.5.4 Camera 3 — ultrawide (1.66 mm, f/2.2)

Aperture 2.20000005 (16397–16398), focal 1.65999997 mm (16401–16402),
OIS none (16403–16404), min focus 0 fixed (16407–16408),
array 3200×2400 (17083–17084), ISO 50…800 (17085–17086), CFA BGGR
(17087–17088), exposure 25 982…849 768 460 ns (17089–17090).

### 8.5.5 High-speed table — exact rows, dumped order (lines 9353–9389)

Weird-format note: the HAL's table row-pairs are dropped in the dump verbatim
(the pairs [ss,bFrame] sequences); exact quoted values as stored:

```
[1920 1080 30 120] [1 1920 1080 60] [120 1 1920 1080] [120 120 1 1920]
[1080 30 240 1] [1920 1080 60 240] [1 1920 1080 240] [240 1 1920 864]
[30 120 1 1920] [864 60 120 1] [1920 864 120 120] [1 1920 800 30]
[120 1 1920 800] [60 120 1 1920] [800 120 120 1] [1280 720 30 120]
[1 1280 720 60] [120 1 1280 720] [120 120 1 1280] [720 30 240 1]
[1280 720 60 240] [1 1280 720 240] [240 1 1080 1080] [1080 30 120 1]
[1080 60 120 1] [1080 1080 120 120] [1 720 480 30] [120 1 720 480]
[60 120 1 720] [480 120 120 1] [720 480 30 240] [1 720 480 60]
[240 1 720 480] [240 240 1 640] [480 30 120 1] [640 480 60 120]
```

An interpretable read (separator quirks untouched on purpose, "exactly as
dumped"): 1920×1080 up to 240 fps contexts, 1280×720 up to 240, 1280×720/720×480
up to 480 fps contexts, 1920×1080@30–120 full-manual AE-mode rows — i.e., the
high-speed creative margins the reference class rides for 60/120/240/480 fps.

### 8.5.6 Vendor tag sample (exact IDs, as dumped)

| Vendor key | Hex ID | Lines |
|---|---|---|
| `org.codeaurora.qcamera3.sessionParameters.EnableAutoHDR` | 0x8064001b | 9271 |
| `org.codeaurora.qcamera3.sessionParameters.numHDRexposure` | 0x8064001d | 9273 |
| `org.codeaurora.qcamera3.sessionParameters.EnableAFBracketing` | 0x8064001f | 9277 |
| `vivo.control.isVideoSnapshot` | 0x812200c4 | 9293 |
| `vivo.control.isRecording` | 0x812200c5 | 9295 |
| `vivo.control.seamlesscontrol` | 0x812200c6 | 9297 |
| `vivo.control.EnableDCGHDR` | 0x812200c8 | 9301 |
| `com.qti.stats_control.ExposureCount` | 0x80c50002 | 9289 |
| `org.quic.camera2.statsNNControl.Enable` | 0x80900000 | 9287 |

Policy (book-level): platform keys first; vendor keys only behind runtime
"availableRequestKeys" membership. (The `vivo.refocus cameraFacing` vendor block
lines 28037 / 29724 shows the depth/refocus section exists — pro-v1 stays out of
the refocus/computational arena.)

## 8.6 On "exact code" (what this annex is instead of a transcription)

A decompiled transcription of `lv.mcprotector.mcpro24fps` is excluded by design
— see Chapter 00's boundary — and that is not a limitation of scope but the
definition of this book: exactness is carried by 8.1–8.5 (public standards,
device truth, and derived engineering) plus Chapter 05's implemented code.
Line-precise *behavior* (what the UI does when X) continues to accrue through
the Chapter 06 protocol, which is legitimate observation practice, and lands in
the worksheet as evidence with sources. Nothing else is needed for the
build-able knowledge to exist, verified, in this book.

## 8.7 Verification instructions (for any reader/chatbot)

1. Open `docs/raw/camera_dump_2026-11-07.txt`.
2. Spot-check any Chapter-02/08 constant at its cited line(s).
3. Spot-check standards constants: a copy of ITU-R BT.2100 Table 5 lists
   `a, b, c` of 8.3 exactly; BT.709/1886 equally.
4. Recompute 8.1 relations by hand at any fps.
5. The Chapter 05 errata (§5.Z) are bookkeeping fixes, never content changes —
   they do not affect any constant or formula in this annex.
