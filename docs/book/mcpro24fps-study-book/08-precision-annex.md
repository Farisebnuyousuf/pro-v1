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
