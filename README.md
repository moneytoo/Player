# (Just) Player

Android video player based on [ExoPlayer](https://github.com/google/ExoPlayer)

To play a video, open it with this Player from any file manager (I recommend Solid Explorer).

This Player uses ExoPlayer ``extension-ffmpeg`` to allow playback off following audio formats:

 * AC-3
 * E-AC-3
 * DTS, DTS-HD

ExoPlayer properly synces audio with video track when using Bluetooth earphones/speaker. (I was not able to find any other nice ExoPlayer based video player so I created this one.)

Features already provided by ExoPlayer:

 * Audio track selection
 * Subtitle selection (embedded only)

Actual features of this Player:

 * Horizontal swipe to seek
 * Vertical swipe to change brightness
 * Rembember last opened file, brightness
