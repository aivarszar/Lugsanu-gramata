package com.convocatis.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.lang.ref.WeakReference

/**
 * Custom ImageGetter that uses Glide to load images from URLs
 * Automatically resizes images to fit within max dimensions
 */
class GlideImageGetter(
    private val context: Context,
    textView: TextView,
    private val maxWidth: Int = 800,
    private val maxHeight: Int = 600
) : Html.ImageGetter {

    private val textViewRef = WeakReference(textView)

    override fun getDrawable(source: String): Drawable {
        val urlDrawable = UrlDrawable()

        // Load image with Glide asynchronously
        Glide.with(context)
            .load(source)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    // Calculate scaled dimensions
                    val imageWidth = resource.intrinsicWidth
                    val imageHeight = resource.intrinsicHeight

                    val (scaledWidth, scaledHeight) = calculateScaledDimensions(
                        imageWidth, imageHeight, maxWidth, maxHeight
                    )

                    resource.setBounds(0, 0, scaledWidth, scaledHeight)
                    urlDrawable.setBounds(0, 0, scaledWidth, scaledHeight)
                    urlDrawable.drawable = resource

                    // Refresh TextView to show the loaded image
                    textViewRef.get()?.let { textView ->
                        textView.text = textView.text
                        textView.invalidate()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // No-op
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // Show error icon
                    val error = errorDrawable ?: context.getDrawable(android.R.drawable.stat_notify_error)
                    if (error != null) {
                        error.setBounds(0, 0, 50, 50)
                        urlDrawable.setBounds(0, 0, 50, 50)
                        urlDrawable.drawable = error

                        textViewRef.get()?.let { textView ->
                            textView.text = textView.text
                            textView.invalidate()
                        }
                    }
                }
            })

        return urlDrawable
    }

    /**
     * Drawable that can be updated after async image load
     */
    private class UrlDrawable : BitmapDrawable() {
        var drawable: Drawable? = null

        override fun draw(canvas: Canvas) {
            drawable?.draw(canvas)
        }
    }

    /**
     * Calculate scaled dimensions to fit within max size while preserving aspect ratio
     */
    private fun calculateScaledDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        if (originalWidth <= 0 || originalHeight <= 0) {
            return Pair(100, 100)
        }

        val widthRatio = maxWidth.toFloat() / originalWidth
        val heightRatio = maxHeight.toFloat() / originalHeight
        val ratio = minOf(widthRatio, heightRatio, 1.0f) // Don't upscale

        val scaledWidth = (originalWidth * ratio).toInt()
        val scaledHeight = (originalHeight * ratio).toInt()

        return Pair(scaledWidth, scaledHeight)
    }
}

