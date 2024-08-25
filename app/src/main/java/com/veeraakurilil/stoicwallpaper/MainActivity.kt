package com.veeraakurilil.stoicwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.PixelCopy
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AccessMode


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val gridLayout: GridLayout = findViewById(R.id.gridLayout)
        for (i in 0 until 960) {
            val frameLayout = FrameLayout(this)
            frameLayout.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setGravity(Gravity.FILL)
                setMargins(5, 5, 5, 5)
            }

            if (i < 458) {
                frameLayout.setBackgroundColor(Color.GREEN)
            } else {
                frameLayout.setBackgroundColor(Color.GRAY)
            }

            gridLayout.addView(frameLayout)
        }

        gridLayout.post {
            for (i in 0 until gridLayout.childCount) {
                val child = gridLayout.getChildAt(i)

                val layoutParams = child.layoutParams as GridLayout.LayoutParams
                layoutParams.width = child.width
                layoutParams.height = child.width
                child.layoutParams = layoutParams
            }

            val view = findViewById<FrameLayout>(R.id.main)
            val bitMap = view.toBitmap(desiredWidth = screenWidth, desiredHeight = screenHeight)
            val filename = "name.png"

            val sd = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
            sd.mkdirs()
            val dest = File(sd, filename)

            try {
                val out = FileOutputStream(dest)
                bitMap.compress(Bitmap.CompressFormat.PNG, 90, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            try {
                val pathURI = Uri.fromFile(File(sd.absolutePath + "/name.png"))


//                val inputStream = contentResolver.openInputStream(pathURI) // Or get the image from another source
//                wallpaperManager.setStream(inputStream)

                openWallpaperPicker(convertFileUriToContentUri(pathURI))

            } catch (e: IOException) {
                // Handle exceptions
                e.printStackTrace()
            }
        }
        Log.i("MainActivity", "onCreate: " + gridLayout.childCount)
    }

    private fun openWallpaperPicker(uri: Uri) {
        val intent = WallpaperManager.getInstance(applicationContext).getCropAndSetWallpaperIntent(uri)
        startActivity(intent)
    }

    fun convertFileUriToContentUri(fileUri: Uri): Uri {
        // Check if the Uri is already a content Uri
        if (fileUri.scheme == ContentResolver.SCHEME_CONTENT) {
            return fileUri
        }

        // If not, try to convert it
        try {
            val contentResolver = applicationContext.contentResolver // Assuming you're in an Activity or have access to context
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileUri.lastPathSegment) // Use the filename from fileUri
                put(MediaStore.MediaColumns.MIME_TYPE, contentResolver.getType(fileUri))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Wallpapers") // Or any other suitable directory
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val contentUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            contentUri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING,
                    0)
                    contentResolver.update(it,
                    contentValues, null, null)
                }
            }
            return contentUri?:fileUri
        } catch (e: IOException) {
            // Handle exceptions
            return fileUri
        }
    }


}

fun View.toBitmap(): Bitmap {

    //Measure and layout the view to ensure its final size is calculated
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    layout(0, 0, 1080, 1920)

    //Create a Bitmap with the same dimensions as the view
    val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)

    //Create a Canvas to draw the view's content onto the Bitmap
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

fun View.toBitmap(desiredWidth: Int = this.width, desiredHeight: Int = this.height): Bitmap {
    //Measure the view with the desired dimensions
    measure(
        View.MeasureSpec.makeMeasureSpec(desiredWidth, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(desiredHeight, View.MeasureSpec.EXACTLY)

    )

    //Layout the view with the measured dimensions
    layout(0, 0, measuredWidth, measuredHeight)

    val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)

    return bitmap
}


