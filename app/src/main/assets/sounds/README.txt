6 of these 8 audio files are still SYNTHETIC PLACEHOLDERS, generated with
ffmpeg's noise generators (anoisesrc, different noise colors + light
filtering per file). They are not recordings of forest, wind, etc. - they
exist purely so the sound machine's playback pipeline (streaming, looping,
volume, sleep timer) is mechanically testable end-to-end without depending
on real ambient-sound content that would need to be sourced and licensed.

rain.mp3 and rain_on_tent.mp3 are REAL recordings: extracted from two
1-hour ambience videos Rusty supplied, trimmed to a clean 3-minute
mid-recording segment (skips any intro/outro), and built into a seamless
loop via an ffmpeg acrossfade (last 6s crossfaded into the first 6s) so
Web Audio's loop=true doesn't produce an audible click/gap at the seam.

To replace a placeholder with the real thing, just overwrite the matching
file with real audio (same filename, or update the path in
sound/SoundLibrary.kt if you rename it) - Aurora just serves the raw bytes
over HTTP (GET /sound/stream?id=...) for the Echo Show's Web Audio engine
to decode, so any format the browser's decodeAudioData() supports works
(mp3/ogg/wav). That's the whole point of the "drop files into assets"
design: no code changes needed. If the replacement isn't already a clean
loop, run it through an acrossfade pass like the one used for rain.mp3
above to avoid an audible seam.

Current catalog entry mapping (see SoundLibrary.kt):
  rain.mp3           - Rain             (real, 3 min loop)
  rain_on_tent.mp3   - Rain on Tent     (real, 3 min loop)
  heavy_rain.mp3     - Heavy Rain       (placeholder)
  thunderstorm.mp3   - Thunderstorm     (placeholder)
  forest.mp3         - Forest           (placeholder)
  wind.mp3           - Wind             (placeholder)
  ocean_waves.mp3    - Ocean Waves      (placeholder)
  fireplace.mp3      - Fireplace        (placeholder)

For real bundled sounds, keep them reasonably compressed (~128kbps MP3 or
OGG Vorbis) rather than lossless - the Echo Show 5's hardware is modest and
this keeps both APK size and decode overhead down. Custom sounds imported
by the user at runtime (via Storage Access Framework) support FLAC/WAV too
since those are user-supplied one-offs, not something bundled into every
install.
