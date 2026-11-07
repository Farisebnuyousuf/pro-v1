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
