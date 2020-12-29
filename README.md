<img align="left" width="56" src="https://raw.githubusercontent.com/moneytoo/Player/master/fastlane/metadata/android/en-US/images/icon.png">

# Just (Video) Player 

[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/moneytoo/Player)](https://github.com/moneytoo/Player/releases/latest)
[![Google Play](https://img.shields.io/endpoint?color=green&logoColor=green&url=https%3A%2F%2Fplayshields.herokuapp.com%2Fplay%3Fi%3Dcom.brouken.player%26l%3DGoogle%2520Play%26m%3Dv%24version)](https://play.google.com/store/apps/details?id=com.brouken.player)
[![F-Droid](https://img.shields.io/f-droid/v/com.brouken.player)](https://f-droid.org/packages/com.brouken.player/)
![Lines of code](https://img.shields.io/tokei/lines/github/moneytoo/Player)
[![ExoPlayer](https://img.shields.io/badge/ExoPlayer-v2.12.2-007ec6)](https://github.com/google/ExoPlayer)

Android video player based on [ExoPlayer](https://github.com/google/ExoPlayer)

It uses ExoPlayer's ``extension-ffmpeg`` with [all its audio formats](https://exoplayer.dev/supported-formats.html#ffmpeg-extension) enabled (it can handle even special formats like AC3, EAC3, DTS, DTS HD, TrueHD etc.).

It properly synces audio with video track when using Bluetooth earphones/speaker. (I was not able to find any other nice ExoPlayer based video player so I created this one.)

## Supported formats

 * **Audio**: Vorbis, Opus, FLAC, ALAC, PCM/WAVE (μ-law, A-law), MP1, MP2, MP3, AMR (NB, WB), AAC (LC, ELD, HE; xHE on Android 9+), AC-3, E-AC-3, DTS, DTS-HD, TrueHD
 * **Video**: H.263, H.264 AVC (Baseline Profile; Main Profile on Android 6+), H.265 HEVC, MPEG-4 SP, VP8, VP9, AV1
 * **Containers**: MP4, MOV, WebM, MKV, Ogg, MPEG-TS, MPEG-PS, FLV
 * **Subtitles**: SRT, SSA, ASS, TTML, VTT

HDR and HDR10+ video playback on compatible/supported hardware.

## Screenshot

<img src="https://raw.githubusercontent.com/moneytoo/Player/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="540">

## Features

 * Audio/subtitle track selection
 * Playback speed control
 * Horizontal swipe to quickly seek
 * Vertical swipe to change brightness (left) / volume (right)
 * PiP (Picture in Picture) on Android 8 or higher
 * Resize (fit/crop)
 * No ads, tracking or even the Internet permission

To load an external (non-embedded) subtitles, long press the file open action in the bottom bar.

Donate: [PayPal](https://paypal.me/MarcelDopita) | [Bitcoin](bitcoin:BC1Q9U2EZGSNUG995FV0M4VAXA90UJJWLUCP78W4N0) | [Litecoin](litecoin:LLZ3fULGwxbs6W9Vf7gtu1EjZvviCka7zP)

## Download

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="75">](https://play.google.com/store/apps/details?id=com.brouken.player)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="75">](https://f-droid.org/packages/com.brouken.player/)
