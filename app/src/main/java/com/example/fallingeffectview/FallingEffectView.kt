package com.example.fallingeffectview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.drawable.toBitmap

class FallingEffectView (context: Context, attrs: AttributeSet) : View(context, attrs) {


     var creationResourcesNum : Int
     var snowflakeImage: Bitmap?
     var snowflakeAlphaMin: Int
     var snowflakeAlphaMax: Int
     var snowflakeAngleMax: Int
     var snowflakeSizeMinInPx: Int
     var snowflakeSizeMaxInPx: Int
     var snowflakeSpeedMin: Int
     var snowflakeSpeedMax: Int
     var snowflakesFadingEnabled: Boolean
     var snowflakesAlreadyFalling: Boolean

    private lateinit var updateSnowflakesThread: UpdateSnowflakesThread
    private var snowflakes: Array<Snowflake>? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlowingEffectView)
        try {
            creationResourcesNum = a.getInt(R.styleable.FlowingEffectView_creationResourcesNum, DEFAULT_SNOWFLAKES_NUM)
            snowflakeImage = a.getDrawable(R.styleable.FlowingEffectView_snowflakeImage)?.toBitmap()
                snowflakeAlphaMin = a.getInt(R.styleable.FlowingEffectView_snowflakeAlphaMin, DEFAULT_SNOWFLAKE_ALPHA_MIN)
            snowflakeAlphaMax = a.getInt(R.styleable.FlowingEffectView_snowflakeAlphaMax, DEFAULT_SNOWFLAKE_ALPHA_MAX)
            snowflakeAngleMax = a.getInt(R.styleable.FlowingEffectView_snowflakeAngleMax, DEFAULT_SNOWFLAKE_ANGLE_MAX)
            snowflakeSizeMinInPx = a.getDimensionPixelSize(R.styleable.FlowingEffectView_snowflakeSizeMin, dpToPx(DEFAULT_SNOWFLAKE_SIZE_MIN_IN_DP))
            snowflakeSizeMaxInPx = a.getDimensionPixelSize(R.styleable.FlowingEffectView_snowflakeSizeMax, dpToPx(DEFAULT_SNOWFLAKE_SIZE_MAX_IN_DP))
            snowflakeSpeedMin = a.getInt(R.styleable.FlowingEffectView_snowflakeSpeedMin, DEFAULT_SNOWFLAKE_SPEED_MIN)
            snowflakeSpeedMax = a.getInt(R.styleable.FlowingEffectView_snowflakeSpeedMax, DEFAULT_SNOWFLAKE_SPEED_MAX)
            snowflakesFadingEnabled = a.getBoolean(R.styleable.FlowingEffectView_snowflakesFadingEnabled, DEFAULT_SNOWFLAKES_FADING_ENABLED)
            snowflakesAlreadyFalling = a.getBoolean(R.styleable.FlowingEffectView_snowflakesAlreadyFalling, DEFAULT_SNOWFLAKES_ALREADY_FALLING)

            setLayerType(LAYER_TYPE_HARDWARE, null)

        } finally {
            a.recycle()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateSnowflakesThread = UpdateSnowflakesThread()
    }

    override fun onDetachedFromWindow() {
        updateSnowflakesThread.quit()
        super.onDetachedFromWindow()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        snowflakes = createSnowflakes()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this && visibility == GONE) {
            snowflakes?.forEach { it.reset() }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isInEditMode) {
            return
        }

        var haveAtLeastOneVisibleSnowflake = false

        val localSnowflakes = snowflakes
        if (localSnowflakes != null) {
            for (snowflake in localSnowflakes) {
                if (snowflake.isStillFalling()) {
                    haveAtLeastOneVisibleSnowflake = true
                    snowflake.draw(canvas)
                }
            }
        }

        if (haveAtLeastOneVisibleSnowflake) {
            updateSnowflakes()
        } else {
            visibility = GONE
        }

        val fallingSnowflakes = snowflakes?.filter { it.isStillFalling() }
        if (fallingSnowflakes?.isNotEmpty() == true) {
            fallingSnowflakes.forEach { it.draw(canvas) }
            updateSnowflakes()
        }
        else {
            visibility = GONE
        }
    }

    fun stopFalling() {
        snowflakes?.forEach { it.shouldRecycleFalling = false }
    }

    fun restartFalling() {
        snowflakes?.forEach { it.shouldRecycleFalling = true }
    }

    private fun createSnowflakes(): Array<Snowflake> {
        val randomizer = Randomizer()

        val snowflakeParams = Snowflake.Params(
            parentWidth = width,
            parentHeight = height,
            image = snowflakeImage,
            alphaMin = snowflakeAlphaMin,
            alphaMax = snowflakeAlphaMax,
            angleMax = snowflakeAngleMax,
            sizeMinInPx = snowflakeSizeMinInPx,
            sizeMaxInPx = snowflakeSizeMaxInPx,
            speedMin = snowflakeSpeedMin,
            speedMax = snowflakeSpeedMax,
            fadingEnabled = snowflakesFadingEnabled,
            alreadyFalling = snowflakesAlreadyFalling)

        return Array(creationResourcesNum) { Snowflake(randomizer, snowflakeParams) }
    }

    private fun updateSnowflakes() {
        updateSnowflakesThread.handler.post {
            var haveAtLeastOneVisibleSnowflake = false

            val localSnowflakes = snowflakes ?: return@post

            for (snowflake in localSnowflakes) {
                if (snowflake.isStillFalling()) {
                    haveAtLeastOneVisibleSnowflake = true
                    snowflake.update()
                }
            }

            if (haveAtLeastOneVisibleSnowflake) {
                postInvalidateOnAnimation()
            }
        }
    }

    private class UpdateSnowflakesThread : HandlerThread("SnowflakesComputations") {
        val handler: Handler

        init {
            start()
            handler = Handler(looper)
        }
    }

    companion object {
        private const val DEFAULT_SNOWFLAKES_NUM = 200
        private const val DEFAULT_SNOWFLAKE_ALPHA_MIN = 150
        private const val DEFAULT_SNOWFLAKE_ALPHA_MAX = 250
        private const val DEFAULT_SNOWFLAKE_ANGLE_MAX = 10
        private const val DEFAULT_SNOWFLAKE_SIZE_MIN_IN_DP = 2
        private const val DEFAULT_SNOWFLAKE_SIZE_MAX_IN_DP = 8
        private const val DEFAULT_SNOWFLAKE_SPEED_MIN = 2
        private const val DEFAULT_SNOWFLAKE_SPEED_MAX = 8
        private const val DEFAULT_SNOWFLAKES_FADING_ENABLED = false
        private const val DEFAULT_SNOWFLAKES_ALREADY_FALLING = false
    }
}