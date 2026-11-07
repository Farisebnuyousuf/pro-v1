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
