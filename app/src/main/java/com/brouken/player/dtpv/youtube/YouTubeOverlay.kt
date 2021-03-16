package com.brouken.player.dtpv.youtube

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.brouken.player.R
import com.brouken.player.dtpv.DoubleTapPlayerView
import com.brouken.player.dtpv.PlayerDoubleTapListener
import com.brouken.player.dtpv.SeekListener
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.android.synthetic.main.yt_overlay.view.*


/**
 * Overlay for [DoubleTapPlayerView] to create a similar UI/UX experience like the official
 * YouTube Android app.
 *
 * The overlay has the typical YouTube scaling circle animation and provides some configurations
 * which can't be accomplished with the regular Android Ripple (I didn't find any options in the
 * documentation ...).
 */
class YouTubeOverlay(context: Context?, private val attrs: AttributeSet?) :
    ConstraintLayout(context!!, attrs), PlayerDoubleTapListener {

    constructor(context: Context?) : this(context, null) {
        // Hide overlay initially when added programmatically
        this.visibility = View.INVISIBLE
    }

    private var playerViewRef: Int = -1

    // Player behaviors
    private var playerView: DoubleTapPlayerView? = null
    private var player: SimpleExoPlayer? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.yt_overlay, this, true)

        // Initialize UI components
        initializeAttributes()
        seconds_view.isForward = true
        changeConstraints(true)

        // This code snippet is executed when the circle scale animation is finished
        circle_clip_tap_view.performAtEnd = {
            performListener?.onAnimationEnd()

            seconds_view.visibility = View.INVISIBLE
            seconds_view.seconds = 0
            seconds_view.stop()
        }
    }

    /**
     * Sets all optional XML attributes and defaults
     */
    private fun initializeAttributes() {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs,
                R.styleable.YouTubeOverlay, 0, 0)

            // PlayerView => see onAttachToWindow
            playerViewRef = a.getResourceId(R.styleable.YouTubeOverlay_yt_playerView, -1)

            // Durations
            animationDuration = a.getInt(
                R.styleable.YouTubeOverlay_yt_animationDuration, 650).toLong()

            seekSeconds = a.getInt(
                R.styleable.YouTubeOverlay_yt_seekSeconds, 10)

            iconAnimationDuration = a.getInt(
                R.styleable.YouTubeOverlay_yt_iconAnimationDuration, 750).toLong()

            // Arc size
            arcSize = a.getDimensionPixelSize(
                R.styleable.YouTubeOverlay_yt_arcSize,
                context.resources.getDimensionPixelSize(R.dimen.dtpv_yt_arc_size)).toFloat()

            // Colors
            tapCircleColor = a.getColor(
                R.styleable.YouTubeOverlay_yt_tapCircleColor,
                ContextCompat.getColor(context, R.color.dtpv_yt_tap_circle_color)
            )

            circleBackgroundColor = a.getColor(
                R.styleable.YouTubeOverlay_yt_backgroundCircleColor,
                ContextCompat.getColor(context, R.color.dtpv_yt_background_circle_color)
            )

            // Seconds TextAppearance
            textAppearance = a.getResourceId(
                R.styleable.YouTubeOverlay_yt_textAppearance,
                R.style.YTOSecondsTextAppearance)

            // Seconds icon
            icon = a.getResourceId(
                R.styleable.YouTubeOverlay_yt_icon,
                R.drawable.ic_play_triangle
            )

            a.recycle()

        } else {
            // Set defaults
            arcSize = context.resources.getDimensionPixelSize(R.dimen.dtpv_yt_arc_size).toFloat()
            tapCircleColor = ContextCompat.getColor(context, R.color.dtpv_yt_tap_circle_color)
            circleBackgroundColor = ContextCompat.getColor(context, R.color.dtpv_yt_background_circle_color)
            animationDuration = 650
            iconAnimationDuration = 750
            seekSeconds = 10
            textAppearance = R.style.YTOSecondsTextAppearance
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // If the PlayerView is set by XML then call the corresponding setter method
        if (playerViewRef != -1)
            playerView((this.parent as View).findViewById(playerViewRef) as DoubleTapPlayerView)
    }

    /**
     * Obligatory call if playerView is not set via XML!
     *
     * Links the DoubleTapPlayerView to this view for recognizing the tapped position.
     *
     * @param playerView PlayerView which triggers the event
     */
    fun playerView(playerView: DoubleTapPlayerView) = apply {
        this.playerView = playerView
    }

    /**
     * Obligatory call! Needs to be called whenever the Player changes.
     *
     * Performs seekTo-calls on the ExoPlayer's Player instance.
     *
     * @param player PlayerView which triggers the event
     */
    fun player(player: SimpleExoPlayer) = apply {
        this.player = player
    }

    /*
        Properties
     */

    private var seekListener: SeekListener? = null

    /**
     * Optional: Sets a listener to observe whether double tap reached the start / end of the video
     */
    fun seekListener(listener: SeekListener) = apply {
        seekListener = listener
    }

    private var performListener: PerformListener? = null

    /**
     * Sets a listener to execute some code before and after the animation
     * (for example UI changes (hide and show views etc.))
     */
    fun performListener(listener: PerformListener) = apply {
        performListener = listener
    }

    /**
     * Forward / rewind duration on a tap in seconds.
     */
    var seekSeconds: Int = 0
        private set

    fun seekSeconds(seconds: Int) = apply {
        seekSeconds = seconds
    }

    /**
     * Color of the scaling circle on touch feedback.
     */
    var tapCircleColor: Int
        get() = circle_clip_tap_view.circleColor
        private set(value) {
            circle_clip_tap_view.circleColor = value
        }

    fun tapCircleColorRes(@ColorRes resId: Int) = apply {
        tapCircleColor = ContextCompat.getColor(context, resId)
    }

    fun tapCircleColorInt(@ColorInt color: Int) = apply {
        tapCircleColor = color
    }

    /**
     * Color of the clipped background circle
     */
    var circleBackgroundColor: Int
        get() = circle_clip_tap_view.circleBackgroundColor
        private set(value) {
            circle_clip_tap_view.circleBackgroundColor = value
        }

    fun circleBackgroundColorRes(@ColorRes resId: Int) = apply {
        circleBackgroundColor = ContextCompat.getColor(context, resId)
    }

    fun circleBackgroundColorInt(@ColorInt color: Int) = apply {
        circleBackgroundColor = color
    }

    /**
     * Duration of the circle scaling animation / speed in milliseconds.
     * The overlay keeps visible until the animation finishes.
     */
    var animationDuration: Long
        get() = circle_clip_tap_view.animationDuration
        private set(value) {
            circle_clip_tap_view.animationDuration = value
        }

    fun animationDuration(duration: Long) = apply {
        animationDuration = duration
    }

    /**
     * Size of the arc which will be clipped from the background circle.
     * The greater the value the more roundish the shape becomes
     */
    var arcSize: Float
        get() = circle_clip_tap_view.arcSize
        internal set(value) {
            circle_clip_tap_view.arcSize = value
        }

    fun arcSize(@DimenRes resId: Int) = apply {
        arcSize = context.resources.getDimension(resId)
    }

    fun arcSize(px: Float) = apply {
        arcSize = px
    }

    /**
     * Duration the icon animation (fade in + fade out) for a full cycle in milliseconds.
     */
    var iconAnimationDuration: Long = 750
        get() = seconds_view.cycleDuration
        private set(value) {
            seconds_view.cycleDuration = value
            field = value
        }

    fun iconAnimationDuration(duration: Long) = apply {
        iconAnimationDuration = duration
    }

    /**
     * One of the three forward icons which will be animated above the seconds indicator.
     * The rewind icon will be the 180° mirrored version.
     *
     * Keep in mind that padding on the left and right of the drawable will be rendered which
     * could result in additional space between the three icons.
     */
    @DrawableRes
    var icon: Int = 0
        get() = seconds_view.icon
        private set(value) {
            seconds_view.stop()
            seconds_view.icon = value
            field = value
        }

    fun icon(@DrawableRes resId: Int) = apply {
        icon = resId
    }

    /**
     * Text appearance of the *xx seconds* text.
     */
    @StyleRes
    var textAppearance: Int = 0
        private set(value) {
            TextViewCompat.setTextAppearance(seconds_view.textView, value)
            field = value
        }

    fun textAppearance(@StyleRes resId: Int) = apply {
        textAppearance = resId
    }

    /**
     * TextView view for *xx seconds*.
     *
     * In case of you'd like to change some specific attributes of the TextView in runtime.
     */
    val secondsTextView: TextView
        get() = seconds_view.textView

    override fun onDoubleTapStarted(posX: Float, posY: Float) {

        if (player?.currentPosition != null && playerView?.width != null) {
            if (posX >= playerView?.width!! * 0.35 && posX <= playerView?.width!! * 0.65) {
                if (player?.isPlaying!!) {
                    player?.pause()
                } else {
                    player?.play()
                    if (playerView?.isControllerFullyVisible!!)
                        playerView?.hideController()
                }
                return
            }
        }

        super.onDoubleTapStarted(posX, posY)
    }

    override fun onDoubleTapProgressUp(posX: Float, posY: Float) {

        // Check first whether forwarding/rewinding is "valid"
        if (player?.currentPosition == null || playerView?.width == null) return
        player?.currentPosition?.let { current ->
            // Rewind and start of the video (+ 0.5 sec tolerance)
            if (posX < playerView?.width!! * 0.35 && current <= 500)
                return

            // Forward and end of the video (- 0.5 sec tolerance)
            if (posX > playerView?.width!! * 0.65 && current >= player?.duration!!.minus(500))
                return
        }

        // YouTube behavior: show overlay on MOTION_UP
        // But check whether the first double tap is in invalid area
        if (this.visibility != View.VISIBLE) {
            if (posX < playerView?.width!! * 0.35 || posX > playerView?.width!! * 0.65) {
                performListener?.onAnimationStart()
                seconds_view.visibility = View.VISIBLE
                seconds_view.start()
            } else
                return
        }

        when {
            posX < playerView?.width!! * 0.35 -> {

                // First time tap or switched
                if (seconds_view.isForward) {
                    changeConstraints(false)
                    seconds_view.apply {
                        isForward = false
                        seconds = 0
                    }
                }

                // Cancel ripple and start new without triggering overlay disappearance
                // (resetting instead of ending)
                circle_clip_tap_view.resetAnimation {
                    circle_clip_tap_view.updatePosition(posX, posY)
                }
                rewinding()
            }
            posX > playerView?.width!! * 0.65 -> {

                // First time tap or switched
                if (!seconds_view.isForward) {
                    changeConstraints(true)
                    seconds_view.apply {
                        isForward = true
                        seconds = 0
                    }
                }

                // Cancel ripple and start new without triggering overlay disappearance
                // (resetting instead of ending)
                circle_clip_tap_view.resetAnimation {
                    circle_clip_tap_view.updatePosition(posX, posY)
                }
                forwarding()
            }
            else -> {
                // Middle area tapped: do nothing
                //
                // playerView?.cancelInDoubleTapMode()
                // circle_clip_tap_view.endAnimation()
                // triangle_seconds_view.stop()
            }
        }
    }

    /**
     * Seeks the video to desired position.
     * Calls interface functions when start reached ([SeekListener.onVideoStartReached])
     * or when end reached ([SeekListener.onVideoEndReached])
     *
     * @param newPosition desired position
     */
    private fun seekToPosition(newPosition: Long?) {
        if (newPosition == null) return

        player?.setSeekParameters(SeekParameters.EXACT);

        // Start of the video reached
        if (newPosition <= 0) {
            player?.seekTo(0)

            seekListener?.onVideoStartReached()
            return
        }

        // End of the video reached
        player?.duration?.let { total ->
            if (newPosition >= total) {
                player?.seekTo(total)

                seekListener?.onVideoEndReached()
                return
            }
        }

        // Otherwise
        playerView?.keepInDoubleTapMode()
        player?.seekTo(newPosition)
    }

    private fun forwarding() {
        seconds_view.seconds += seekSeconds
        seekToPosition(player?.currentPosition?.plus(seekSeconds * 1000))
    }

    private fun rewinding() {
        seconds_view.seconds += seekSeconds
        seekToPosition(player?.currentPosition?.minus(seekSeconds * 1000))
    }

    private fun changeConstraints(forward: Boolean) {
        val constraintSet = ConstraintSet()
        with(constraintSet) {
            clone(root_constraint_layout)
            if (forward) {
                clear(seconds_view.id, ConstraintSet.START)
                connect(seconds_view.id, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END)
            } else {
                clear(seconds_view.id, ConstraintSet.END)
                connect(seconds_view.id, ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START)
            }
            seconds_view.start()
            applyTo(root_constraint_layout)
        }
    }

    interface PerformListener {
        /**
         * Called when the overlay is not visible and onDoubleTapProgressUp event occurred.
         * Visibility of the overlay should be set to VISIBLE within this interface method.
         */
        fun onAnimationStart()

        /**
         * Called when the circle animation is finished.
         * Visibility of the overlay should be set to GONE within this interface method.
         */
        fun onAnimationEnd()
    }
}
