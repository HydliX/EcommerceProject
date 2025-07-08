package com.example.ecommerceproject.leader

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.*

suspend fun exportChartToPdf(
    context: Context,
    title: String,
    data: List<Pair<String, Int>>,
    chartType: String,
    snackbarHostState: SnackbarHostState
) {
    withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Laporan_${title.replace(" ", "")}_$timeStamp.pdf"
        val storageDir = File(context.getExternalFilesDir(null), "Laporan")
        if (!storageDir.exists()) storageDir.mkdirs()
        val file = File(storageDir, fileName)

        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background color
            canvas.drawColor(android.graphics.Color.parseColor("#F8F9FA"))

            // Header with rounded background
            val headerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#6366F1")
                isAntiAlias = true
            }
            val headerRect = android.graphics.RectF(30f, 30f, 565f, 80f)
            canvas.drawRoundRect(headerRect, 16f, 16f, headerPaint)

            // Title
            val titlePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Laporan $title", 297.5f, 60f, titlePaint)

            // Chart area with card background
            val cardPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = true
                setShadowLayer(8f, 2f, 2f, android.graphics.Color.parseColor("#20000000"))
            }
            val cardRect = android.graphics.RectF(40f, 100f, 555f, 420f)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)

            when (chartType) {
                "bar" -> drawEnhancedBarChartOnPdf(canvas, data)
                "line" -> drawEnhancedLineChartOnPdf(canvas, data)
                "pie" -> drawEnhancedPieChartOnPdf(canvas, data)
            }

            // Data table with enhanced styling
            drawEnhancedDataTable(canvas, data, title)

            // Footer
            val footerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#6B7280")
                textSize = 10f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val currentTime = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
            canvas.drawText("Generated on $currentTime", 297.5f, 820f, footerPaint)

            document.finishPage(page)
            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("PDF berhasil dibuat!")
                openFile(context, file, "application/pdf")
            }
        } catch (e: Exception) {
            Log.e("PdfExport", "Gagal: ${e.message}", e)
            withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Gagal: ${e.message}") }
        }
    }
}

fun drawEnhancedBarChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return

    val maxQuantity = data.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val chartRect = android.graphics.RectF(70f, 130f, 525f, 380f)
    val barColors = listOf(
        android.graphics.Color.parseColor("#6366F1"),
        android.graphics.Color.parseColor("#8B5CF6"),
        android.graphics.Color.parseColor("#06B6D4"),
        android.graphics.Color.parseColor("#10B981"),
        android.graphics.Color.parseColor("#F59E0B")
    )

    // Draw axes
    val axisPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Y-axis
    canvas.drawLine(chartRect.left, chartRect.top, chartRect.left, chartRect.bottom, axisPaint)
    // X-axis
    canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint)

    // Draw grid lines and Y-axis labels
    val yLabelCount = 5
    val maxLabelValue = ((maxQuantity.toInt() / yLabelCount + 1) * yLabelCount).coerceAtLeast(1)
    val gridPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E5E7EB")
        strokeWidth = 1f
    }
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        textAlign = android.graphics.Paint.Align.RIGHT
        isAntiAlias = true
    }

    for (i in 0..yLabelCount) {
        val quantity = (maxLabelValue * i / yLabelCount).toInt()
        val yPosition = chartRect.bottom - (quantity / maxQuantity) * chartRect.height()

        if (i > 0) {
            canvas.drawLine(chartRect.left, yPosition, chartRect.right, yPosition, gridPaint)
        }
        canvas.drawText(quantity.toString(), chartRect.left - 8f, yPosition + 4f, labelPaint)
    }

    // Calculate bar dimensions
    val totalWidth = chartRect.width()
    val barWidth = (totalWidth / data.size) * 0.6f
    val spacing = (totalWidth - (barWidth * data.size)) / (data.size + 1)

    // Draw bars with shadows and gradients
    data.forEachIndexed { index, (name, quantity) ->
        val barHeight = (quantity / maxQuantity) * chartRect.height()
        val left = chartRect.left + spacing + (index * (barWidth + spacing))
        val top = chartRect.bottom - barHeight

        // Shadow
        val shadowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#20000000")
            isAntiAlias = true
        }
        val shadowRect = android.graphics.RectF(left + 3f, top + 3f, left + barWidth + 3f, chartRect.bottom + 3f)
        canvas.drawRoundRect(shadowRect, 8f, 8f, shadowPaint)

        // Gradient effect simulation with multiple colors
        val baseColor = barColors[index % barColors.size]
        val lightColor = adjustColorBrightness(baseColor, 1.2f)

        // Main bar
        val barPaint = android.graphics.Paint().apply {
            color = baseColor
            isAntiAlias = true
        }
        val barRect = android.graphics.RectF(left, top, left + barWidth, chartRect.bottom)
        canvas.drawRoundRect(barRect, 8f, 8f, barPaint)

        // Light gradient overlay
        val gradientPaint = android.graphics.Paint().apply {
            color = lightColor
            alpha = 80
            isAntiAlias = true
        }
        val gradientRect = android.graphics.RectF(left, top, left + barWidth, top + barHeight * 0.3f)
        canvas.drawRoundRect(gradientRect, 8f, 8f, gradientPaint)

        // Value label above bar
        if (barHeight > 0) {
            val valuePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText(quantity.toString(), left + barWidth / 2, top - 8f, valuePaint)
        }

        // Product name label below bar
        val namePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(name.take(8), left + barWidth / 2, chartRect.bottom + 15f, namePaint)
    }
}

