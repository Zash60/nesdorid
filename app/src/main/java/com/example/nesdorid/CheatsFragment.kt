package com.example.nesdorid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class CheatsFragment : Fragment() {

    private lateinit var cheatsList: ListView
    private lateinit var addCheatButton: Button
    private lateinit var removeCheatButton: Button
    private val cheats = mutableListOf<Cheat>()
    private lateinit var adapter: ArrayAdapter<String>
    private var selectedPosition: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cheats, container, false)

        cheatsList = view.findViewById(R.id.cheatsList)
        addCheatButton = view.findViewById(R.id.addCheatButton)
        removeCheatButton = view.findViewById(R.id.removeCheatButton)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cheats.map { it.description })
        cheatsList.adapter = adapter

        loadCheats()

        cheatsList.setOnItemClickListener { _, _, position, _ ->
            selectedPosition = position
        }

        addCheatButton.setOnClickListener {
            showAddCheatDialog()
        }

        removeCheatButton.setOnClickListener {
            if (selectedPosition != -1) {
                cheats.removeAt(selectedPosition)
                adapter.clear()
                adapter.addAll(cheats.map { it.description })
                adapter.notifyDataSetChanged()
                saveCheats()
                selectedPosition = -1
            } else {
                Toast.makeText(requireContext(), "Select a cheat to remove", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showAddCheatDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_cheat, null)
        val codeEdit = dialogView.findViewById<EditText>(R.id.cheatCodeEdit)
        val descEdit = dialogView.findViewById<EditText>(R.id.cheatDescEdit)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Cheat")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val code = codeEdit.text.toString()
                val desc = descEdit.text.toString()
                if (code.isNotEmpty()) {
                    cheats.add(Cheat(code, desc))
                    adapter.clear()
                    adapter.addAll(cheats.map { it.description })
                    adapter.notifyDataSetChanged()
                    saveCheats()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadCheats() {
        val emulatorActivity = requireActivity() as EmulatorActivity
        val romUriString = emulatorActivity.intent.getStringExtra("ROM_URI") ?: return
        val cheatsDir = File(requireContext().filesDir, "cheats")
        val cheatFile = File(cheatsDir, getCheatFileName(romUriString))
        if (cheatFile.exists()) {
            try {
                val json = cheatFile.readText()
                val type = object : TypeToken<List<Cheat>>() {}.type
                val loadedCheats = Gson().fromJson<List<Cheat>>(json, type)
                cheats.addAll(loadedCheats)
                adapter.clear()
                adapter.addAll(cheats.map { it.description })
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun saveCheats() {
        val emulatorActivity = requireActivity() as EmulatorActivity
        val romUriString = emulatorActivity.intent.getStringExtra("ROM_URI") ?: return
        val cheatsDir = File(requireContext().filesDir, "cheats")
        cheatsDir.mkdirs()
        val cheatFile = File(cheatsDir, getCheatFileName(romUriString))
        val json = Gson().toJson(cheats)
        cheatFile.writeText(json)
        // Apply to core
        emulatorActivity.retroCoreInstance.retroCheatSetBatch(cheats.toTypedArray())
    }

    private fun getCheatFileName(romUriString: String): String {
        val uri = android.net.Uri.parse(romUriString)
        val displayName = uri.lastPathSegment ?: "unknown"
        return displayName.substringBeforeLast('.') + ".json"
    }
}