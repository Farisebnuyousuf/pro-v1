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
