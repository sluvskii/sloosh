package com.slooshfilm.app.ui.sficons

import androidx.annotation.DrawableRes
import com.slooshfilm.app.R

/**
 * Maps exact Apple SF Symbol names to the project's vendored vector drawables
 * (res/drawable/ic_sf_*.xml). Used by [GlasmioResolvedIcon] when the symbol font
 * does not cover a glyph, so the app never falls back to a foreign icon set.
 *
 * Every drawable in res/drawable/ic_sf_* must appear here; the drawable for a name
 * is a hand-copied outline/fill variant whose geometry matches the Apple name.
 */
object SlooshIconFallback {

    private val map = HashMap<String, Int>(64).apply {
        put("antenna.radiowaves.left.and.right", R.drawable.ic_sf_antenna_radiowaves)
        put("arrow.down.circle", R.drawable.ic_sf_arrow_down_circle)
        put("arrow.down.circle.fill", R.drawable.ic_sf_arrow_down_circle_fill)
        put("arrow.down.to.line", R.drawable.ic_sf_arrow_down_to_line)
        put("backward.end.fill", R.drawable.ic_sf_backward_end_fill)
        put("backward.fill", R.drawable.ic_sf_backward_fill)
        put("bookmark.fill", R.drawable.ic_sf_bookmark_fill)
        put("bubble.fill", R.drawable.ic_sf_bubble_fill)
        put("captions.bubble.fill", R.drawable.ic_sf_captions_bubble_fill)
        put("checkmark", R.drawable.ic_sf_checkmark)
        put("checkmark.circle.fill", R.drawable.ic_sf_checkmark_circle_fill)
        put("chevron.down", R.drawable.ic_sf_chevron_down)
        put("chevron.left", R.drawable.ic_sf_chevron_left)
        put("chevron.right", R.drawable.ic_sf_chevron_right)
        put("chevron.up.chevron.down", R.drawable.ic_sf_chevron_up_chevron_down)
        put("clock.arrow.circlepath", R.drawable.ic_sf_clock_arrow_circlepath)
        put("doc.text.fill", R.drawable.ic_sf_doc_text_fill)
        put("dock.rectangle", R.drawable.ic_sf_dock_rectangle)
        put("envelope.fill", R.drawable.ic_sf_envelope_fill)
        put("exclamationmark.triangle.fill", R.drawable.ic_sf_exclamationmark_triangle_fill)
        put("eye", R.drawable.ic_sf_eye)
        put("eye.slash", R.drawable.ic_sf_eye_slash)
        put("film.fill", R.drawable.ic_sf_film_fill)
        put("flame.fill", R.drawable.ic_sf_flame_fill)
        put("forward.end.fill", R.drawable.ic_sf_forward_end_fill)
        put("forward.fill", R.drawable.ic_sf_forward_fill)
        put("gauge.with.needle", R.drawable.ic_sf_gauge_with_needle)
        put("gearshape", R.drawable.ic_sf_gearshape)
        put("gearshape.fill", R.drawable.ic_sf_gearshape_fill)
        put("gobackward.10", R.drawable.ic_sf_gobackward_10)
        put("goforward.10", R.drawable.ic_sf_goforward_10)
        put("heart", R.drawable.ic_sf_heart)
        put("heart.fill", R.drawable.ic_sf_heart_fill)
        put("house.fill", R.drawable.ic_sf_house_fill)
        put("info.circle.fill", R.drawable.ic_sf_info_circle_fill)
        put("internaldrive.fill", R.drawable.ic_sf_internaldrive_fill)
        put("lock.fill", R.drawable.ic_sf_lock_fill)
        put("lock.open", R.drawable.ic_sf_lock_open)
        put("magnifyingglass", R.drawable.ic_sf_magnifyingglass)
        put("minus", R.drawable.ic_sf_minus)
        put("paintpalette.fill", R.drawable.ic_sf_paintpalette_fill)
        put("pause.fill", R.drawable.ic_sf_pause_fill)
        put("person.fill", R.drawable.ic_sf_person_fill)
        put("play.fill", R.drawable.ic_sf_play_fill)
        put("play.rectangle.fill", R.drawable.ic_sf_play_rectangle_fill)
        put("plus", R.drawable.ic_sf_plus)
        put("rectangle.grid.1x2", R.drawable.ic_sf_rectangle_grid_1x2)
        put("rectangle.portrait.and.arrow.right", R.drawable.ic_sf_rectangle_portrait_and_arrow_right)
        put("rectangle.stack.fill", R.drawable.ic_sf_rectangle_stack_fill)
        put("slider.horizontal.3", R.drawable.ic_sf_slider_horizontal_3)
        put("sparkles", R.drawable.ic_sf_sparkles)
        put("speaker.slash.fill", R.drawable.ic_sf_speaker_slash_fill)
        put("speaker.wave.1.fill", R.drawable.ic_sf_speaker_wave_1_fill)
        put("speaker.wave.3.fill", R.drawable.ic_sf_speaker_wave_3_fill)
        put("square.and.arrow.up", R.drawable.ic_sf_square_arrow_up)
        put("square.and.pencil", R.drawable.ic_sf_square_and_pencil)
        put("square.grid.2x2", R.drawable.ic_sf_square_grid_2x2)
        put("star.fill", R.drawable.ic_sf_star_fill)
        put("sun.max.fill", R.drawable.ic_sf_sun_max_fill)
        put("sun.min.fill", R.drawable.ic_sf_sun_min_fill)
        put("trash.fill", R.drawable.ic_sf_trash_fill)
        put("tv.fill", R.drawable.ic_sf_tv_fill)
        put("waveform", R.drawable.ic_sf_waveform)
        put("xmark", R.drawable.ic_sf_xmark)
    }

    @DrawableRes
    fun resIdOrNull(systemName: String): Int? = map[systemName]
}
