package com.example.detectorulcreeps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun OpticScanScreen(onResult: (Float) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    var torchOn by remember { mutableStateOf(true) }
    var session by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(OpticState(0f, 0f, 0f, 0)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Scanare optică", style = MaterialTheme.typography.titleLarge)
        if (!hasPermission) {
            Text(
                "Aplicația are nevoie de permisiunea pentru cameră.",
                modifier = Modifier.padding(top = 24.dp),
            )
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Acordă permisiunea")
            }
        } else {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.aspectRatio(state.frameAspect).fillMaxSize()) {
                    CameraPreview(
                        torchOn = torchOn,
                        session = session,
                        onState = { state = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val minSide = 24.dp.toPx()
                        for (blob in state.blobs) {
                            val w = maxOf(blob.width * size.width, minSide)
                            val h = maxOf(blob.height * size.height, minSide)
                            drawRect(
                                color = Color.Red,
                                topLeft = Offset(blob.centerX * size.width - w / 2, blob.centerY * size.height - h / 2),
                                size = Size(w, h),
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }
                    }
                }
            }
            Text(
                if (state.blobCount > 0) "Punct luminos detectat" else "Niciun punct luminos",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("scor optic %.2f (maxim %.2f), puncte: %d".format(state.filteredScore, state.peakScore, state.blobCount))
            LinearProgressIndicator(progress = { state.filteredScore }, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Țineți telefonul la 30–50 cm și mișcați-l lent spre obiectele suspecte, cu blițul aprins.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { torchOn = !torchOn }) { Text(if (torchOn) "Bliț: ON" else "Bliț: OFF") }
            Button(onClick = { session++ }) { Text("Resetează") }
            Button(onClick = onBack) { Text("Înapoi") }
        }
        Button(onClick = { onResult(state.peakScore) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Vezi rezultatul")
        }
    }
}

@Composable
private fun CameraPreview(
    torchOn: Boolean,
    session: Int,
    onState: (OpticState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            // TextureView: permite desenarea chenarelor deasupra imaginii.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val currentOnState by rememberUpdatedState(onState)

    DisposableEffect(lifecycleOwner, session) {
        val executor = Executors.newSingleThreadExecutor()
        val detector = RetroreflectionDetector()
        val processor = OpticProcessor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(android.util.Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { image ->
                val rotation = image.imageInfo.rotationDegrees
                val upright = rotation % 180 == 0
                val aspect = if (upright) image.width.toFloat() / image.height else image.height.toFloat() / image.width
                val raw = detector.detect(image.toLumaFrame())
                image.close()
                val detection = raw.copy(blobs = raw.blobs.map { it.rotated(rotation) })
                currentOnState(processor.process(detection, aspect))
            }
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis,
            )
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun ImageProxy.toLumaFrame(): LumaFrame {
    val plane = planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return LumaFrame(width, height, plane.rowStride, bytes)
}
