package com.example.workman

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

class CustomStarRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var starEmpty: Drawable? = null
    private var starHalf: Drawable? = null
    private var starFull: Drawable? = null
    private var rating: Float = 0f // Rating out of 5
    private var starCount: Int = 5 // Total number of stars

    init {
        init()
    }

    private fun init() {
        // Load drawables
        starEmpty = ContextCompat.getDrawable(context, R.drawable.star_empty)
        starHalf = ContextCompat.getDrawable(context, R.drawable.star_half)
        starFull = ContextCompat.getDrawable(context, R.drawable.star_full)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height
        val starWidth = width / starCount

        for (i in 0 until starCount) {
            when {
                i < rating.toInt() -> {
                    starFull?.setBounds(i * starWidth, 0, (i + 1) * starWidth, height)
                    starFull?.draw(canvas)
                }
                i < rating -> {
                    starHalf?.setBounds(i * starWidth, 0, (i + 1) * starWidth, height)
                    starHalf?.draw(canvas)
                }
                else -> {
                    starEmpty?.setBounds(i * starWidth, 0, (i + 1) * starWidth, height)
                    starEmpty?.draw(canvas)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            rating = (x / (width / starCount)).toFloat()
            invalidate() // Redraw the view
            return true
        }
        return super.onTouchEvent(event)
    }

    fun getRating(): Float {
        return rating
    }

    fun setRating(rating: Float) {
        this.rating = rating
        invalidate() // Redraw the view
    }
}
