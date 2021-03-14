package com.brouken.player.dtpv.youtube.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.brouken.player.R
import kotlinx.android.synthetic.main.yt_seconds_view.view.*

/**
 * Layout group which handles the icon animation while forwarding and rewinding.
 *
 * Since it's based on view's alpha the fading effect is more fluid (more YouTube-like) than
 * using static drawables, especially when [cycleDuration] is low.
 *
 * Used by [YouTubeOverlay][com.github.vkay94.dtpv.youtube.YouTubeOverlay].
 */
class SecondsView(context: Context?, attrs: AttributeSet?) :
    ConstraintLayout(context!!, attrs) {

    /**
     * Defines the duration for a full cycle of the triangle animation.
     * Each animation step takes 20% of it.
     */
    var cycleDuration: Long = 750L
        set(value) {
            firstAnimator.duration = value / 5
            secondAnimator.duration = value / 5
            thirdAnimator.duration = value / 5
            fourthAnimator.duration = value / 5
            fifthAnimator.duration = value / 5
            field = value
        }

    /**
     * Sets the `TextView`'s seconds text according to the device`s language.
     */
    var seconds: Int = 0
        set(value) {
            tv_seconds.text = context.resources.getQuantityString(
                R.plurals.quick_seek_x_second, value, value
            )
            field = value
        }

    /**
     * Mirrors the triangles depending on what kind of type should be used (forward/rewind).
     */
    var isForward: Boolean = true
        set(value) {
            triangle_container.rotation = if (value) 0f else 180f
            field = value
        }

    val textView: TextView
        get() = tv_seconds

    @DrawableRes
    var icon: Int = R.drawable.ic_play_triangle
        set(value) {
            if (value > 0) {
                icon_1.setImageResource(value)
                icon_2.setImageResource(value)
                icon_3.setImageResource(value)
            }
            field = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.yt_seconds_view, this, true)
    }

    /**
     * Starts the triangle animation
     */
    fun start() {
        stop()
        firstAnimator.start()
    }

    /**
     * Stops the triangle animation
     */
    fun stop() {
        firstAnimator.cancel()
        secondAnimator.cancel()
        thirdAnimator.cancel()
        fourthAnimator.cancel()
        fifthAnimator.cancel()
        reset()
    }

    private fun reset() {
        icon_1.alpha = 0f
        icon_2.alpha = 0f
        icon_3.alpha = 0f
    }

    private val firstAnimator: ValueAnimator = CustomValueAnimator(
        {
            icon_1.alpha = 0f
            icon_2.alpha = 0f
            icon_3.alpha = 0f
        }, {
            icon_1.alpha = it
        }, {
            secondAnimator.start()
        }
    )

    private val secondAnimator: ValueAnimator = CustomValueAnimator(
        {
            icon_1.alpha = 1f
            icon_2.alpha = 0f
            icon_3.alpha = 0f
        }, {
            icon_2.alpha = it
        }, {
            thirdAnimator.start()
        }
    )

    private val thirdAnimator: ValueAnimator = CustomValueAnimator(
        {
            icon_1.alpha = 1f
            icon_2.alpha = 1f
            icon_3.alpha = 0f
        }, {
            icon_1.alpha = 1f - icon_3.alpha // or 1f - it (t3.alpha => all three stay a little longer together)
            icon_3.alpha = it
        }, {
            fourthAnimator.start()
        }
    )

    private val fourthAnimator: ValueAnimator = CustomValueAnimator(
        {
            icon_1.alpha = 0f
            icon_2.alpha = 1f
            icon_3.alpha = 1f
        }, {
            icon_2.alpha = 1f - it
        }, {
            fifthAnimator.start()
        }
    )

    private val fifthAnimator: ValueAnimator = CustomValueAnimator(
        {
            icon_1.alpha = 0f
            icon_2.alpha = 0f
            icon_3.alpha = 1f
        }, {
            icon_3.alpha = 1f - it
        }, {
            firstAnimator.start()
        }
    )

    private inner class CustomValueAnimator(
        start: () -> Unit, update: (value: Float) -> Unit, end: () -> Unit
    ): ValueAnimator() {

        init {
            duration = cycleDuration / 5
            setFloatValues(0f, 1f)

            addUpdateListener { update(it.animatedValue as Float) }
            doOnStart { start() }
            doOnEnd { end() }
        }
    }
}