package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.RoomEntity
import java.util.concurrent.Executors

@Composable
fun ArRoomScannerView(
    activeRoom: RoomEntity?,
    equipmentList: List<EquipmentEntity>,
    annotations: List<AnnotationEntity>,
    onCapturePhoto: (Bitmap, String, String?, String) -> Unit,
    onAddArPushpin: (text: String, posX: Float, posY: Float, color: Long) -> Unit,
    onDeleteAnnotation: (AnnotationEntity) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        // Permission Request UI
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0B132B)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The Real-Time AR Scanner uses your device camera feed to scan rooms, detect surfaces, and project holographic 3D pushpins.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Camera & AR State
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }

    // Pushpin Dialog State
    var showPlacePinDialog by remember { mutableStateOf(false) }
    var pendingPinCoords by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var selectedPinForDetails by remember { mutableStateOf<AnnotationEntity?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // AR Scanning Beam Animation & Pin Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "ar_scanner_beam")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y"
    )
    val pinPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pin_pulse"
    )

    // Filter only AR pushpin annotations
    val arPushpins = annotations.filter { it.type == "AR_PUSHPIN" }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Live CameraX Preview View
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. AR HUD Spatial Overlay & Interactive Tap Layer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ar_scanner_canvas")
                .pointerInput(arPushpins) {
                    detectTapGestures { tapOffset ->
                        val normX = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val normY = (tapOffset.y / size.height.toFloat()).coerceIn(0f, 1f)

                        // Check if tapped on an existing pushpin
                        var tappedPin: AnnotationEntity? = null
                        for (pin in arPushpins) {
                            val pinX = pin.posX * size.width
                            val pinY = pin.posY * size.height
                            val dist = (tapOffset - Offset(pinX, pinY)).getDistance()
                            if (dist < 60f) {
                                tappedPin = pin
                                break
                            }
                        }

                        if (tappedPin != null) {
                            selectedPinForDetails = tappedPin
                        } else {
                            // Place new 3D pushpin at these coordinates
                            pendingPinCoords = Offset(normX, normY)
                            showPlacePinDialog = true
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // Center targeting reticle
            val cx = w / 2f
            val cy = h / 2f
            val reticleSize = 50f
            val bracketLen = 24f
            val strokeW = 2.5f
            val arColor = Color(0xFF00E5FF)

            // Top-Left bracket
            drawLine(arColor, Offset(cx - reticleSize, cy - reticleSize), Offset(cx - reticleSize + bracketLen, cy - reticleSize), strokeW)
            drawLine(arColor, Offset(cx - reticleSize, cy - reticleSize), Offset(cx - reticleSize, cy - reticleSize + bracketLen), strokeW)

            // Top-Right bracket
            drawLine(arColor, Offset(cx + reticleSize, cy - reticleSize), Offset(cx + reticleSize - bracketLen, cy - reticleSize), strokeW)
            drawLine(arColor, Offset(cx + reticleSize, cy - reticleSize), Offset(cx + reticleSize, cy - reticleSize + bracketLen), strokeW)

            // Bottom-Left bracket
            drawLine(arColor, Offset(cx - reticleSize, cy + reticleSize), Offset(cx - reticleSize + bracketLen, cy + reticleSize), strokeW)
            drawLine(arColor, Offset(cx - reticleSize, cy + reticleSize), Offset(cx - reticleSize, cy + reticleSize - bracketLen), strokeW)

            // Bottom-Right bracket
            drawLine(arColor, Offset(cx + reticleSize, cy + reticleSize), Offset(cx + reticleSize - bracketLen, cy + reticleSize), strokeW)
            drawLine(arColor, Offset(cx + reticleSize, cy + reticleSize), Offset(cx + reticleSize, cy + reticleSize - bracketLen), strokeW)

            // Center pip
            drawCircle(color = arColor, radius = 3.5f, center = Offset(cx, cy))

            // AR Scanning Laser Line
            val beamY = h * scanLineY
            drawLine(
                color = arColor.copy(alpha = 0.75f),
                start = Offset(w * 0.1f, beamY),
                end = Offset(w * 0.9f, beamY),
                strokeWidth = 2f
            )
            drawLine(
                color = arColor.copy(alpha = 0.25f),
                start = Offset(w * 0.1f, beamY),
                end = Offset(w * 0.9f, beamY),
                strokeWidth = 8f
            )

            // Render 3D Digital Pushpins placed by user at specific coordinates
            arPushpins.forEach { pin ->
                val pinScreenX = pin.posX * w
                val pinScreenY = pin.posY * h
                val pinColor = Color(pin.color.toInt())

                // 1. Base anchor rings (simulated 3D floor/surface attachment)
                drawCircle(
                    color = pinColor.copy(alpha = (1f - pinPulse) * 0.8f),
                    radius = 16f + pinPulse * 20f,
                    center = Offset(pinScreenX, pinScreenY),
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = pinColor.copy(alpha = 0.9f),
                    radius = 8f,
                    center = Offset(pinScreenX, pinScreenY)
                )

                // 2. Upright vertical 3D pushpin needle
                val needleHeight = 45f
                val pinHeadCenter = Offset(pinScreenX, pinScreenY - needleHeight)

                drawLine(
                    color = Color.White,
                    start = Offset(pinScreenX, pinScreenY),
                    end = pinHeadCenter,
                    strokeWidth = 2.5f
                )

                // 3. Pushpin Diamond / Spherical Head
                val headRadius = 12f
                drawCircle(
                    color = pinColor,
                    radius = headRadius,
                    center = pinHeadCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = headRadius * 0.45f,
                    center = pinHeadCenter
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = headRadius,
                    center = pinHeadCenter,
                    style = Stroke(width = 1.5f)
                )

                // 4. Floating holographic text callout badge
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val textW = paint.measureText(pin.text)
                val badgePadH = 12f
                val badgePadV = 8f
                val badgeTop = pinHeadCenter.y - headRadius - 28f

                drawRoundRect(
                    color = Color(0xDD0B132B),
                    topLeft = Offset(pinHeadCenter.x - textW / 2f - badgePadH, badgeTop - badgePadV),
                    size = androidx.compose.ui.geometry.Size(textW + badgePadH * 2, 28f + badgePadV),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = pinColor,
                    topLeft = Offset(pinHeadCenter.x - textW / 2f - badgePadH, badgeTop - badgePadV),
                    size = androidx.compose.ui.geometry.Size(textW + badgePadH * 2, 28f + badgePadV),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                    style = Stroke(width = 1.5f)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    pin.text,
                    pinHeadCenter.x,
                    badgeTop + 14f,
                    paint
                )
            }
        }

        // 3. Top Telemetry Bar & Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xDD0B132B),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PushPin, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "3D PUSHPIN MODE ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "Tap camera view to place 3D pin (${arPushpins.size} placed)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Row {
                        // Torch Toggle
                        IconButton(
                            onClick = {
                                torchEnabled = !torchEnabled
                                camera?.cameraControl?.enableTorch(torchEnabled)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Toggle Torch",
                                tint = if (torchEnabled) Color(0xFFFFD166) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close Scanner
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp).testTag("close_ar_scanner_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Floating Tip Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xAA0B132B),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 92.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.AddLocation, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap anywhere on physical walls or floor to place 3D marker",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 5. Bottom Shutter & Capture Bar
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color(0xDD0B132B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Telemetry Readout
                Column {
                    Text(
                        text = "COORDINATE ANCHOR: ACTIVE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${arPushpins.size} PUSHPINS IN FRAME",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Primary Shutter Button (Takes high-res AR photo with overlaid pushpins)
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                    val rotationDegrees = image.imageInfo.rotationDegrees
                                    if (rotationDegrees != 0) {
                                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                    }
                                    image.close()

                                    val roomName = activeRoom?.name ?: "Facility AR Scan"
                                    onCapturePhoto(bitmap, roomName, null, "AR View with ${arPushpins.size} Pushpins")
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                        .testTag("ar_scanner_shutter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture AR Photo",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Switch Camera Lens
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Modal Dialog: Place 3D Pushpin at Tapped Coordinates
    if (showPlacePinDialog) {
        var pinLabel by remember { mutableStateOf("") }
        var pinCategory by remember { mutableStateOf("Hazard") }
        var selectedColor by remember { mutableStateOf(0xFFFFD166) } // Amber default for pin

        val categories = listOf("Hazard ⚠️", "Repair 🔧", "Inspection 🔍", "Marker 📍")
        val pinColors = listOf(
            0xFFFFD166, // Amber
            0xFFEF4444, // Red
            0xFF00E5FF, // Cyan
            0xFF10B981  // Emerald Green
        )

        AlertDialog(
            onDismissRequest = { showPlacePinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PushPin, contentDescription = null, tint = Color(selectedColor.toInt()))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Place 3D Pushpin Marker", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinLabel.isNotBlank()) {
                            onAddArPushpin(
                                "$pinCategory: ${pinLabel.trim()}",
                                pendingPinCoords.x,
                                pendingPinCoords.y,
                                selectedColor
                            )
                            showPlacePinDialog = false
                        }
                    },
                    enabled = pinLabel.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColor.toInt())),
                    modifier = Modifier.testTag("confirm_place_pin_btn")
                ) {
                    Text("Anchor Pushpin", color = if (selectedColor == 0xFF00E5FF || selectedColor == 0xFFFFD166) Color.Black else Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlacePinDialog = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = String.format("Anchoring coordinates: X: %.1f%%, Y: %.1f%%", pendingPinCoords.x * 100f, pendingPinCoords.y * 100f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = pinLabel,
                        onValueChange = { pinLabel = it },
                        label = { Text("Marker Label / Description") },
                        placeholder = { Text("e.g. Pipe leak, High heat conduit, Valve 4...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ar_pin_label_input")
                    )

                    // Category Chips
                    Text("Category Tag:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            val cleanCat = cat.split(" ")[0]
                            FilterChip(
                                selected = pinCategory == cleanCat,
                                onClick = { pinCategory = cleanCat },
                                label = { Text(cat, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Color Palette
                    Text("Pushpin Glow Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pinColors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.toInt()))
                                    .border(
                                        width = if (selectedColor == c) 3.dp else 1.dp,
                                        color = if (selectedColor == c) Color.White else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = c }
                            )
                        }
                    }
                }
            }
        )
    }

    // Modal Dialog: Inspect or Delete Existing Pushpin
    selectedPinForDetails?.let { pin ->
        AlertDialog(
            onDismissRequest = { selectedPinForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(pin.color.toInt()))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3D Digital Marker", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPinForDetails = null }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteAnnotation(pin)
                        selectedPinForDetails = null
                    }
                ) {
                    Text("Delete Pushpin", color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column {
                    Text(
                        text = pin.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("Frame coordinates: X: %.1f%%, Y: %.1f%%", pin.posX * 100f, pin.posY * 100f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}
