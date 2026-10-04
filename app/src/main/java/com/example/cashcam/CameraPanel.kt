package com.example.cashcam

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CameraPanel(ratio: Float, enabled: Boolean, onCaptured: (File) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    var front by rememberSaveable { mutableStateOf(false) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var capturing by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var initializing by remember { mutableStateOf(true) }
    var retry by remember { mutableIntStateOf(0) }
    val latestCaptured by rememberUpdatedState(onCaptured)
    val latestError by rememberUpdatedState(onError)
    DisposableEffect(controller, lifecycle, front, retry) {
        ready = false
        initializing = true
        var disposed = false
        val future = controller.initializationFuture
        future.addListener({
            if (!disposed) {
                try {
                    future.get()
                    val preferred = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    val alternate = if (front) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                    controller.cameraSelector = if (controller.hasCamera(preferred)) preferred else alternate
                    controller.bindToLifecycle(lifecycle)
                    controller.setZoomRatio(zoom.coerceAtMost(controller.zoomState.value?.maxZoomRatio ?: 1f))
                    ready = true
                } catch (_: Exception) { latestError("No se pudo abrir esta cámara. Cambia de cámara o vuelve a intentarlo.") }
                finally { initializing = false }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { disposed = true; controller.unbind() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.widthIn(max = (460 * ratio).dp).fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(32.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(factory = { PreviewView(it).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                this.controller = controller
            } }, modifier = Modifier.fillMaxSize())
            if (initializing) CircularProgressIndicator()
            else if (!ready) FilledTonalButton(onClick = { retry++ }) { Text("Reintentar cámara") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = { front = !front; zoom = 1f }, enabled = enabled && !capturing) {
                Icon(Icons.Rounded.Cameraswitch, "Cambiar cámara")
            }
            Button(onClick = {
                capturing = true
                val dir = File(context.filesDir, "drafts").apply { mkdirs() }
                val file = File(dir, "${UUID.randomUUID()}.jpg")
                // PreviewView maintains the shared crop viewport and device rotation.
                try {
                    controller.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                capturing = false; latestCaptured(file)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                capturing = false; file.delete(); latestError("No se pudo tomar la foto. Inténtalo de nuevo.")
                            }
                        })
                } catch (_: Exception) { capturing = false; file.delete(); latestError("La cámara aún no está lista") }
            }, enabled = ready && enabled && !capturing, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.PhotoCamera, null); Spacer(Modifier.width(10.dp)); Text(if (capturing) "Capturando…" else "Tomar foto")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Zoom", style = MaterialTheme.typography.labelLarge)
            Slider(value = zoom, onValueChange = {
                val max = controller.zoomState.value?.maxZoomRatio ?: 1f
                zoom = it.coerceAtMost(max)
                controller.setZoomRatio(zoom)
            }, valueRange = 1f..4f, enabled = ready && enabled && !capturing,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Text("%.1f×".format(zoom), style = MaterialTheme.typography.labelLarge)
        }
    }
}
