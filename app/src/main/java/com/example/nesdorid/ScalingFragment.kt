package com.example.nesdorid

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment

class ScalingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scaling, container, false)

        val radioGroup: RadioGroup = view.findViewById(R.id.scalingGroup)
        val prefs = requireContext().getSharedPreferences("scaling_prefs", Context.MODE_PRIVATE)
        val currentMode = prefs.getInt("scaling_mode", 0) // default to fit screen
        when (currentMode) {
            0 -> radioGroup.check(R.id.radioFitScreen)
            1 -> radioGroup.check(R.id.radioAspectRatio)
            2 -> radioGroup.check(R.id.radioPixelPerfect)
        }
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioFitScreen -> 0
                R.id.radioAspectRatio -> 1
                R.id.radioPixelPerfect -> 2
                else -> 0
            }
            prefs.edit().putInt("scaling_mode", mode).apply()
            (activity as? EmulatorActivity)?.updateScaling()
        }

        return view
    }
}