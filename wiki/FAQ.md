# Frequently Asked Questions (FAQ)

Common questions about Rhythm Music Player.

## 📥 Installation & Updates

### Where can I download Rhythm?

Download from:
- **[F-Droid](https://f-droid.org/packages/chromahub.rhythm.app)** (official F-Droid repository)
- **[GitHub Releases](https://github.com/cromaguy/Rhythm/releases/latest)** (direct APK)
- **[IzzyOnDroid F-Droid Repo](https://apt.izzysoft.de/fdroid/index/apk/chromahub.rhythm.app)**
- **[Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/cromaguy/Rhythm/)**

### Is Rhythm available on Google Play Store?

Yes — very soon! Rhythm is coming to the Google Play Store. Currently it's distributed through GitHub, F-Droid, IzzyOnDroid, and Obtainium while Play Store review is finalized.

### How do I update Rhythm?

- **F-Droid**: Automatic updates through the F-Droid app
- **Obtainium**: Automatic notifications and one-tap updates
- **F-Droid (IzzyOnDroid)**: Automatic updates through the F-Droid app
- **Manual**: Download new APK from GitHub Releases and install over existing app

### Why can't I install the update?

If you get "App not installed" error:
- **Different sources have different signatures**. Stick to one installation method.
- If switching sources, uninstall first, then install fresh.

---

## 🎵 Audio & Playback

### What audio formats does Rhythm support?

**Fully supported:**
- Lossless: FLAC, ALAC, WAV, PCM
- Lossy: MP3, AAC, Opus, Vorbis

**Device-dependent:**
- Dolby: AC-3, E-AC-3, Atmos (requires hardware support)
- DTS (requires hardware support)

**Not supported:**
- APE, DSD/DSF, MQA, AIFF, WMA Lossless

📖 **Full details:** [Audio Formats Guide](https://github.com/cromaguy/Rhythm/wiki/Audio-Formats)

### Why won't my music files play?

Common reasons:
1. **Unsupported format** - Convert to FLAC or MP3
2. **Corrupted file** - Try playing in VLC to verify
3. **Missing permissions** - Grant storage access
4. **Wrong file location** - Ensure files are in accessible folders

### Does Rhythm support gapless playback?

Yes! Gapless playback works for FLAC, MP3, AAC, and Opus formats.

### What is Replay Gain?

Replay Gain normalizes the perceived loudness of your music. Choose **Album Gain** to maintain dynamic range within albums, or **Track Gain** for consistent loudness across all songs. Configure in Settings → Audio → Replay Gain.

### Does Rhythm support crossfade?

Yes! Crossfade smoothly blends the end of one track into the start of the next with an adjustable duration of 1–12 seconds. Configure in Settings → Audio → Crossfade.

### Can I use the equalizer?

Yes! Rhythm includes:
- 10-band professional equalizer
- 6032+ AutoEQ device-optimized presets
- Import/export custom profiles
- Bass boost and virtualizer effects

---

## 🎤 Lyrics

### How do I get synchronized lyrics?

Rhythm fetches lyrics from:
1. **LRCLib** (online, community-driven)
2. **Embedded lyrics** (from audio file metadata)

Enable in: Settings → Lyrics → Enable synced lyrics

### Lyrics are out of sync. How do I fix them?

- Tap lyrics settings icon in player
- Use +/- buttons to adjust timing
- Save offset for that specific track

### Can I add my own lyrics?

Yes! Use a metadata editor like Mp3tag to embed `.lrc` format lyrics into your audio files.

---

## 🖼️ Album Art & Metadata

### Album art not showing?

Solutions:
1. **Embed artwork** in audio files using Mp3tag
2. **Enable online fetch**: Settings → Online Features → Fetch artwork
3. **Clear cache**: Settings → Apps → Rhythm → Storage → Clear cache
4. **Rescan library**: Settings → Library → Rescan Media

### Can I edit song metadata?

Yes! Rhythm supports full metadata editing:
- Song title, artist, album, genre
- Track number, year
- Album artwork
- Batch operations

Long-press song → Edit Info

Requires storage write permission.

### Why can't I edit some files?

- Files on SD card may require special permissions
- Some files may be read-only
- Try moving files to internal storage

---

## 📱 Permissions & Privacy

### Why does Rhythm need storage permission?

To access and play your music files. Rhythm uses scoped storage on Android 11+ for enhanced privacy.

### Does Rhythm collect my data?

**No.** Rhythm is 100% FOSS and privacy-focused:
- ✅ No analytics or tracking
- ✅ No personal data collection
- ✅ All data stored locally
- ✅ Internet only for optional features (lyrics, artwork)

### Can I use Rhythm completely offline?

Yes! Core functionality works without internet:
- Music playback
- Playlists
- Equalizer
- Widgets
- **Rhythm Go** downloaded streaming tracks

Internet only needed for:
- Online lyrics (LRCLib)
- Album artwork fetch (Deezer)
- App updates
- Streaming Mode server connection

---

## 📊 Features

### Does Rhythm have a sleep timer?

Yes! Access from player screen → Menu → Sleep Timer

### Can I create and manage playlists?

Yes! Full playlist support:
- Create/edit/delete playlists
- Reorder songs (drag & drop)
- Multi-select removal
- Import/export (M3U, PLS formats)
- Grid view
- Smart sorting options

### Does Rhythm support Android Auto?

Yes! Android Auto is supported in Rhythm.

### Is there a widget?

Yes! Multiple widgets available:
- **Glance widgets**: 6 responsive Material 3 layouts (2x1 to 4x4)
- **Legacy widgets**: 7 RemoteViews layouts (2x1 to 5x5)
- **Lyrics widget**: Display current song lyrics on home screen
- **Real-time updates**
- **Playback controls**
- **Customizable appearance**

### Can I customize the theme?

Yes! Comprehensive theming:
- Light/Dark/System modes
- Material You dynamic colors (Android 12+)
- Custom color schemes
- Font selection
- Material Symbols icons
- Festive seasonal themes

### Is there an A–Z scroll bar?

Yes! When your library exceeds a threshold, an alphabetical scroll bar appears on the right side of Songs, Albums, and Artists tabs. Drag your finger along the letters to jump to items starting with that letter.

### What is Rhythm Go?

Rhythm Go lets you download tracks from your streaming server (Subsonic, Navidrome, Jellyfin) for offline playback. Downloaded tracks appear in a dedicated section and work without internet access. Configure in Settings → Streaming → Rhythm Go.

---

## 🔧 Troubleshooting

### App keeps crashing. What should I do?

1. Update to latest version
2. Clear app cache (not data)
3. Check storage space (need 500MB+ free)
4. Restart device
5. If persists, report on [GitHub Issues](https://github.com/cromaguy/Rhythm/issues)

### Music library is empty after scanning?

1. Grant storage permission: Settings → Apps → Rhythm → Permissions
2. Check blacklist/whitelist: Rhythm Settings → Library → Filters
3. Verify music location (standard folders like /Music)
4. Rescan media: Settings → Library → Rescan Media

### Widget not updating?

1. Remove and re-add widget
2. Disable battery optimization: Settings → Apps → Rhythm → Battery → Unrestricted
3. Grant notification permission
4. Update to latest version

### Bluetooth auto-play not working?

1. Enable: Rhythm Settings → Playback → Resume on Bluetooth
2. Grant Bluetooth permission
3. Ensure device is properly paired
4. Check device-specific settings

---

## 🌐 Online Features

### What is LRCLib?

LRCLib is a community-driven synced lyrics database. Rhythm fetches synchronized lyrics from it when available.

### Does Rhythm upload my listening data?

**No.** All statistics and data remain on your device. Nothing is uploaded to servers.

### Why does Rhythm need internet access?

Only for optional features:
- **LRCLib** - Synced lyrics
- **Deezer API** - Album artwork
- **YouTube Music API** - Album artwork lookup
- **GitHub** - Update checking

You can block internet (via firewall) for completely offline use.

---

## 💾 Backup & Data

### How do I backup my playlists?

Settings → Backup & Restore → Create Backup

Or export individual playlists as M3U/PLS files.

### Will I lose data when updating?

No! Updates preserve:
- Playlists
- Settings
- Statistics
- Metadata edits

Always install updates over existing app (don't uninstall first).

### Can I transfer data to a new device?

1. Backup on old device
2. Copy backup file to new device
3. Install Rhythm on new device
4. Settings → Backup & Restore → Restore from Backup

---

## 🤝 Contributing

### How can I contribute?

- Report bugs on [GitHub Issues](https://github.com/cromaguy/Rhythm/issues)
- Suggest features
- Submit pull requests
- Help translate
- Join [Telegram community](https://t.me/RhythmSupport)

See [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing) for details.

### Is the source code available?

Yes! Rhythm is fully open source under GPL-3.0 license:
**[GitHub Repository](https://github.com/cromaguy/Rhythm)**

### Can I fork and modify Rhythm?

Yes, under GPL-3.0 terms:
- Keep it open source
- Credit original project
- Share modifications under same license

---

## 📱 Compatibility

### What Android version is required?

**Android 8.0+ (API 26)**

### Does Rhythm work on tablets?

Yes! Rhythm has optimized **multi-pane layouts** for tablets:
- Library on the left, content on the right
- Optimized player, settings, and library views
- Portrait and landscape support
- Also works on foldables

### Does it support split-screen?

Yes, Rhythm works in split-screen/multi-window mode.

---

## 🆘 Getting Help

### Where can I get support?

- **[Telegram Community](https://t.me/RhythmSupport)** - Live help
- **[Discord Server](https://discord.gg/XjPyUYPQYc)** - Community chat
- **[GitHub Discussions](https://github.com/cromaguy/Rhythm/discussions)** - Q&A forum
- **[GitHub Issues](https://github.com/cromaguy/Rhythm/issues)** - Bug reports
- **[Wiki](https://github.com/cromaguy/Rhythm/wiki)** - Documentation

### How do I report a bug?

1. Check if it's already reported: [GitHub Issues](https://github.com/cromaguy/Rhythm/issues)
2. If not, create new issue with:
   - Android version
   - Device model
   - Rhythm version
   - Steps to reproduce
   - Screenshots if applicable

---

## 🔮 Future Plans

### Is Android Auto support planned?

Android Auto is already supported in Rhythm!

### Will there be a desktop version?

Currently focused on Android. Desktop version not planned at this time.

### When is the next release?

Check [GitHub Releases](https://github.com/cromaguy/Rhythm/releases) for updates. Follow development on [Telegram](https://t.me/RhythmSupport).

---

**Question not answered?** Ask in our [Telegram Community](https://t.me/RhythmSupport)!
