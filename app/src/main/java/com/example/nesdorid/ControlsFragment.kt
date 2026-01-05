package com.example.nesdorid

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment

class ControlsFragment : Fragment() {

    private val PREFS_NAME = "controls_prefs"
    private val KEY_SENSITIVITY = "sensitivity"
    private val KEY_OPACITY = "opacity"
    private val KEY_DPAD_VISIBLE = "dpad_visible"
    private val KEY_A_VISIBLE = "a_visible"
    private val KEY_B_VISIBLE = "b_visible"
    private val KEY_START_VISIBLE = "start_visible"
    private val KEY_SELECT_VISIBLE = "select_visible"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controls, container, false)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Sensitivity
        val sensitivityBar: SeekBar = view.findViewById(R.id.sensitivityBar)
        val sensitivityText: TextView = view.findViewById(R.id.sensitivityText)
        val sensitivity = prefs.getInt(KEY_SENSITIVITY, 50)
        sensitivityBar.progress = sensitivity
        sensitivityText.text = "Sensitivity: $sensitivity"

        sensitivityBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivityText.text = "Sensitivity: $progress"
                prefs.edit().putInt(KEY_SENSITIVITY, progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Opacity
        val opacityBar: SeekBar = view.findViewById(R.id.opacityBar)
        val opacityText: TextView = view.findViewById(R.id.opacityText)
        val opacity = prefs.getInt(KEY_OPACITY, 100)
        opacityBar.progress = opacity
        opacityText.text = "Opacity: ${opacity}%"

        opacityBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                opacityText.text = "Opacity: ${progress}%"
                prefs.edit().putInt(KEY_OPACITY, progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Visibility switches
        val dpadSwitch: Switch = view.findViewById(R.id.dpadVisible)
        val aSwitch: Switch = view.findViewById(R.id.buttonAVisible)
        val bSwitch: Switch = view.findViewById(R.id.buttonBVisible)
        val startSwitch: Switch = view.findViewById(R.id.buttonStartVisible)
        val selectSwitch: Switch = view.findViewById(R.id.buttonSelectVisible)

        dpadSwitch.isChecked = prefs.getBoolean(KEY_DPAD_VISIBLE, true)
        aSwitch.isChecked = prefs.getBoolean(KEY_A_VISIBLE, true)
        bSwitch.isChecked = prefs.getBoolean(KEY_B_VISIBLE, true)
        startSwitch.isChecked = prefs.getBoolean(KEY_START_VISIBLE, true)
        selectSwitch.isChecked = prefs.getBoolean(KEY_SELECT_VISIBLE, true)

        dpadSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DPAD_VISIBLE, isChecked).apply()
        }
        aSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_A_VISIBLE, isChecked).apply()
        }
        bSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_B_VISIBLE, isChecked).apply()
        }
        startSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_START_VISIBLE, isChecked).apply()
        }
        selectSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SELECT_VISIBLE, isChecked).apply()
        }

        return view
    }
}