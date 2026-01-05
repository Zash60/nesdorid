package com.example.nesdorid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File

class SaveStatesFragment : Fragment() {

    private lateinit var listView: ListView
    private val saveSlots = (1..10).map { "Slot $it" }
    private var selectedSlot: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_save_states, container, false)

        listView = view.findViewById(R.id.saveStatesList)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, saveSlots)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedSlot = position
            Toast.makeText(requireContext(), "Selected ${saveSlots[position]}", Toast.LENGTH_SHORT).show()
        }

        val saveButton: Button = view.findViewById(R.id.saveButton)
        val loadButton: Button = view.findViewById(R.id.loadButton)

        saveButton.setOnClickListener {
            if (selectedSlot != -1) {
                val retroCore = (requireActivity() as EmulatorActivity).retroCoreInstance
                val size = retroCore.retroSerializeSize()
                val data = ByteArray(size.toInt())
                if (retroCore.retroSerialize(data)) {
                    val savesDir = File(requireContext().filesDir, "saves")
                    savesDir.mkdirs()
                    val file = File(savesDir, "slot${selectedSlot + 1}.sav")
                    file.writeBytes(data)
                    Toast.makeText(requireContext(), "Saved to ${saveSlots[selectedSlot]}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Select a slot first", Toast.LENGTH_SHORT).show()
            }
        }

        loadButton.setOnClickListener {
            if (selectedSlot != -1) {
                val retroCore = (requireActivity() as EmulatorActivity).retroCoreInstance
                val savesDir = File(requireContext().filesDir, "saves")
                val file = File(savesDir, "slot${selectedSlot + 1}.sav")
                if (file.exists()) {
                    val data = file.readBytes()
                    if (retroCore.retroUnserialize(data)) {
                        Toast.makeText(requireContext(), "Loaded from ${saveSlots[selectedSlot]}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Load failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No save file for ${saveSlots[selectedSlot]}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Select a slot first", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}