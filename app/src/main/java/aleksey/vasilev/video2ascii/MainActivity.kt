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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    private lateinit var analyzer: ImageAnalyzer

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

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun PermissionPreview(cameraPermissionState: PermissionState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val textToShow = "Пожалуйста, предоставьте доступ к камере"
            Text(textToShow, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.fillMaxHeight(0.06f))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Запросить разрешения")
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Composable
    private fun VideoPreview(modifier: Modifier) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentContext = LocalContext.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(currentContext) }
        val executor = ContextCompat.getMainExecutor(currentContext)
        AndroidView(modifier = modifier, factory = { context ->
            val imageView = ImageView(context)
            analyzer = ImageAnalyzer(imageView)
            val imageAnalysis = ImageAnalysis.Builder().apply {
                setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            }
            val buildImageAnalysis = imageAnalysis.build().apply {
                setAnalyzer(executor, analyzer)
            }
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    buildImageAnalysis,
                )
            }, executor)
            imageView
        })
    }
}