package com.example.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.Pathfinding3D
import com.example.ui.NavTarget
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HeadsUpDisplay(
    navTarget: NavTarget?,
    userLocation: Offset,
    cameraYaw: Float,
    is3DMode: Boolean,
    isDarkTheme: Boolean,
    isImmersiveMode: Boolean = false,
    immersedRoomName: String? = null,
    onClearNavigation: () -> Unit,
    onExitImmersive: () -> Unit = {},
    onWalk: (Float) -> Unit = {},
    onTurn: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {

        // Top Immersive Room Banner (when stepped inside room)
        AnimatedVisibility(
            visible = isImmersiveMode,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) Color(0xEE0B132B) else Color(0xEEFFFFFF),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("hud_immersive_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ROOM INTERIOR IMMERSION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = immersedRoomName ?: "Room Interior",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Eye-level 360° view • Drag canvas to look around",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onExitImmersive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("exit_immersive_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Navigation Target Banner (when navigating and not in immersive mode)
        AnimatedVisibility(
            visible = navTarget != null && !isImmersiveMode,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            navTarget?.let { target ->
                val distMeters = Pathfinding3D.calculateDistanceMeters(userLocation, target.targetOffset)
                val bearing = Pathfinding3D.calculateBearingDeg(userLocation, target.targetOffset)
                val relativeBearing = (bearing - cameraYaw + 360f) % 360f

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xDD0B132B) else Color(0xEEFFFFFF),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("hud_nav_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Dynamic Turn Arrow
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(28.dp)) {
                                val rad = Math.toRadians((relativeBearing - 90.0))
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val arrowLength = size.width * 0.38f

                                val tipX = cx + cos(rad).toFloat() * arrowLength
                                val tipY = cy + sin(rad).toFloat() * arrowLength

                                val leftRad = rad + Math.PI * 0.82
                                val rightRad = rad - Math.PI * 0.82

                                val leftX = cx + cos(leftRad).toFloat() * (arrowLength * 0.55f)
                                val leftY = cy + sin(leftRad).toFloat() * (arrowLength * 0.55f)

                                val rightX = cx + cos(rightRad).toFloat() * (arrowLength * 0.55f)
                                val rightY = cy + sin(rightRad).toFloat() * (arrowLength * 0.55f)

                                val path = Path().apply {
                                    moveTo(tipX, tipY)
                                    lineTo(leftX, leftY)
                                    lineTo(cx, cy)
                                    lineTo(rightX, rightY)
                                    close()
                                }
                                drawPath(path, color = Color(0xFF00E5FF))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Target Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HUD WAYFINDING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (target.tag != null) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.Black
                                    ) {
                                        Text(text = target.tag, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = target.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Room: ${target.roomName} • Heading: ${bearing.toInt()}°",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Distance readout
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("%.1f m", distMeters),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = onClearNavigation,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("hud_clear_nav_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Navigation",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top-Right Compass Radar Dial
        CompassRadar(
            cameraYaw = cameraYaw,
            navBearing = navTarget?.let { Pathfinding3D.calculateBearingDeg(userLocation, it.targetOffset) },
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (navTarget != null || isImmersiveMode) 88.dp else 12.dp, end = 12.dp)
        )

        // Immersion Walk & Turn Controls (Floating on right side)
        if (isImmersiveMode) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0xCC141F36) else Color(0xCCFFFFFF),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Step Forward
                    IconButton(
                        onClick = { onWalk(1.5f) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .testTag("walk_forward_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Step Forward", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Turn Left
                        IconButton(
                            onClick = { onTurn(-30f) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("turn_left_btn")
                        ) {
                            Icon(imageVector = Icons.Default.RotateLeft, contentDescription = "Look Left", modifier = Modifier.size(18.dp))
                        }

                        // Turn Right
                        IconButton(
                            onClick = { onTurn(30f) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("turn_right_btn")
                        ) {
                            Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Look Right", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Step Backward
                    IconButton(
                        onClick = { onWalk(-1.5f) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("walk_backward_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Step Back", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CompassRadar(
    cameraYaw: Float,
    navBearing: Float?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(if (isDarkTheme) Color(0xCC141F36) else Color(0xCCFFFFFF))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            .testTag("hud_compass_radar"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width / 2f - 4f

            // Radar concentric rings
            drawCircle(
                color = if (isDarkTheme) Color(0x3300E5FF) else Color(0x220284C7),
                radius = radius,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = if (isDarkTheme) Color(0x2200E5FF) else Color(0x180284C7),
                radius = radius * 0.6f,
                style = Stroke(width = 0.8f)
            )

            // North needle rotated by camera yaw
            val northAngleRad = Math.toRadians((-cameraYaw - 90.0))
            val nTipX = cx + cos(northAngleRad).toFloat() * (radius - 2f)
            val nTipY = cy + sin(northAngleRad).toFloat() * (radius - 2f)

            val sAngleRad = Math.toRadians((-cameraYaw + 90.0))
            val sTipX = cx + cos(sAngleRad).toFloat() * (radius - 6f)
            val sTipY = cy + sin(sAngleRad).toFloat() * (radius - 6f)

            // Red North Needle
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(cx, cy),
                end = Offset(nTipX, nTipY),
                strokeWidth = 3f
            )

            // White South Needle
            drawLine(
                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                start = Offset(cx, cy),
                end = Offset(sTipX, sTipY),
                strokeWidth = 2f
            )

            // If navigating to target, draw glowing cyan waypoint bearing dot on radar
            if (navBearing != null) {
                val targetAngleRad = Math.toRadians((navBearing - cameraYaw - 90.0))
                val dotX = cx + cos(targetAngleRad).toFloat() * (radius - 3f)
                val dotY = cy + sin(targetAngleRad).toFloat() * (radius - 3f)

                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 4.5f,
                    center = Offset(dotX, dotY)
                )
            }

            // Center core
            drawCircle(
                color = if (isDarkTheme) Color.White else Color.Black,
                radius = 2.5f,
                center = Offset(cx, cy)
            )
        }

        // 'N' Label
        Text(
            text = "N",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEF4444),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
        )
    }
}