fun drawEnhancedLineChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.size < 2) return

    val maxQuantity = data.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val chartRect = android.graphics.RectF(70f, 130f, 525f, 380f)

    // Draw axes
    val axisPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2f
        isAntiAlias = true
    }
    canvas.drawLine(chartRect.left, chartRect.top, chartRect.left, chartRect.bottom, axisPaint)
    canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint)

    // Draw grid and Y-axis labels
    val yLabelCount = 5
    val maxLabelValue = ((maxQuantity.toInt() / yLabelCount + 1) * yLabelCount).coerceAtLeast(1)
    val gridPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E5E7EB")
        strokeWidth = 1f
    }
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        textAlign = android.graphics.Paint.Align.RIGHT
        isAntiAlias = true
    }

    for (i in 0..yLabelCount) {
        val quantity = (maxLabelValue * i / yLabelCount).toInt()
        val yPosition = chartRect.bottom - (quantity / maxQuantity) * chartRect.height()

        if (i > 0) {
            canvas.drawLine(chartRect.left, yPosition, chartRect.right, yPosition, gridPaint)
        }
        canvas.drawText(quantity.toString(), chartRect.left - 8f, yPosition + 4f, labelPaint)
    }

    // Calculate points
    val points = data.mapIndexed { index, (_, quantity) ->
        val x = chartRect.left + (index.toFloat() / (data.size - 1)) * chartRect.width()
        val y = chartRect.bottom - (quantity / maxQuantity) * chartRect.height()
        android.graphics.PointF(x, y)
    }

    // Draw shadow line
    val shadowPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#20000000")
        strokeWidth = 4f
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        pathEffect = android.graphics.CornerPathEffect(8f)
    }
    val shadowPath = android.graphics.Path()
    points.forEachIndexed { index, point ->
        if (index == 0) {
            shadowPath.moveTo(point.x + 2f, point.y + 2f)
        } else {
            shadowPath.lineTo(point.x + 2f, point.y + 2f)
        }
    }
    canvas.drawPath(shadowPath, shadowPaint)

    // Draw main line
    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#6366F1")
        strokeWidth = 4f
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        pathEffect = android.graphics.CornerPathEffect(8f)
    }
    val linePath = android.graphics.Path()
    points.forEachIndexed { index, point ->
        if (index == 0) {
            linePath.moveTo(point.x, point.y)
        } else {
            linePath.lineTo(point.x, point.y)
        }
    }
    canvas.drawPath(linePath, linePaint)

    // Draw points and labels
    points.forEachIndexed { index, point ->
        // Shadow circle
        val shadowCirclePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#20000000")
            isAntiAlias = true
        }
        canvas.drawCircle(point.x + 2f, point.y + 2f, 6f, shadowCirclePaint)

        // Main circle
        val circlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#6366F1")
            isAntiAlias = true
        }
        canvas.drawCircle(point.x, point.y, 6f, circlePaint)

        // Inner white circle
        val innerCirclePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(point.x, point.y, 3f, innerCirclePaint)

        // Value label
        val valuePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(data[index].second.toString(), point.x, point.y - 15f, valuePaint)

        // X-axis labels
        val xLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(data[index].first.take(8), point.x, chartRect.bottom + 15f, xLabelPaint)
    }
}

