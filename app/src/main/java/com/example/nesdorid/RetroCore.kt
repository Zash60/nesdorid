package com.example.nesdorid

import android.util.Log

data class RetroGameInfo(
    val path: String? = null,
    val data: ByteArray? = null,
    val size: Long = 0,
    val meta: String? = null
)

data class RetroSystemAvInfo(
    val geometry: RetroGameGeometry,
    val timing: RetroSystemTiming
)

data class RetroGameGeometry(
    val baseWidth: Int,
    val baseHeight: Int,
    val maxWidth: Int,
    val maxHeight: Int,
    val aspectRatio: Float
)

data class RetroSystemTiming(
    val fps: Double,
    val sampleRate: Double
)

class RetroCore {

    companion object {
        private const val TAG = "RetroCore"

        init {
            try {
                System.loadLibrary("mesen")
                Log.d(TAG, "Mesen library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load Mesen library", e)
            }
        }
    }

    // Core lifecycle
    external fun retroInit()
    external fun retroDeinit()
    external fun retroLoadGame(game: RetroGameInfo): Boolean
    external fun retroUnloadGame()
    external fun retroRun()
    external fun retroReset()

    // Input
    external fun getInputState(port: Int, device: Int, index: Int, id: Int): Int
    external fun setInputState(port: Int, device: Int, index: Int, id: Int, state: Int)

    // Video
    external fun retroGetSystemAvInfo(): RetroSystemAvInfo

    // Audio
    external fun retroGetAudioSample(): Short
    external fun retroGetAudioSampleBatch(buffer: ShortArray, frames: Int): Int

    // Save states
    external fun retroSerializeSize(): Long
    external fun retroSerialize(data: ByteArray): Boolean
    external fun retroUnserialize(data: ByteArray): Boolean

    // Cheats
    external fun retroCheatSet(index: Int, enabled: Boolean, code: String?)
    external fun retroCheatSetBatch(cheats: Array<Cheat>)

    // High-res scaling (libretro extensions)
    external fun retroSetPixelFormat(format: Int)
    external fun retroGetRegion(): Int  // RETRO_REGION_NTSC or PAL

    // Additional extensions
    external fun retroSetControllerPortDevice(port: Int, device: Int)
    external fun retroGetMemorySize(id: Int): Long
    external fun retroGetMemoryData(id: Int): ByteArray?
}