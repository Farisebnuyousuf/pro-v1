# Device capability report — iQOO I2221 (verification phone)

Measured directly from the device's camera HAL with `adb shell dumpsys media.camera`,
on 2026-11-07. **Provenance: Android OS / Qualcomm HAL vendor_qti — not extracted
from any third-party app's code.** Raw evidence: `docs/raw/camera_dump_2026-11-07.txt`.
Software context: Android 16, Snapdragon 8 Gen 3 (`pineapple`), vivo/iQOO firmware.

This file is the honest-parameters reference for every milestone: the app should
surface these ranges truthfully and adapt gracefully when a phone reports less.

## Reference app installed

- `lv.mcprotector.mcpro24fps` (mcpro24fps), versionCode 430037, versionName `043dd`,
  minSdk 24, targetSdk 36. Runs on the same HAL documented below — so anything it
  records on this phone is achievable within these limits. It is used as a
  **behavioral/UX reference only**: we observe what it does, we never touch its
  binaries, assets, or branding (its license forbids that; and our app is our owns).

## Rear cameras (vendor_qti provider, 4 physical devices)

| # | Lens | Aperture | OIS | Min focus | Role |
|---|------|----------|-----|-----------|------|
| 0 | 4.84 mm | f/1.79 | yes | 10 cm (APPROXIMATE calib.) | main wide — our primary |
| 1 | 3.74 mm | f/2.45 | no | fixed (0 cm) | 2×-equivalent portrait/tele |
| 2 | 4.84 mm | f/1.79 | yes | 10 cm | duplicate of main (vivo dual-pipeline mode) — verify which one to open at runtime |
| 3 | 1.66 mm | f/2.2 | no | fixed (0 cm) | ultrawide |

Front: 2 front cameras on the `vendorVcf` (vivo computational) provider. pro-v1
stays on the standard `vendor_qti` Camera2 path for platform-first behavior.

## Main camera (device 0) — measured numbers

### Sensor
- Sensor size: **6.55 × 4.92 mm** (≈1/1.5" class)
- Active / pixel array: 4096 × 3072, **RGGB**
- White level: **1023** → RAW pipeline is **10-bit** (RAW10) — DNGs must declare this
- Timestamp source: **REALTIME** (per-frame pipeline later needs this)
- Lens shading applied by HAL = TRUE
- maxFrameDuration: 13.4 s → long-exposure stills possible

### Manual controls (M2)
- **ISO range: 72 – 3 200.** Honest callout: far below marketing claims of other
  modes; extended ISO normally runs via vivo's vendor pipeline. Manual ISO UI shows
  exactly this range and reports it in the capability report.
- **Exposure: 87.5 µs – 13.36 s** (≈1/11 400 s – 13 s)
- AE modes include **AE OFF** (`aeAvailableModes [0,1,2,3]`) — locked-manual path OK
- AF modes [0–4] (incl. AF OFF) — **manual focus is only possible on the main
  lenses**; tele/UW report fixed focus → MF slider disables there
- WB: 9 modes available; AE compensation: **±4 EV, step 0.1**; antibanding [0–3];
  NR modes [0–4]; aberration [0,1,2]; metering regions: 1 AF / 1 AE / 1 AWB
  (tap-to-meter = 1 region; no multi-region metering)

### Motion / fps (M4)
- Standard AE target-fps locks: 10, 14, 15, 24, 30 (no 60-60 lock in standard AE)
- **CONSTRAINED_HIGH_SPEED_VIDEO** supported: high-speed table includes
  **1080p up to 240 fps** and 720p down to 480 fps — the 60/120/240 fps tiers and
  slow-mo must use constrained-high-speed sessions
- Video stabilization at HAL level: only OFF available (`[0]`) — vivo's EIS lives in
  the vendor pipeline (session params), not in standard Camera2. Treat EIS as a
  vendor-investigation item, not a guaranteed toggle.

### Color / tone curve (M5)
- Hardware level: **LEVEL_3**
- `tonemap.availableToneMapModes [0,1,2]` = CONTRAST_CURVE, FAST, HIGH_QUALITY —
  **no PRESET_CURVE (3) and no GAMMA_VALUE (4)** → our log gamma must be built as a
  custom **CONTRAST_CURVE with up to 64 control points** (tonemap.maxCurvePoints=64)
- **DYNAMIC_RANGE_TEN_BIT + COLOR_SPACE_PROFILES** capabilities present → **10-bit
  HLG recording is available** (API 34+ path with HLG10 Profile). HLG is the other
  grade-friendly "log-adjacent" alternative to the custom curve.
- **RAW** capability present → DNG capture fine; 10-bit white level per above

## Implications checklist for the milestone ladder

| M | What this device allows |
|---|------------------------|
| M2 (manual v1) | ISO slider 72–3200, shutter 87 µs–13.4 s, WB 9 modes, MF on main only |
| M3 (exposure engine) | AE OFF + 1-region tap meter, ±4 EV comp, lock via AE OFF |
| M4 (quality & motion) | fps locks: 10/14/15/24/30 standard; 60/120/240 via high-speed; EIS via vendor session params (investigate) |
| M5 (log profile) | custom 64-point CONTRAST_CURVE log, and/or HLG10 10-bit record; RAW10 DNG white level 1023 |
| M6 (pro UX) | histogram from YUV preview; level/scale — no HAL constraint |

## Open verification items (owner QA on hardware)
1. Which of devices 0/2 opens with the full pipeline when requested — test both at runtime.
2. Do vivo session-parameter EIS toggles actually apply under Camera2 (dump shows `vivo.control.seamlesscontrol` and friends)? Probe, log, report.
3. Confirm min/anti-banding behavior at 10/14/15 fps locks (flicker use cases).
4. DNG white level and shading map behavior: verify DNG opens correctly in Lightroom.
