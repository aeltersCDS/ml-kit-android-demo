package com.codigodelsur.mlkit.core.presentation.component

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.codigodelsur.mlkit.R
import com.codigodelsur.mlkit.core.presentation.util.rotateBitmap

@Composable
fun MlkCameraPreview(
    modifier: Modifier,
    cameraSelector: CameraSelector = DEFAULT_BACK_CAMERA,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    onPhotoCapture: ((Bitmap) -> Unit)? = null,
    onSwitchCamera: ((CameraSelector) -> Unit)? = null,
    setUpDetector: ((LifecycleCameraController, Context) -> Unit)? = null,
    overlays: @Composable BoxScope.() -> Unit = {}
) {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }
    val background = MaterialTheme.colorScheme.surface
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            factory = {
                PreviewView(it).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(background.toArgb())
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    this.scaleType = scaleType
                }.also { previewView ->
                    setUpDetector?.invoke(cameraController, context)
                    cameraController.bindToLifecycle(lifecycleOwner)
                    previewView.controller = cameraController
                }
            },
            update = {
                cameraController.cameraSelector = cameraSelector
            }
        )

        overlays()

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onSwitchCamera != null) {
                FloatingActionButton(
                    onClick = {
                        val newSelector =
                            if (cameraSelector == DEFAULT_BACK_CAMERA) {
                                DEFAULT_FRONT_CAMERA
                            } else {
                                DEFAULT_BACK_CAMERA
                            }
                        onSwitchCamera(newSelector)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_switch_camera_24),
                        contentDescription = stringResource(id = R.string.camera_preview_switch_camera)
                    )
                }
            }

            if (onPhotoCapture != null) {
                FloatingActionButton(
                    onClick = {
                        capturePhoto(context, cameraController, onPhotoCapture)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera_24),
                        contentDescription = stringResource(id = R.string.camera_preview_capture_photo)
                    )
                }
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
    cameraController: LifecycleCameraController,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val mainExecutor = ContextCompat.getMainExecutor(context)

    cameraController.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val correctedBitmap: Bitmap = image
                .toBitmap()
                .rotateBitmap(image.imageInfo.rotationDegrees)

            onPhotoCaptured(correctedBitmap)
            image.close()
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("MlkCameraPreview", "Error capturing image", exception)
        }
    })
}