package com.example.workman

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore

import android.content.Intent


class ScreenshotObserver(handler: Handler, private val context: Context) : ContentObserver(handler) {
    private val contentResolver = context.contentResolver
    private val screenshotUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        checkForScreenshots()
    }

    private fun checkForScreenshots() {
        val cursor = contentResolver.query(
            screenshotUri,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED),
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val fileName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                if (fileName.contains("screenshot", true)) {
                    // Screenshot detected, start overlay service
                    context.startService(Intent(context, OverlayService::class.java))
                }
            }
        }
    }
}

//class ScreenshotObserver(handler: Handler, private val context: Context) : ContentObserver(handler) {
//    private val contentResolver = context.contentResolver
//    private val screenshotUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//
//    override fun onChange(selfChange: Boolean) {
//        super.onChange(selfChange)
//        checkForScreenshots()
//    }
//
//    private fun checkForScreenshots() {
//        val cursor = contentResolver.query(
//            screenshotUri,
//            arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED),
//            null,
//            null,
//            MediaStore.Images.Media.DATE_ADDED + " DESC"
//        )
//
//        cursor?.use {
//            if (it.moveToFirst()) {
//                val fileName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
//                if (fileName.contains("screenshot", true)) {
//                    // Screenshot detected, overlay black screen
//                    overlayBlackScreen()
//                }
//            }
//        }
//    }
//
//    private fun overlayBlackScreen() {
//        val blackScreen = View(context).apply {
//            setBackgroundColor(Color.BLACK)
//            layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//            )
//        }
//        (context as Activity).runOnUiThread {
//            (context.window.decorView as ViewGroup).addView(blackScreen)
//        }
//    }
//}