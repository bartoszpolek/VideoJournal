# Storage notes

Video files are stored in app-private directories:

- final videos: `context.filesDir/videos/{uuid}.mp4`
- thumbnails: `context.filesDir/thumbnails/{uuid}.jpg`
- in-progress recordings: `context.cacheDir/video_recording/{uuid}.mp4`

The app does not request runtime storage permission for these paths. Android keeps app-private
storage scoped to this app, so the user-facing graceful permission flow only needs camera access
and optional microphone access. IO failures, missing temp files, and thumbnail failures are handled
as regular save/cleanup errors rather than permission prompts.
