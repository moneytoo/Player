# (Just) Player

Android video player based on [ExoPlayer](https://github.com/google/ExoPlayer)

This Player uses ExoPlayer's ``extension-ffmpeg`` with [all its audio formats](https://exoplayer.dev/supported-formats.html#ffmpeg-extension) enabled.

ExoPlayer properly synces audio with video track when using Bluetooth earphones/speaker. (I was not able to find any other nice ExoPlayer based video player so I created this one.)

Features already provided by ExoPlayer:

 * Audio track selection
 * Subtitle selection
 * Playback speed control

Actual features of this Player:

 * Horizontal swipe to quickly seek
 * Vertical swipe to change brightness (left) / volume (right)
 * Remember last opened file, its position, brightness
 * PiP (Picture in Picture) on Android 8 or higher
 * Video title etc.
 * No ads, tracking or even the Internet permission

To load an external (non-embedded) subtitles, long press the file open action in the bottom bar.

## Download

Available at the [Play Store](https://play.google.com/store/apps/details?id=com.brouken.player)
