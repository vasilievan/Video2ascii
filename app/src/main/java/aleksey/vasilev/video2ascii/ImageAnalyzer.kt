package aleksey.vasilev.video2ascii

import android.annotation.SuppressLint
import android.graphics.*
import android.widget.ImageView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ImageAnalyzer(private val imageView: ImageView) :
    ImageAnalysis.Analyzer {
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var bitmap: Bitmap
    private lateinit var letter: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var widthList: List<Int>
    private lateinit var heightList: List<Int>
    private lateinit var paint: Paint
    private var maxHeight = Int.MIN_VALUE
    private var maxWidth = Int.MIN_VALUE
    private val firstSymbolCode = 32
    private val lastSymbolCode = 126
    private val asciiSymbols = MutableList(lastSymbolCode - firstSymbolCode + 1) {
        (it + firstSymbolCode).toChar().toString()
    }
    private val asciiSymbolsSizes = mutableMapOf<String, Pair<Int, Int>>()
    private val asciiSymbolsAverages = mutableMapOf<Quadruple, IntArray>()
    private val matrix = Matrix().apply {
        postRotate(90f)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (!::bitmapBuffer.isInitialized) {
            bitmapBuffer = Bitmap.createBitmap(
                image.width, image.height, Bitmap.Config.ARGB_8888
            )
            bitmap = Bitmap.createBitmap(
                image.height, image.width, Bitmap.Config.ARGB_8888
            )
            measureLetterSizes()
            createLetterSamples()
        }
        image.use {
            bitmapBuffer.copyPixelsFromBuffer(it.image!!.planes[0].buffer)
            bitmap = Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                matrix,
                true
            )
        }
        transform()
        image.close()
    }

    private fun measureLetterSizes() {
        val bounds = Rect()
        paint = Paint()
        paint.apply {
            typeface = Typeface.create("Arial", Typeface.NORMAL)
            textSize = 8f
        }
        asciiSymbols.forEach {
            paint.getTextBounds(it, 0, 1, bounds)
            val width = bounds.width()
            val height = bounds.height()
            asciiSymbolsSizes[it] = width to height
            if (width > maxWidth) {
                maxWidth = width
            }
            if (height > maxHeight) {
                maxHeight = height
            }
        }
        widthList = (0..(bitmap.width - maxWidth) step maxWidth).toList()
        heightList = (0..(bitmap.height - maxHeight) step maxHeight).toList()
        letter = Bitmap.createBitmap(
            maxWidth, maxHeight, Bitmap.Config.ARGB_8888
        )
        canvas = Canvas(letter)
    }

    private fun createLetterSamples() {
        asciiSymbols.forEach {
            paint.color = Color.WHITE
            paint.apply {
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
            }
            canvas.drawPaint(paint)
            paint.color = Color.BLACK
            canvas.drawText(
                it,
                maxWidth / 2f,
                (maxHeight - (paint.descent() + paint.ascent())) / 2f,
                paint
            )
            val average = IntArray(maxHeight * maxWidth)
            letter.getPixels(average, 0, maxWidth, 0, 0, maxWidth, maxHeight)
            val clone = average.clone()
            asciiSymbolsAverages[average.countCorners()] = clone
        }
    }

    private fun transform() {
        widthList.parallelStream().forEach { x ->
            heightList.parallelStream().forEach { y ->
                val average = IntArray(maxHeight * maxWidth)
                bitmap.getPixels(average, 0, maxWidth, x, y, maxWidth, maxHeight)
                val minimum = average.countCorners()
                val bits = asciiSymbolsAverages[asciiSymbolsAverages.keys.minByOrNull {
                    it.getDistance(minimum)
                }]
                val red = (average.sumOf { it.and(0x00FF0000).shr(16) } / average.size).shl(16)
                val green = (average.sumOf { it.and(0x0000FF00).shr(8) } / average.size).shl(8)
                val blue = average.sumOf { it.and(0x000000FF) } / average.size
                bitmap.setPixels(
                    bits!!.map { red.or(green).or(blue).or(it) }.toIntArray(),
                    0,
                    maxWidth,
                    x,
                    y,
                    maxWidth,
                    maxHeight
                )
            }
        }
        imageView.setImageBitmap(bitmap.crop())
    }

    private fun Bitmap.crop(): Bitmap {
        val bitmap = Bitmap.createBitmap(this, 0, 0, widthList.last(), heightList.last())
        this.recycle()
        return bitmap
    }

    private fun IntArray.countCorners(): Quadruple {
        letter.getPixels(this, 0, maxWidth, 0, 0, maxWidth / 2, maxHeight / 2)
        val nw = this.sum() / this.size
        letter.getPixels(this, 0, maxWidth, maxWidth / 2, 0, maxWidth / 2, maxHeight / 2)
        val ne = this.sum() / this.size
        letter.getPixels(this, 0, maxWidth, 0, maxHeight / 2, maxWidth / 2, maxHeight / 2)
        val sw = this.sum() / this.size
        letter.getPixels(
            this,
            0,
            maxWidth,
            maxWidth / 2,
            maxHeight / 2,
            maxWidth / 2,
            maxHeight / 2
        )
        val se = this.sum() / this.size
        return Quadruple(nw, ne, sw, se)
    }
}