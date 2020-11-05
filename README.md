# (Just) Player

Android video player based on [ExoPlayer](https://github.com/google/ExoPlayer)

To play a video, open it with this Player from any file manager.

This Player uses ExoPlayer's ``extension-ffmpeg`` with [all its audio formats](https://exoplayer.dev/supported-formats.html#ffmpeg-extension) enabled.

ExoPlayer properly synces audio with video track when using Bluetooth earphones/speaker. (I was not able to find any other nice ExoPlayer based video player so I created this one.)

Features already provided by ExoPlayer:

 * Audio track selection
 * Subtitle selection (embedded only)
 * Playback speed control

Actual features of this Player:

 * Horizontal swipe to quickly seek
 * Vertical swipe to change brightness (left) / volume (right)
 * Remember last opened file, brightness
