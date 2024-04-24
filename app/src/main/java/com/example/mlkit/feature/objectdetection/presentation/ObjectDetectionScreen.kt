package com.example.mlkit.feature.objectdetection.presentation

import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mlkit.R
import com.example.mlkit.app.ui.theme.MlkTheme
import com.example.mlkit.app.ui.theme.Typography
import com.example.mlkit.core.presentation.component.CameraPermissionRequester
import com.example.mlkit.core.presentation.component.MikCameraPreview
import com.example.mlkit.core.presentation.component.MlkTopAppBar
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.random.Random

@Composable
fun ObjectDetectionRoute(
    modifier: Modifier = Modifier,
    viewModel: ObjectDetectionViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObjectDetectionScreen(
        modifier = modifier,
        hideUnlabeled = state.hideUnlabeled,
        detectedObjects = state.detectedObjects,
        onObjectDetected = { objects ->
            viewModel.updateDetectedObjects(objects)
        },
        onToggleHideUnlabeled = {
            viewModel.toggleHideUnlabeled()
        },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectDetectionScreen(
    modifier: Modifier = Modifier,
    hideUnlabeled: Boolean,
    detectedObjects: List<DetectedObject>,
    onObjectDetected: (List<DetectedObject>) -> Unit,
    onToggleHideUnlabeled: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        MlkTopAppBar(
            titleRes = R.string.feature_object_detection_title,
            onNavigationClick = onBackClick
        )
        CameraPermissionRequester(
            modifier = Modifier.weight(1.0f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MikCameraPreview(modifier = modifier.fillMaxSize(),
                    setUpDetector = { cameraController, context ->
                        val options = ObjectDetectorOptions.Builder()
                            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                            .enableMultipleObjects()
                            .enableClassification()
                            .build()
                        val objectDetector = ObjectDetection.getClient(options)

                        cameraController.setImageAnalysisAnalyzer(
                            ContextCompat.getMainExecutor(
                            context
                        ), MlKitAnalyzer(
                            listOf(objectDetector),
                            ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                            ContextCompat.getMainExecutor(context)
                        ) { result: MlKitAnalyzer.Result? ->
                            val objects = result?.getValue(objectDetector)
                            onObjectDetected(objects.orEmpty())
                        })
                    }
                )

                BoundingBoxesOverlay(
                    modifier = Modifier.fillMaxSize(),
                    hideUnlabeled = hideUnlabeled,
                    objects = detectedObjects
                )

                Button(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp),
                    onClick = onToggleHideUnlabeled
                ) {
                    Text(
                        text = stringResource(
                            id = if (hideUnlabeled) {
                                R.string.object_detection_show_all
                            } else {
                                R.string.object_detection_hide_unlabeled
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BoundingBoxesOverlay(
    modifier: Modifier,
    hideUnlabeled: Boolean,
    objects: List<DetectedObject>
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.clipToBounds()) {
        for (detectedObject in objects) {
            val label = detectedObject.labels.maxByOrNull { it.confidence }

            val color = getColorFromId(detectedObject.trackingId ?: 1)
            // Draw the rectangle
            val boxRect = detectedObject.boundingBox.toComposeRect()
            if (label != null || !hideUnlabeled) {
                drawRect(
                    color = color,
                    topLeft = boxRect.topLeft,
                    size = boxRect.size,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            if (label != null) {
                val legend = "${label.text} - ${"%.2f".format(label.confidence)}"
                val legendStyle = Typography.labelMedium.copy(color = color)

                val measure = textMeasurer.measure(
                    text = legend,
                    style = legendStyle
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = legend,
                    style = legendStyle,
                    topLeft = Offset(
                        x = boxRect.left,
                        y = boxRect.top - measure.getBoundingBox(0).height
                    )
                )
            }
        }
    }
}

private fun getColorFromId(trackingId: Int): Color {
    val random = Random(trackingId.hashCode())
    val r = random.nextInt(256)
    val g = random.nextInt(256)
    val b = random.nextInt(256)
    return Color(r, g, b)
}

@Preview
@Composable
private fun TextRecognitionScreenPreview() {
    MlkTheme {
        ObjectDetectionScreen(
            hideUnlabeled = false,
            detectedObjects = listOf(),
            onObjectDetected = { _ -> },
            onToggleHideUnlabeled = {},
            onBackClick = {}
        )
    }
}