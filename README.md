# Android Media Player


A robust Android media player application that plays both audio and video files from local storage with full playback controls.

## Features

- 🎵 **Audio Playback**: MP3, WAV, AAC, and other audio formats
- 🎥 **Video Playback**: MP4, MKV, and other video formats
- 🎚️ **Playback Controls**:
  - Play/Pause
  - Seek forward/backward (10-second jumps)
  - Progress seekbar
  - Current time/duration display
- 📁 **File Browser**: Select media files from device storage
- 🔄 **Surface Handling**: Proper video surface lifecycle management

## Technical Implementation

### Core Components

1. **MediaPlayer**: Android's native media playback engine
2. **SurfaceView**: For video rendering
3. **Handler**: For UI updates during playback
4. **FileUtils**: URI to file path conversion

### Key Classes

- `MainActivity`: Main controller handling UI and playback
- `FileUtils`: Helper for file path resolution

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/yourusername/android-media-player.git
