# Audio Format Support

Rhythm uses **Media3 ExoPlayer 1.10.1 + FFmpeg Decoder** for professional-grade audio playback. This page details supported formats, technical limitations, and recommendations.

---

## ✅ Fully Supported Formats

These formats work out-of-the-box on all supported Android devices:

| Format | Container | Quality | Bit Depth | Sample Rate | Notes |
|:---:|:---:|:---:|:---:|:---:|:---|
| **FLAC** | `.flac` | Lossless | Up to 32-bit | Up to 384kHz | Recommended for lossless audio |
| **ALAC** | `.m4a`, `.alac` | Lossless | Up to 32-bit | Up to 384kHz | Apple Lossless Audio Codec |
| **MP3** | `.mp3` | Lossy | N/A | Up to 48kHz | All bitrates, VBR support |
| **AAC** | `.m4a`, `.aac`, `.mp4` | Lossy | N/A | Up to 96kHz | AAC-LC, HE-AAC, HE-AACv2 |
| **Vorbis** | `.ogg` | Lossy | N/A | Up to 192kHz | Ogg Vorbis audio |
| **Opus** | `.opus`, `.ogg` | Lossy/Lossless | N/A | Up to 48kHz | Modern, efficient codec |
| **WAV** | `.wav` | Lossless | Up to 32-bit | Up to 192kHz | Uncompressed PCM audio |
| **PCM** | Various | Lossless | Up to 32-bit | Up to 192kHz | Raw audio data |

---

## 🎬 FFmpeg-Decoded Formats

These formats are decoded by the bundled FFmpeg extension (available in all builds):

| Format | Container | Notes |
|:---:|:---:|:---|
| **EAC3-JOC (Dolby Atmos)** | `.eac3`, `.m4a`, `.mkv` | Decoded via FFmpeg; stereo/surround output |
| **AC-3 (Dolby Digital)** | `.ac3`, `.m4a` | Decoded via FFmpeg extension |
| **WMA** | `.wma` | Decoded via FFmpeg extension |

---

## ⚠️ Device-Dependent Formats

These formats require hardware support and may not work on all devices:

| Format | Container | Notes |
|:---:|:---:|:---|
| **Dolby Digital (AC-3)** | `.ac3`, `.m4a` | Also decoded via FFmpeg extension in Rhythm |
| **Dolby Digital Plus (E-AC-3)** | `.eac3`, `.m4a` | Also decoded via FFmpeg (EAC3-JOC/Atmos supported) |
| **DTS** | `.dts`, `.m4a` | Requires compatible device/hardware decoder |
| **Dolby Atmos** | Various | EAC3-JOC decoded via FFmpeg; full Atmos requires hardware |
| **WMA** | `.wma` | Also decoded via FFmpeg extension in Rhythm |

### Checking Device Compatibility

To check if your device supports Dolby/DTS:
1. Try playing a test file in Rhythm
2. If it plays, your device has the necessary decoder
3. If it fails, convert to a supported format (see below)

---

## ❌ Unsupported Formats

These formats are **not supported** by Media3 ExoPlayer. You must convert them to play in Rhythm:

