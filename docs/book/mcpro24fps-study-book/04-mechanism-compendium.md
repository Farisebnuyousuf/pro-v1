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
