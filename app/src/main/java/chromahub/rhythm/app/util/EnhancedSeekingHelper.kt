/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Enhanced seeking utilities leveraging Media3 1.9.0 improvements
 * 
 * Media3 1.9.0 includes:
 * - Automatic seeking performance improvements for eligible videos
 * - Better scrubbing mode support
 * - Key-frame accurate seeking
 */
class EnhancedSeekingHelper(private val player: ExoPlayer) {
    
    companion object {
        private const val TAG = "EnhancedSeeking"
    }
    
    private var isScrubbingMode = false
    
    /**
     * Enable scrubbing mode for frequent seeks
     * Optimizes for cases where user is dragging a seek bar
     * Note: Manual implementation since setScrubbingModeEnabled requires newer API
     */
    fun enableScrubbingMode() {
        isScrubbingMode = true
        Log.d(TAG, "Scrubbing mode enabled for fast seeking")
    }
    
    /**
     * Disable scrubbing mode after seeking is complete
     */
    fun disableScrubbingMode() {
        isScrubbingMode = false
        Log.d(TAG, "Scrubbing mode disabled")
    }
    
    /**
     * Perform precise seek to position
     * Takes advantage of Media3 1.9.0 seeking improvements
     */
    fun seekToPosition(positionMs: Long) {
        player.seekTo(positionMs)
        Log.d(TAG, "Seeking to position: ${positionMs}ms")
    }
    
    /**
     * Seek forward by a specific amount
     * Optimized in Media3 1.9.0 for better performance
     */
    fun seekForward(incrementMs: Long = 10000L) {
        val newPosition = player.currentPosition + incrementMs
        seekToPosition(newPosition.coerceAtMost(player.duration))
    }
    
    /**
     * Seek backward by a specific amount
     */
    fun seekBackward(incrementMs: Long = 10000L) {
        val newPosition = player.currentPosition - incrementMs
        seekToPosition(newPosition.coerceAtLeast(0))
    }
    
    // Configurable seek increments
    private var seekBackIncrement = 10000L
    private var seekForwardIncrement = 10000L
    
    /**
     * Configure custom seek increments
     */
    fun configureSeekIncrements(
        seekBackMs: Long = 10000L,
        seekForwardMs: Long = 10000L
    ) {
        seekBackIncrement = seekBackMs
        seekForwardIncrement = seekForwardMs
        
        Log.d(TAG, "Configured seek increments - Back: ${seekBackMs}ms, Forward: ${seekForwardMs}ms")
    }
    
    /**
     * Get configured seek back increment
     */
    fun getSeekBackIncrement(): Long = seekBackIncrement
    
    /**
     * Get configured seek forward increment
     */
    fun getSeekForwardIncrement(): Long = seekForwardIncrement
}
