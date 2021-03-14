package com.brouken.player.dtpv

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.brouken.player.CustomStyledPlayerView
import com.brouken.player.R
import com.google.android.exoplayer2.ui.PlayerView

/**
 * Custom player class for Double-Tapping listening
 */
open class DoubleTapPlayerView @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CustomStyledPlayerView(context!!, attrs, defStyleAttr) {

    private val gestureDetector: GestureDetectorCompat
    private val gestureListener: DoubleTapGestureListener

    private var controller: PlayerDoubleTapListener? = null
        get() = gestureListener.controls
        set(value) {
            gestureListener.controls = value
            field = value
        }

    private var controllerRef: Int = -1

    init {
        gestureListener = DoubleTapGestureListener(this)
        gestureDetector = GestureDetectorCompat(context, gestureListener)

        // Check whether controller is set through XML
        attrs?.let {
            val a = context?.obtainStyledAttributes(attrs, R.styleable.DoubleTapPlayerView, 0,0)
            controllerRef = a?.getResourceId(R.styleable.DoubleTapPlayerView_dtpv_controller, -1) ?: -1

            a?.recycle()
        }
    }

    /**
     * If this field is set to `true` this view will handle double tapping, otherwise it will
     * handle touches the same way as the original [PlayerView][com.google.android.exoplayer2.ui.PlayerView] does
     */
    var isDoubleTapEnabled = true

    /**
     * Time window a double tap is active, so a followed tap is calling a gesture detector
     * method instead of normal tap (see [PlayerView.onTouchEvent])
     */
    var doubleTapDelay: Long = 700
        get() = gestureListener.doubleTapDelay
        set(value) {
            gestureListener.doubleTapDelay = value
            field = value
        }

    /**
     * Sets the [PlayerDoubleTapListener] which handles the gesture callbacks.
     *
     * Primarily used for [YouTubeOverlay][com.github.vkay94.dtpv.youtube.YouTubeOverlay]
     */
    fun controller(controller: PlayerDoubleTapListener) = apply { this.controller = controller }

    /**
     * Returns the current state of double tapping.
     */
    fun isInDoubleTapMode(): Boolean = gestureListener.isDoubleTapping

    /**
     * Resets the timeout to keep in double tap mode.
     *
     * Called once in [PlayerDoubleTapListener.onDoubleTapStarted]. Needs to be called
     * from outside if the double tap is customized / overridden to detect ongoing taps
     */
    fun keepInDoubleTapMode() {
        gestureListener.keepInDoubleTapMode()
    }

    /**
     * Cancels double tap mode instantly by calling [PlayerDoubleTapListener.onDoubleTapFinished]
     */
    fun cancelInDoubleTapMode() {
        gestureListener.cancelInDoubleTapMode()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isDoubleTapEnabled) {
            val consumed = gestureDetector.onTouchEvent(ev)

            // Do not trigger original behavior when double tapping
            // otherwise the controller would show/hide - it would flack
            if (!consumed)
                return super.onTouchEvent(ev)

            return true
        }
        return super.onTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // If the PlayerView is set by XML then call the corresponding setter method
        if (controllerRef != -1) {
            try {
                val view = (this.parent as View).findViewById(controllerRef) as View
                if (view is PlayerDoubleTapListener) {
                    controller(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("DoubleTapPlayerView",
                    "controllerRef is either invalid or not PlayerDoubleTapListener: ${e.message}")
            }
        }

    }

    /**
     * Gesture Listener for double tapping
     *
     * For more information which methods are called in certain situations look for
     * [GestureDetector.onTouchEvent][android.view.GestureDetector.onTouchEvent],
     * especially for ACTION_DOWN and ACTION_UP
     */
    private class DoubleTapGestureListener(private val rootView: CustomStyledPlayerView) : GestureDetector.SimpleOnGestureListener() {

        private val mHandler = Handler()
        private val mRunnable = Runnable {
            if (DEBUG) Log.d(TAG, "Runnable called")
            isDoubleTapping = false
            controls?.onDoubleTapFinished()
        }

        var controls: PlayerDoubleTapListener? = null
        var isDoubleTapping = false
        var doubleTapDelay: Long = 650

        /**
         * Resets the timeout to keep in double tap mode.
         *
         * Called once in [PlayerDoubleTapListener.onDoubleTapStarted]. Needs to be called
         * from outside if the double tap is customized / overridden to detect ongoing taps
         */
        fun keepInDoubleTapMode() {
            isDoubleTapping = true
            mHandler.removeCallbacks(mRunnable)
            mHandler.postDelayed(mRunnable, doubleTapDelay)
        }

        /**
         * Cancels double tap mode instantly by calling [PlayerDoubleTapListener.onDoubleTapFinished]
         */
        fun cancelInDoubleTapMode() {
            mHandler.removeCallbacks(mRunnable)
            isDoubleTapping = false
            controls?.onDoubleTapFinished()
        }

        override fun onDown(e: MotionEvent): Boolean {
            // Used to override the other methods
            if (isDoubleTapping) {
                controls?.onDoubleTapProgressDown(e.x, e.y)
                return true
            }
            return super.onDown(e)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (isDoubleTapping) {
                if (DEBUG) Log.d(TAG, "onSingleTapUp: isDoubleTapping = true")
                controls?.onDoubleTapProgressUp(e.x, e.y)
                return true
            }
            return super.onSingleTapUp(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Ignore this event if double tapping is still active
            // Return true needed because this method is also called if you tap e.g. three times
            // in a row, therefore the controller would appear since the original behavior is
            // to hide and show on single tap
            if (isDoubleTapping) return true
            if (DEBUG) Log.d(TAG, "onSingleTapConfirmed: isDoubleTap = false")
            return rootView.tap();
            //return rootView.performClick()
            //return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // First tap (ACTION_DOWN) of both taps
            if (DEBUG) Log.d(TAG, "onDoubleTap")
            if (!isDoubleTapping) {
                isDoubleTapping = true
                keepInDoubleTapMode()
                controls?.onDoubleTapStarted(e.x, e.y)
            }
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            // Second tap (ACTION_UP) of both taps
            if (e.actionMasked == MotionEvent.ACTION_UP && isDoubleTapping) {
                if (DEBUG) Log.d(
                    TAG,
                    "onDoubleTapEvent, ACTION_UP"
                )
                controls?.onDoubleTapProgressUp(e.x, e.y)
                return true
            }
            return super.onDoubleTapEvent(e)
        }

        companion object {
            private const val TAG = ".DTGListener"
            private var DEBUG = true
        }
    }
}