fun drawEnhancedPieChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.second }.toFloat()
    val center = android.graphics.PointF(297.5f, 255f)
    val radius = 100f
    var startAngle = -90f

    val colors = listOf(
        android.graphics.Color.parseColor("#E57373"),
        android.graphics.Color.parseColor("#4FC3F7"),
        android.graphics.Color.parseColor("#FFB300"),
        android.graphics.Color.parseColor("#81C784"),
        android.graphics.Color.parseColor("#BA68C8")
    )

    // Draw shadow
    val shadowPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#20000000")
        isAntiAlias = true
    }
    canvas.drawCircle(center.x + 4f, center.y + 4f, radius, shadowPaint)

    data.forEachIndexed { index, (name, quantity) ->
        val percentage = quantity / total
        val sweepAngle = percentage * 360f

        // Main slice
        val slicePaint = android.graphics.Paint().apply {
            color = colors[index % colors.size]
            isAntiAlias = true
        }
        val rect = android.graphics.RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius
        )
        canvas.drawArc(rect, startAngle, sweepAngle, true, slicePaint)

        // Highlight edge
        val highlightPaint = android.graphics.Paint().apply {
            color = adjustColorBrightness(colors[index % colors.size], 1.3f)
            isAntiAlias = true
        }
        val highlightRect = android.graphics.RectF(
            center.x - radius + 5f, center.y - radius + 5f,
            center.x + radius - 5f, center.y + radius - 5f
        )
        canvas.drawArc(highlightRect, startAngle, sweepAngle * 0.3f, true, highlightPaint)

        // Labels for significant segments
        if (percentage > 0.05f) {
            val midAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val labelRadius = radius * 0.7f
            val labelX = center.x + labelRadius * Math.cos(midAngle).toFloat()
            val labelY = center.y + labelRadius * Math.sin(midAngle).toFloat()

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isAntiAlias = true
                setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
            }

            val displayName = if (name.length > 8) name.take(8) + "..." else name
            canvas.drawText(displayName, labelX, labelY, labelPaint)
        }

        startAngle += sweepAngle
    }
}

