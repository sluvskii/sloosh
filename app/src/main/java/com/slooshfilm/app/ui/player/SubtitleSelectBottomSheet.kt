package com.slooshfilm.app.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.slooshfilm.app.R

class SubtitleSelectBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance() = SubtitleSelectBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // You can create a layout for this later
        return inflater.inflate(R.layout.sheet_subtitle_select, container, false)
    }
}
