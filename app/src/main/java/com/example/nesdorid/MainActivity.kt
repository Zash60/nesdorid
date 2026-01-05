package com.example.nesdorid

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RomAdapter
    private val roms = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences

    private val pickRomLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val mimeType = contentResolver.getType(it)
                Log.d(TAG, "Selected ROM URI: $it, MIME type: $mimeType")
                if (mimeType == "application/octet-stream" || it.toString().endsWith(".nes")) {
                    roms.add(it.toString())
                    saveRoms()
                    adapter.notifyDataSetChanged()
                    Log.d(TAG, "ROM added successfully")
                } else {
                    Log.w(TAG, "Invalid ROM file selected")
                    Toast.makeText(this, "Invalid ROM file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting ROM", e)
                Toast.makeText(this, "Error selecting ROM: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: Log.d(TAG, "No ROM selected")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        sharedPreferences = getSharedPreferences("nesdorid_prefs", MODE_PRIVATE)
        loadRoms()

        recyclerView = findViewById(R.id.recyclerView)
        adapter = RomAdapter(roms) { romUriString ->
            val intent = Intent(this, EmulatorActivity::class.java)
            intent.putExtra("ROM_URI", romUriString)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2) // Default to grid

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            pickRomLauncher.launch("application/octet-stream") // For .nes files
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun loadRoms() {
        val romSet = sharedPreferences.getStringSet("roms", emptySet())
        roms.addAll(romSet ?: emptySet())
    }

    private fun saveRoms() {
        val editor = sharedPreferences.edit()
        editor.putStringSet("roms", roms.toSet())
        editor.apply()
    }

}