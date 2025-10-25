package com.convocatis.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Custom ImageGetter that uses Glide to load images from URLs
 * Automatically resizes images to fit within max dimensions
 */
class GlideImageGetter(
    private val context: Context,
    private val textView: TextView,
    private val maxWidth: Int = 800,
    private val maxHeight: Int = 600
) : Html.ImageGetter {

    override fun getDrawable(source: String): Drawable {
        val placeholder = context.getDrawable(android.R.drawable.ic_menu_gallery)
        placeholder?.setBounds(0, 0, 100, 100)

        // Load image with Glide asynchronously using CustomTarget
        MainScope().launch {
            try {
                Glide.with(context)
                    .load(source)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            // Calculate scaled dimensions to fit within max size
                            val imageWidth = resource.intrinsicWidth
                            val imageHeight = resource.intrinsicHeight

                            val (scaledWidth, scaledHeight) = calculateScaledDimensions(
                                imageWidth, imageHeight, maxWidth, maxHeight
                            )

                            resource.setBounds(0, 0, scaledWidth, scaledHeight)

                            // Update TextView with new image
                            textView.text = textView.text
                            textView.invalidate()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // No-op
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            // Show error icon
                            val error = errorDrawable ?: context.getDrawable(android.R.drawable.stat_notify_error)
                            error?.setBounds(0, 0, 50, 50)
                            textView.text = textView.text
                            textView.invalidate()
                        }
                    })
            } catch (e: Exception) {
                android.util.Log.e("GlideImageGetter", "Error loading image: $source", e)
            }
        }

        return placeholder ?: BitmapDrawable(context.resources, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
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
            return Pair(maxWidth, maxHeight)
        }

        val widthRatio = maxWidth.toFloat() / originalWidth
        val heightRatio = maxHeight.toFloat() / originalHeight
        val ratio = minOf(widthRatio, heightRatio, 1.0f) // Don't upscale

        val scaledWidth = (originalWidth * ratio).toInt()
        val scaledHeight = (originalHeight * ratio).toInt()

        return Pair(scaledWidth, scaledHeight)
    }
}
