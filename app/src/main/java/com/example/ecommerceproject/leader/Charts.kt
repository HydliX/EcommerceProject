package com.example.ecommerceproject.leader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EnhancedBarChart(topProducts: List<Map<String, Any>>) {
    if (topProducts.isEmpty()) return

    val totalQuantity = topProducts.sumOf { it["quantity"] as? Int ?: 0 }.toFloat()
    val barWidth = 50.dp
    val chartHeight = 320.dp
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF06B6D4),
        Color(0xFF10B981), Color(0xFFF59E0B)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("BarChart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Chart Title
            Text(
                text = "Produk Terpopuler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val leftPadding = 80.dp.toPx()
                val rightPadding = 40.dp.toPx()
                val bottomPadding = 80.dp.toPx()
                val topPadding = 60.dp.toPx()
                val chartAreaWidth = canvasWidth - leftPadding - rightPadding
                val chartAreaHeight = canvasHeight - bottomPadding - topPadding

                val spacing = (chartAreaWidth - topProducts.size * barWidth.toPx()) / (topProducts.size + 1)
                val maxQuantity = topProducts.maxOfOrNull { it["quantity"] as? Int ?: 0 }?.toFloat() ?: 1f

                // Draw Y-axis
                drawLine(
                    start = Offset(leftPadding, topPadding),
                    end = Offset(leftPadding, canvasHeight - bottomPadding),
                    color = Color.Gray.copy(alpha = 0.6f),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw X-axis
                drawLine(
                    start = Offset(leftPadding, canvasHeight - bottomPadding),
                    end = Offset(canvasWidth - rightPadding, canvasHeight - bottomPadding),
                    color = Color.Gray.copy(alpha = 0.6f),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw Y-axis labels and grid lines
                val yLabelCount = 5
                val stepValue = (maxQuantity / yLabelCount).coerceAtLeast(1f)
                for (i in 0..yLabelCount) {
                    val quantity = (stepValue * i).toInt()
                    val yPosition = canvasHeight - bottomPadding - (quantity / maxQuantity) * chartAreaHeight

                    // Draw grid lines
                    if (i > 0) {
                        drawLine(
                            start = Offset(leftPadding, yPosition),
                            end = Offset(canvasWidth - rightPadding, yPosition),
                            color = Color.Gray.copy(alpha = 0.2f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Y-axis labels
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            quantity.toString(),
                            leftPadding - 16.dp.toPx(),
                            yPosition + 6.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                        )
                    }
                }

                // Draw bars and labels
                topProducts.forEachIndexed { index, product ->
                    val quantity = (product["quantity"] as? Int)?.toFloat() ?: 0f
                    val normalizedHeight = if (maxQuantity > 0) (quantity / maxQuantity) * chartAreaHeight else 0f
                    val xPosition = leftPadding + spacing * (index + 1) + barWidth.toPx() * index

                    // Draw bar shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.1f),
                        topLeft = Offset(xPosition + 4.dp.toPx(), canvasHeight - bottomPadding - normalizedHeight + 4.dp.toPx()),
                        size = Size(barWidth.toPx(), normalizedHeight),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )

                    // Draw main bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors[index % colors.size].copy(alpha = 0.9f),
                                colors[index % colors.size].copy(alpha = 0.7f)
                            )
                        ),
                        topLeft = Offset(xPosition, canvasHeight - bottomPadding - normalizedHeight),
                        size = Size(barWidth.toPx(), normalizedHeight),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )

                    // Draw quantity label on top of bar
                    if (normalizedHeight > 0) {
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                "${quantity.toInt()}",
                                xPosition + barWidth.toPx() / 2,
                                canvasHeight - bottomPadding - normalizedHeight - 16.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    textSize = 11.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    // Draw product name below X-axis
                    val productName = product["name"] as String
                    val displayName = if (productName.length > 10) productName.take(10) + "..." else productName

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            displayName,
                            xPosition + barWidth.toPx() / 2,
                            canvasHeight - bottomPadding + 24.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 11.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedLineChart(
    topProducts: List<Map<String, Any>>,
    dayLabels: List<String>,
    salesDataByDay: Map<String, Map<String, Int>>
) {
    if (topProducts.isEmpty()) return

    val maxDailyQuantity = dayLabels.flatMap { day ->
        topProducts.map { product ->
            salesDataByDay[day]?.get(product["name"] as String)?.toFloat() ?: 0f
        }
    }.maxOrNull() ?: 1f

    val chartHeight = 320.dp
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF06B6D4),
        Color(0xFF10B981), Color(0xFFF59E0B)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("LineChart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Chart Title
            Text(
                text = "Tren Produk Terpopuler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val leftPadding = 80.dp.toPx()
                val rightPadding = 40.dp.toPx()
                val bottomPadding = 80.dp.toPx()
                val topPadding = 60.dp.toPx()
                val chartAreaWidth = canvasWidth - leftPadding - rightPadding
                val chartAreaHeight = canvasHeight - bottomPadding - topPadding

                val pointSpacing = if (dayLabels.size > 1) chartAreaWidth / (dayLabels.size - 1) else 0f
                val productPoints = topProducts.map { mutableListOf<Offset>() }

                // Prepare points for each product
                dayLabels.forEachIndexed { index, day ->
                    topProducts.forEachIndexed { productIndex, product ->
                        val productName = product["name"] as String
                        val quantity = salesDataByDay[day]?.get(productName)?.toFloat() ?: 0f
                        val normalizedHeight = if (maxDailyQuantity > 0) (quantity / maxDailyQuantity) * chartAreaHeight else 0f
                        val xPosition = leftPadding + index * pointSpacing
                        val point = Offset(xPosition, canvasHeight - bottomPadding - normalizedHeight)
                        productPoints[productIndex].add(point)
                    }
                }

                // Draw Y-axis
                drawLine(
                    start = Offset(leftPadding, topPadding),
                    end = Offset(leftPadding, canvasHeight - bottomPadding),
                    color = Color.Gray.copy(alpha = 0.6f),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw X-axis
                drawLine(
                    start = Offset(leftPadding, canvasHeight - bottomPadding),
                    end = Offset(canvasWidth - rightPadding, canvasHeight - bottomPadding),
                    color = Color.Gray.copy(alpha = 0.6f),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw Y-axis labels and grid
                val yLabelCount = 5
                val stepValue = (maxDailyQuantity / yLabelCount).coerceAtLeast(1f)
                for (i in 0..yLabelCount) {
                    val quantity = (stepValue * i).toInt()
                    val yPosition = canvasHeight - bottomPadding - (quantity / maxDailyQuantity) * chartAreaHeight

                    if (i > 0) {
                        drawLine(
                            start = Offset(leftPadding, yPosition),
                            end = Offset(canvasWidth - rightPadding, yPosition),
                            color = Color.Gray.copy(alpha = 0.2f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            quantity.toString(),
                            leftPadding - 16.dp.toPx(),
                            yPosition + 6.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                        )
                    }
                }

                // Draw X-axis labels
                dayLabels.forEachIndexed { index, day ->
                    val xPosition = leftPadding + index * pointSpacing
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            day,
                            xPosition,
                            canvasHeight - bottomPadding + 24.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 11.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }
                }

                // Draw lines and points for each product
                topProducts.forEachIndexed { productIndex, product ->
                    val points = productPoints[productIndex]

                    if (points.size > 1) {
                        // Draw line shadow
                        val shadowPath = Path().apply {
                            points.forEachIndexed { index, point ->
                                if (index == 0) moveTo(point.x + 3.dp.toPx(), point.y + 3.dp.toPx())
                                else lineTo(point.x + 3.dp.toPx(), point.y + 3.dp.toPx())
                            }
                        }
                        drawPath(
                            path = shadowPath,
                            color = Color.Black.copy(alpha = 0.1f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw main line
                        val linePath = Path().apply {
                            points.forEachIndexed { index, point ->
                                if (index == 0) {
                                    moveTo(point.x, point.y)
                                } else {
                                    lineTo(point.x, point.y)
                                }
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = colors[productIndex % colors.size],
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Draw points
                    points.forEach { point ->
                        // Draw point shadow
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.1f),
                            radius = 6.dp.toPx(),
                            center = point + Offset(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Draw main point
                        drawCircle(
                            color = colors[productIndex % colors.size],
                            radius = 6.dp.toPx(),
                            center = point
                        )

                        // Draw inner point
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = point
                        )
                    }
                }
            }

            // Legend
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(topProducts) { index, product ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                colors[index % colors.size].copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    colors[index % colors.size],
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(product["name"] as String).take(10)}: ${product["quantity"]}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedPieChart(topManagers: List<Pair<String, Int>>) {
    if (topManagers.isEmpty()) return

    val totalOrders = topManagers.sumOf { it.second }.toFloat()
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF4FC3F7), Color(0xFFFFB300),
        Color(0xFF81C784), Color(0xFFBA68C8)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("PieChart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Chart Title
            Text(
                text = "Top Managers Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(24.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2 - 60.dp.toPx()).coerceAtLeast(80.dp.toPx())
                var startAngle = -90f

                // Draw pie chart shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.1f),
                    radius = radius,
                    center = center + Offset(6.dp.toPx(), 6.dp.toPx())
                )

                topManagers.forEachIndexed { index, (managerName, quantity) ->
                    val percentage = quantity / totalOrders
                    val sweepAngle = percentage * 360f

                    // Draw pie slice
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors[index % colors.size].copy(alpha = 0.9f),
                                colors[index % colors.size].copy(alpha = 0.7f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Draw percentage labels for significant segments
                    if (percentage > 0.05f) {
                        val midAngle = startAngle + sweepAngle / 2
                        val labelRadius = radius * 0.7f
                        val labelX = center.x + labelRadius * cos(Math.toRadians(midAngle.toDouble())).toFloat()
                        val labelY = center.y + labelRadius * sin(Math.toRadians(midAngle.toDouble())).toFloat()

                        val percentageText = "${(percentage * 100).toInt()}%"

                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                percentageText,
                                labelX,
                                labelY,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 12.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    startAngle += sweepAngle
                }
            }

            // Legend
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(topManagers) { index, (managerName, quantity) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                colors[index % colors.size].copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    colors[index % colors.size],
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${managerName.take(10)}: $quantity",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}