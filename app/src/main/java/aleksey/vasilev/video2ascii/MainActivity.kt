package aleksey.vasilev.video2ascii

import aleksey.vasilev.video2ascii.ui.theme.Video2asciiTheme
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/** Video2ascii converter. Presents image as a sequence of same size blocks (one equals to the
 *  biggest letter in fonts with given parameters). Then a photo is presented in grayscale, 4 areas
 *  for each block are counted (NW, NE, SW, SE). And due to the density of black pixels comparing to
 *  default letters (in fact, vectors that present them), next letter is chosen.
 *
 *  To change camera view, click on video.
 *
 *  @author <a href="mailto:enthusiastic.programmer@yandex.ru">Алексей Васильев</a>
 *  @version 1.0
 *  */

class MainActivity : ComponentActivity() {
    private lateinit var analyzer: ImageAnalyzer
    private lateinit var buildImageAnalysis: ImageAnalysis
    private lateinit var imageView: ImageView
    private val imageAnalysis = ImageAnalysis.Builder().apply {
        setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Video2asciiTheme {
                val cameraPermissionState = rememberPermissionState(
                    android.Manifest.permission.CAMERA
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    if (cameraPermissionState.status.isGranted) VideoPreview(Modifier.fillMaxSize())
                    else PermissionPreview(cameraPermissionState)
                }
            }
        }
    }

    /** Request permissions if needed.
     * */

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun PermissionPreview(cameraPermissionState: PermissionState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val textToShow = stringResource(R.string.request_permissions_text)
            Text(textToShow, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.fillMaxHeight(0.06f))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text(stringResource(R.string.request_permissions))
            }
        }
    }

    /** Video preview to show modified video.
     * */

    @SuppressLint("UnsafeOptInUsageError")
    @Composable
    private fun VideoPreview(modifier: Modifier) {
        val currentContext = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(currentContext) }
        val executor = ContextCompat.getMainExecutor(currentContext)
        var direction by remember {
            mutableStateOf(1)
        }
        AndroidView(modifier = modifier.clickable {
            direction = (direction + 1) % 2
            addListener(cameraProviderFuture, direction, lifecycleOwner, executor)
            imageView.rotation = if (direction == 0) 180f else 0f
        }, factory = { context ->
            imageView = ImageView(context)
            analyzer = ImageAnalyzer(imageView)
            buildImageAnalysis = imageAnalysis.build().apply {
                setAnalyzer(executor, analyzer)
            }
            addListener(cameraProviderFuture, direction, lifecycleOwner, executor)
            imageView
        })
    }

    /** Listener on cameraProviderFuture to change camera view.
     * */

    private fun addListener(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        direction: Int,
        lifecycleOwner: LifecycleOwner,
        executor: Executor
    ) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(direction)
                .build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                buildImageAnalysis,
            )
        }, executor)
    }
}