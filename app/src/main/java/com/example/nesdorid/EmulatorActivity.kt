package com.example.nesdorid

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EmulatorActivity : AppCompatActivity() {

    private val TAG = "EmulatorActivity"
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var retroCore: RetroCore
    private lateinit var renderer: RetroRenderer
    val retroCoreInstance: RetroCore get() = retroCore
    private var romUriString: String? = null
    private val choreographer = Choreographer.getInstance()
    private var isEmulating = false
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isEmulating) {
                glSurfaceView.requestRender()
                choreographer.postFrameCallback(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emulator)

        romUriString = intent.getStringExtra("ROM_URI")

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                setupControls()
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_save_states -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, SaveStatesFragment())
                        .commit()
                }
                R.id.nav_cheats -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CheatsFragment())
                        .commit()
                }
                R.id.nav_scaling -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ScalingFragment())
                        .commit()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        val menuButton: ImageButton = findViewById(R.id.menuButton)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Setup GLSurfaceView
        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // RGBA8888 with depth
        renderer = RetroRenderer()
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        // Initialize RetroCore
        retroCore = RetroCore()
        retroCore.retroInit()
        Log.d(TAG, "RetroCore initialized")
        romUriString?.let {
            try {
                val uri = Uri.parse(it)
                Log.d(TAG, "Loading ROM from URI: $it")
                val inputStream = contentResolver.openInputStream(uri)
                val data = inputStream?.readBytes()
                inputStream?.close()
                if (data != null) {
                    Log.d(TAG, "ROM data loaded, size: ${data.size}")
                    val gameInfo = RetroGameInfo(path = null, data = data, size = data.size.toLong())
                    val loadResult = retroCore.retroLoadGame(gameInfo)
                    if (loadResult) {
                        Log.d(TAG, "Game loaded successfully")
                        loadAndApplyCheats()
                        // Start emulation at 60 FPS
                        isEmulating = true
                        choreographer.postFrameCallback(frameCallback)
                    } else {
                        Log.e(TAG, "Failed to load game")
                        finish()
                    }
                } else {
                    Log.e(TAG, "Failed to read ROM data")
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ROM", e)
                finish()
            }
        } ?: Log.w(TAG, "No ROM URI provided")

        // Setup controls
        setupControls()
    }

    private fun setupControls() {
        val prefs = getSharedPreferences("controls_prefs", Context.MODE_PRIVATE)

        val dpad: ImageButton = findViewById(R.id.dpad)
        val buttonA: ImageButton = findViewById(R.id.buttonA)
        val buttonB: ImageButton = findViewById(R.id.buttonB)
        val buttonStart: View = findViewById(R.id.buttonStart)
        val buttonSelect: View = findViewById(R.id.buttonSelect)

        // Apply opacity
        val opacity = prefs.getInt("opacity", 100) / 100f
        dpad.alpha = opacity
        buttonA.alpha = opacity
        buttonB.alpha = opacity
        buttonStart.alpha = opacity
        buttonSelect.alpha = opacity

        // Apply visibility
        dpad.visibility = if (prefs.getBoolean("dpad_visible", true)) View.VISIBLE else View.GONE
        buttonA.visibility = if (prefs.getBoolean("a_visible", true)) View.VISIBLE else View.GONE
        buttonB.visibility = if (prefs.getBoolean("b_visible", true)) View.VISIBLE else View.GONE
        buttonStart.visibility = if (prefs.getBoolean("start_visible", true)) View.VISIBLE else View.GONE
        buttonSelect.visibility = if (prefs.getBoolean("select_visible", true)) View.VISIBLE else View.GONE

        // TODO: Implement touch listeners for NES inputs
        Log.d(TAG, "Controls setup, but touch listeners not implemented")
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        // Stop emulation when app is backgrounded for battery optimization
        isEmulating = false
        choreographer.removeFrameCallback(frameCallback)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        // Resume emulation if game was loaded
        if (romUriString != null && !isEmulating) {
            isEmulating = true
            choreographer.postFrameCallback(frameCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop emulation
        isEmulating = false
        choreographer.removeFrameCallback(frameCallback)
        // Recycle bitmap to reduce memory usage
        renderer.recycleBitmap()
        saveCheats()
        retroCore.retroUnloadGame()
        retroCore.retroDeinit()
    }

    private fun loadAndApplyCheats() {
        val cheatsDir = File(filesDir, "cheats")
        val cheatFile = File(cheatsDir, getCheatFileName(romUriString ?: ""))
        if (cheatFile.exists()) {
            try {
                val json = cheatFile.readText()
                val type = object : TypeToken<List<Cheat>>() {}.type
                val loadedCheats = Gson().fromJson<List<Cheat>>(json, type)
                Log.d(TAG, "Loaded ${loadedCheats.size} cheats")
                // Apply loaded cheats in batch for optimization
                retroCore.retroCheatSetBatch(loadedCheats.toTypedArray())
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cheats", e)
            }
        } else {
            Log.d(TAG, "No cheat file found")
        }
    }

    private fun saveCheats() {
        // Since cheats are saved in the fragment when modified, this is a backup
        // But to ensure, we can load current cheats if fragment is active
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? CheatsFragment
        if (fragment != null) {
            // Fragment is active, it already saves
        } else {
            // If not active, load and save? But since it's saved on change, perhaps no need
        }
    }

    private fun getCheatFileName(romUriString: String): String {
        val uri = Uri.parse(romUriString)
        val displayName = uri.lastPathSegment ?: "unknown"
        return displayName.substringBeforeLast('.') + ".json"
    }

    fun updateScaling() {
        renderer.updateViewport(glSurfaceView.width, glSurfaceView.height)
    }

    inner class RetroRenderer : GLSurfaceView.Renderer {
        private var videoBitmap: Bitmap? = null

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            // TODO: Initialize OpenGL
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            applyScaling(gl, width, height)
        }

        fun updateViewport(width: Int, height: Int) {
            applyScaling(null, width, height)
        }

        private fun applyScaling(gl: GL10?, width: Int, height: Int) {
            val avInfo = retroCore.retroGetSystemAvInfo()
            val videoWidth = avInfo.geometry.baseWidth
            val videoHeight = avInfo.geometry.baseHeight
            val prefs = getSharedPreferences("scaling_prefs", Context.MODE_PRIVATE)
            val scalingMode = prefs.getInt("scaling_mode", 0)

            when (scalingMode) {
                0 -> { // Fit Screen
                    gl?.glViewport(0, 0, width, height)
                }
                1 -> { // Maintain Aspect Ratio
                    val scale = minOf(width.toFloat() / videoWidth, height.toFloat() / videoHeight)
                    val scaledWidth = (videoWidth * scale).toInt()
                    val scaledHeight = (videoHeight * scale).toInt()
                    val x = (width - scaledWidth) / 2
                    val y = (height - scaledHeight) / 2
                    gl?.glViewport(x, y, scaledWidth, scaledHeight)
                }
                2 -> { // Pixel Perfect
                    val scale = minOf(width / videoWidth, height / videoHeight)
                    val integerScale = scale.coerceAtMost(8) // limit to 8x for high-res
                    val scaledWidth = videoWidth * integerScale
                    val scaledHeight = videoHeight * integerScale
                    val x = (width - scaledWidth) / 2
                    val y = (height - scaledHeight) / 2
                    gl?.glViewport(x, y, scaledWidth, scaledHeight)
                }
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            retroCore.retroRun()
            Log.d(TAG, "Rendering frame, but video not displayed")
            // TODO: Render video with bitmap recycling for memory optimization
            // When implementing video rendering, ensure to recycle old bitmaps:
            // videoBitmap?.recycle()
            // videoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        }

        fun recycleBitmap() {
            videoBitmap?.recycle()
            videoBitmap = null
        }
    }
}