fun drawEnhancedDataTable(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>, title: String) {
    val tableRect = android.graphics.RectF(40f, 440f, 555f, 750f)

    // Table background
    val tableBgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        setShadowLayer(4f, 1f, 1f, android.graphics.Color.parseColor("#20000000"))
    }
    canvas.drawRoundRect(tableRect, 12f, 12f, tableBgPaint)

    // Table title
    val tableTitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#374151")
        textSize = 16f
        textAlign = android.graphics.Paint.Align.LEFT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText("Detail Data $title", 55f, 465f, tableTitlePaint)

    // Header background
    val headerBgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#F3F4F6")
        isAntiAlias = true
    }
    val headerRect = android.graphics.RectF(50f, 475f, 545f, 500f)
    canvas.drawRoundRect(headerRect, 8f, 8f, headerBgPaint)

    // Table headers
    val headerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#374151")
        textSize = 12f
        textAlign = android.graphics.Paint.Align.LEFT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val headers = listOf("Peringkat", "Nama", "Jumlah", "Persentase")
    val columnPositions = listOf(65f, 140f, 350f, 450f)

    headers.forEachIndexed { index, header ->
        canvas.drawText(header, columnPositions[index], 492f, headerPaint)
    }

    // Table rows
    val rowPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#374151")
        textSize = 11f
        textAlign = android.graphics.Paint.Align.LEFT
        isAntiAlias = true
    }

    val totalQuantity = data.sumOf { it.second }
    var yPosition = 520f

    data.forEachIndexed { index, (name, quantity) ->
        // Alternate row background
        if (index % 2 == 1) {
            val rowBgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F9FAFB")
                isAntiAlias = true
            }
            val rowBgRect = android.graphics.RectF(50f, yPosition - 15f, 545f, yPosition + 5f)
            canvas.drawRoundRect(rowBgRect, 4f, 4f, rowBgPaint)
        }

        val percentage = if (totalQuantity > 0) (quantity.toFloat() / totalQuantity * 100) else 0f

        canvas.drawText((index + 1).toString(), columnPositions[0], yPosition, rowPaint)
        canvas.drawText(name, columnPositions[1], yPosition, rowPaint)
        canvas.drawText("$quantity Unit", columnPositions[2], yPosition, rowPaint)
        canvas.drawText("${String.format("%.1f", percentage)}%", columnPositions[3], yPosition, rowPaint)

        yPosition += 20f

        if (yPosition > 730f) return // Prevent overflow
    }
}

fun adjustColorBrightness(color: Int, factor: Float): Int {
    val red = ((android.graphics.Color.red(color) * factor).toInt().coerceAtMost(255))
    val green = ((android.graphics.Color.green(color) * factor).toInt().coerceAtMost(255))
    val blue = ((android.graphics.Color.blue(color) * factor).toInt().coerceAtMost(255))
    return android.graphics.Color.rgb(red, green, blue)
}

suspend fun exportToExcel(
    context: Context,
    title: String,
    data: List<Pair<String, Int>>,
    snackbarHostState: SnackbarHostState
) {
    withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Laporan_${title.replace(" ", "")}_$timeStamp.xlsx"
        val storageDir = File(context.getExternalFilesDir(null), "Laporan")
        if (!storageDir.exists()) storageDir.mkdirs()
        val file = File(storageDir, fileName)

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(title)

            // Create cell style for header
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setFont(workbook.createFont().apply {
                    bold = true
                    fontHeightInPoints = 12
                })
            }

            // Create cell style for alternating rows
            val alternateRowStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index // Use GREY_25_PERCENT as a fallback
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            // Table headers
            val headers = listOf("Peringkat", "Nama", "Jumlah", "Persentase")
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // Table data
            val totalQuantity = data.sumOf { it.second }
            data.forEachIndexed { index, (name, quantity) ->
                val row = sheet.createRow(index + 1)
                val percentage = if (totalQuantity > 0) (quantity.toFloat() / totalQuantity * 100) else 0f

                // Apply alternating row background
                if (index % 2 == 1) {
                    (0..3).forEach { cellIndex ->
                        val cell = row.createCell(cellIndex)
                        cell.cellStyle = alternateRowStyle
                    }
                }

                row.createCell(0).setCellValue((index + 1).toDouble())
                row.createCell(1).setCellValue(name)
                row.createCell(2).setCellValue("$quantity Unit")
                row.createCell(3).setCellValue("${String.format("%.1f", percentage)}%")
            }

            // Adjust column widths
            sheet.setColumnWidth(0, 25 * 256) // Peringkat
            sheet.setColumnWidth(1, 40 * 256) // Nama
            sheet.setColumnWidth(2, 25 * 256) // Jumlah
            sheet.setColumnWidth(3, 25 * 256) // Persentase

            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()

            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Excel berhasil dibuat!")
                openFile(
                    context,
                    file,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            }
        } catch (e: Exception) {
            Log.e("ExcelExport", "Gagal: ${e.message}", e)
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Gagal mengekspor Excel: ${e.message}")
            }
        }
    }
}

fun openFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Buka dengan"))
    } catch (e: Exception) {
        Log.e("OpenFile", "Tidak ada aplikasi untuk membuka file tipe $mimeType", e)
    }
}
