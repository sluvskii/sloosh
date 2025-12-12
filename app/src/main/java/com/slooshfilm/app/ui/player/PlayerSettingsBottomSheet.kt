package com.slooshfilm.app.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.slooshfilm.app.R

class PlayerSettingsBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onQualitySelected(q: String)
        fun onSpeedSelected(speed: Float)
    }

    private var listener: Listener? = null
    fun setOnOptionSelectedListener(l: Listener) { listener = l }

    companion object {
        fun newInstance() = PlayerSettingsBottomSheet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.sheet_player_settings, container, false)
        val qualityGroup = v.findViewById<RadioGroup>(R.id.rg_quality)
        val speedGroup = v.findViewById<RadioGroup>(R.id.rg_speed)

        qualityGroup.setOnCheckedChangeListener { _, checkedId ->
            val q = when (checkedId) {
                R.id.q_1080 -> "1080"
                R.id.q_720 -> "720"
                R.id.q_480 -> "480"
                else -> "auto"
            }
            listener?.onQualitySelected(q)
            dismiss()
        }

        speedGroup.setOnCheckedChangeListener { _, checkedId ->
            val s = when (checkedId) {
                R.id.s_0_5 -> 0.5f
                R.id.s_1_5 -> 1.5f
                R.id.s_2_0 -> 2f
                else -> 1f
            }
            listener?.onSpeedSelected(s)
            dismiss()
        }

        return v
    }
}