| Format | Why Not Supported | Recommended Alternative |
|:---:|:---|:---|
| **APE** (Monkey's Audio) | Proprietary codec, no Android support | Convert to **FLAC** |
| **DSD/DSF** | Super Audio CD format, niche codec | Convert to **FLAC** or **PCM** |
| **MQA** | Proprietary MQA codec | Use **FLAC** version |
| **WMA Lossless** | Microsoft proprietary, limited support | Convert to **FLAC** |
| **AIFF** | Limited Android support | Convert to **WAV** |
| **TAK** | Rare lossless format | Convert to **FLAC** |
| **WavPack** | Limited mobile support | Convert to **FLAC** |
| **MKA** (Matroska Audio) | Container not scanned/supported by the local media library | Extract audio stream (e.g. to **FLAC** or **M4A**) |

---

## 📊 Audio Quality Detection

Rhythm automatically detects and displays quality badges in the player:

| Badge | Criteria | Formats |
|:---:|:---|:---|
| **🔊 Lossless** | Lossless compression | FLAC, ALAC, WAV, PCM |
| **🎭 Dolby** | Dolby Audio detected | AC-3, E-AC-3, Dolby Atmos |
| **🎬 DTS** | DTS Audio detected | DTS, DTS-HD |
| **📡 Hi-Res** | ≥48kHz sample rate OR lossless | FLAC ≥48kHz, Hi-Res WAV |

### Quality Detection Details

- **Lossless Detection**: Based on codec type
- **Hi-Res Detection**: Sample rate ≥48kHz or bit depth ≥24-bit
- **Dolby/DTS Detection**: Codec identification from metadata
- **Container Detection**: Parsed from file headers

---

## 🔧 Technical Limitations

### Container Format Ambiguity
Some containers can hold multiple codecs:
- `.m4a` can contain: AAC, ALAC, AC-3, E-AC-3
- `.ogg` can contain: Vorbis, Opus, FLAC
- `.mp4` can contain: AAC, AC-3, video tracks

Rhythm identifies the **actual codec** inside, not just the container.

### Matroska Audio (.mka)
Rhythm does not support Matroska Audio (`.mka`) files. Although ExoPlayer can play audio streams from Matroska container (`.mkv`) files when parsed as video streams, the dedicated `.mka` audio-only container is not registered for local media scanning. To play these files, we recommend extracting the underlying audio streams into standard FLAC, M4A, or MP3 containers.

### Hardware Dependencies
- **Dolby/DTS**: Requires device-specific hardware decoders
- **Sample Rate**: Limited by Android AudioTrack (typically 192kHz max)
- **Bit Depth**: Most Android devices support 16-bit or 24-bit
- **Channel Configuration**: Stereo universally supported, multi-channel varies

### ExoPlayer Capabilities
- **ExoPlayer 1.10.1 + FFmpeg**: Adds EAC3-JOC, AC-3, WMA decoding beyond ExoPlayer defaults
- **Gapless Playback**: Supported for MP3, AAC, FLAC, Opus
- **Seeking**: Accurate for most formats, approximate for some streaming codecs
- **Metadata**: Depends on container format (ID3 for MP3, Vorbis comments for FLAC)
- **Album Art**: Embedded images in FLAC, MP3, M4A

---

## 💡 Format Recommendations

### For Maximum Compatibility
✅ **Use these formats**:
1. **FLAC** - Universal lossless support
2. **MP3** - Universal lossy support (320kbps or V0 for quality)
3. **AAC** - Modern lossy format (256kbps recommended)

### For Lossless Audio Libraries
✅ **FLAC is the gold standard**:
- Widely supported across all platforms
- Open-source and royalty-free
- Excellent compression (50-60% of WAV size)
- Preserves all audio data perfectly
- Supports embedded artwork and metadata

### For Hi-Res Audio
✅ **FLAC 24-bit/96kHz or higher**:
- Check your device's DAC capabilities first
- Most modern smartphones support up to 192kHz
- Diminishing returns beyond 96kHz for most listeners
- Use high-quality headphones/speakers to appreciate the difference

### For Space Efficiency
✅ **Modern lossy formats**:
- **Opus** @ 128-160kbps: Best quality-per-bitrate
- **AAC** @ 256kbps: Excellent quality, wide compatibility
- **MP3** @ 320kbps or V0: Still excellent for most listeners

---

## 🛠️ Converting Unsupported Formats

### Recommended Conversion Tools

#### Desktop (Windows/Mac/Linux)
1. **[FFmpeg](https://ffmpeg.org/)** (Command-line)
   ```bash
   # APE to FLAC
   ffmpeg -i input.ape output.flac
   
   # DSD to FLAC
   ffmpeg -i input.dsf output.flac
   
   # AIFF to WAV
   ffmpeg -i input.aiff output.wav
   ```

2. **[dBpoweramp](https://www.dbpoweramp.com/)** (Windows/Mac - Paid)
   - User-friendly GUI
   - Batch conversion
   - Preserves metadata

3. **[foobar2000](https://www.foobar2000.org/)** (Windows - Free)
   - Excellent converter
   - Extensive format support
   - Free and lightweight

#### Online (Web-based)
- **[CloudConvert](https://cloudconvert.com/)**: Supports 200+ formats
- **[Online-Convert](https://www.online-convert.com/)**: Audio conversion
- **[Zamzar](https://www.zamzar.com/)**: Various audio formats

⚠️ **Privacy Warning**: Online converters upload your files. Use desktop tools for sensitive/personal audio.

### Batch Conversion Scripts

For Linux/Mac users with many files:

```bash
#!/bin/bash
# Convert all APE files to FLAC in current directory
for file in *.ape; do
    ffmpeg -i "$file" "${file%.ape}.flac"
done
```

```bash
# Convert all DSD files to FLAC (24-bit/88.2kHz)
for file in *.dsf; do
    ffmpeg -i "$file" -sample_fmt s32 -ar 88200 "${file%.dsf}.flac"
done
```

---

## 🎯 Troubleshooting Playback Issues

### File Won't Play
1. **Check format**: Verify it's a supported format
2. **Inspect codec**: Use MediaInfo or FFprobe to identify actual codec
3. **Test on device**: Try playing in other apps (VLC, YouTube Music)
4. **Convert format**: Use FFmpeg to convert to FLAC or MP3

### Dolby/DTS Files Won't Play
- **Not supported by device**: Convert to FLAC or AAC
- **Check device specs**: Look for Dolby Atmos/DTS support in manufacturer specs
- **Try other apps**: Test if VLC or other players work

### Poor Audio Quality
- **Check bitrate**: Low bitrate = poor quality
- **Verify source**: Ensure original file is high quality
- **Use equalizer**: Apply AutoEQ preset for your device
- **Check output device**: Quality limited by headphones/speakers

### Metadata Not Showing
- **Re-tag files**: Use MP3Tag or Picard to fix metadata
- **Rescan library**: Settings → Library → Rescan Media
- **Check file permissions**: Ensure Rhythm has read access

---

## 📚 Additional Resources

### Format Information
- [Wikipedia: Audio Coding Format](https://en.wikipedia.org/wiki/Audio_coding_format)
- [Hydrogen Audio Wiki](https://wiki.hydrogenaud.io/)
- [ExoPlayer Supported Formats](https://exoplayer.dev/supported-formats.html)

### Metadata Tools
- **[Mp3tag](https://www.mp3tag.de/)** - Windows/Mac metadata editor
- **[MusicBrainz Picard](https://picard.musicbrainz.org/)** - Auto-tagging tool
- **[Kid3](https://kid3.kde.org/)** - Cross-platform tag editor

### Audio Analysis
- **[MediaInfo](https://mediaarea.net/MediaInfo)** - Technical file information
- **[FFprobe](https://ffmpeg.org/ffprobe.html)** - Command-line media analyzer
- **[Spek](https://www.spek.cc/)** - Acoustic spectrum analyzer

---

**Questions?** Ask in our [Telegram Community](https://t.me/RhythmSupport), [Discord Server](https://discord.gg/XjPyUYPQYc), or check the [FAQ](https://github.com/cromaguy/Rhythm/wiki/FAQ).
