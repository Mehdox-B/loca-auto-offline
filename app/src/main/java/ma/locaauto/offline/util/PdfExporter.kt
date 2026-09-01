package ma.locaauto.offline.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import ma.locaauto.offline.data.Car
import ma.locaauto.offline.data.Client
import ma.locaauto.offline.data.Contract
import ma.locaauto.offline.data.Invoice
import ma.locaauto.offline.data.Reservation
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    private const val CONTRACT_TEMPLATE_ASSET = "Exemple_contrat_location_app_auto.pdf"
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f

    /** Fills the bilingual Fahd Car scan because the source PDF has no AcroForm fields. */
    fun exportContract(context: Context, contract: Contract, reservation: Reservation, client: Client, car: Car): String = runCatching {
        val template = templateFile(context)
        val descriptor = ParcelFileDescriptor.open(template, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val document = PdfDocument()
        try {
            for (pageIndex in 0 until renderer.pageCount) {
                val sourcePage = renderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(sourcePage.width * 2, sourcePage.height * 2, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                sourcePage.close()

                val outputPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageIndex + 1).create())
                outputPage.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, PAGE_WIDTH, PAGE_HEIGHT), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                if (pageIndex == 0) fillFirstPage(outputPage.canvas, contract, reservation, client, car)
                document.finishPage(outputPage)
                bitmap.recycle()
            }
            save(context, document, "${safeName(contract.number)}.pdf")
        } finally {
            document.close()
            renderer.close()
            descriptor.close()
        }
    }.getOrElse { "Export impossible : ${it.message}" }

    /** Compatibility overload for callers that only have display strings. */
    fun exportContract(context: Context, contract: Contract, client: String, vehicle: String): String = runCatching {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create())
        header(page.canvas, "CONTRAT DE LOCATION", contract.number)
        var y = 205f
        y = section(page.canvas, "Informations du contrat", y)
        y = line(page.canvas, "Client : $client", y)
        y = line(page.canvas, "Véhicule : $vehicle", y)
        line(page.canvas, "État : ${contract.status}", y)
        footer(page.canvas)
        document.finishPage(page)
        save(context, document, "${safeName(contract.number)}.pdf")
    }.getOrElse { "Export impossible : ${it.message}" }

    fun exportInvoice(context: Context, invoice: Invoice, client: String, vehicle: String): String = runCatching {
        val document = PdfDocument(); val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas; header(canvas, "FACTURE", invoice.number)
        var y = 205f
        y = section(canvas, "Détails", y); y = line(canvas, "Client : $client", y); y = line(canvas, "Véhicule : $vehicle", y); y = line(canvas, "Date : ${date(invoice.issuedAt)}", y)
        y += 18; y = section(canvas, "Montants", y); y = amount(canvas, "Sous-total HT", invoice.subtotal, y); y = amount(canvas, "TVA", invoice.tax, y); y = amount(canvas, "Total TTC", invoice.total, y)
        y += 18; line(canvas, "Paiement : ${invoice.paymentStatus} • ${invoice.paymentMethod}", y)
        footer(canvas); document.finishPage(page); save(context, document, "${safeName(invoice.number)}.pdf")
    }.getOrElse { "Export impossible : ${it.message}" }

    private fun fillFirstPage(canvas: Canvas, contract: Contract, reservation: Reservation, client: Client, car: Car) {
        val nameParts = client.fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val lastName = nameParts.lastOrNull().orEmpty()
        val firstName = nameParts.dropLast(1).joinToString(" ")
        val addressLines = client.address.trim().chunked(45)

        field(canvas, RectF(300f, 46f, 420f, 74f), contract.number, 310f, 65f, 14f, bold = true)
        field(canvas, RectF(300f, 76f, 420f, 98f), "RES-${reservation.id}", 310f, 91f, 12f, bold = true)
        field(canvas, RectF(105f, 101f, 280f, 124f), "${car.brand} ${car.model}", 120f, 117f, 11f, bold = true)
        field(canvas, RectF(360f, 101f, 435f, 124f), car.licensePlate, 368f, 117f, 11f, bold = true)
        field(canvas, RectF(170f, 123f, 285f, 146f), "FBS", 180f, 139f, 11f)
        field(canvas, RectF(170f, 146f, 285f, 169f), "FBS", 180f, 162f, 11f)
        field(canvas, RectF(458f, 101f, 515f, 124f), date(reservation.startDate), 462f, 117f, 9f, bold = true)
        field(canvas, RectF(520f, 101f, 557f, 124f), time(reservation.startDate), 523f, 117f, 9f, bold = true)
        field(canvas, RectF(458f, 124f, 515f, 147f), date(reservation.endDate), 462f, 140f, 9f, bold = true)
        field(canvas, RectF(520f, 124f, 557f, 147f), time(reservation.endDate), 523f, 140f, 9f, bold = true)
        field(canvas, RectF(435f, 147f, 557f, 169f), reservation.totalDays.toString(), 490f, 162f, 11f, bold = true)

        field(canvas, RectF(135f, 188f, 285f, 211f), lastName, 155f, 204f, 11f, bold = true)
        field(canvas, RectF(135f, 210f, 285f, 233f), firstName, 155f, 226f, 11f, bold = true)
        field(canvas, RectF(130f, 232f, 285f, 253f), "", 150f, 247f, 10f)
        white(canvas, RectF(128f, 252f, 285f, 290f))
        field(canvas, RectF(135f, 253f, 285f, 272f), addressLines.getOrNull(0).orEmpty(), 145f, 267f, 9f)
        field(canvas, RectF(135f, 272f, 285f, 291f), addressLines.getOrNull(1).orEmpty(), 145f, 286f, 9f)
        field(canvas, RectF(135f, 294f, 285f, 316f), client.identityNumber, 155f, 309f, 11f, bold = true)
        field(canvas, RectF(135f, 318f, 285f, 340f), "", 155f, 333f, 10f)
        field(canvas, RectF(135f, 349f, 285f, 372f), client.driverLicenseNumber, 155f, 365f, 11f, bold = true)
        field(canvas, RectF(135f, 392f, 285f, 414f), client.phone, 155f, 408f, 10f, bold = true)

        field(canvas, RectF(135f, 416f, 220f, 439f), money(reservation.dailyRate), 155f, 432f, 11f, bold = true)
        field(canvas, RectF(135f, 441f, 220f, 464f), "0.00", 155f, 457f, 11f, bold = true)
        field(canvas, RectF(135f, 466f, 220f, 489f), money(reservation.totalPrice), 155f, 482f, 11f, bold = true)
        field(canvas, RectF(455f, 416f, 557f, 440f), contract.startMileage.toString(), 505f, 432f, 11f, bold = true)
        field(canvas, RectF(455f, 441f, 557f, 465f), contract.endMileage?.toString().orEmpty(), 505f, 457f, 11f, bold = true)
        white(canvas, RectF(145f, 500f, 550f, 560f))
        field(canvas, RectF(150f, 501f, 545f, 522f), reservation.options, 155f, 516f, 9f)
    }

    private fun templateFile(context: Context): File {
        val target = File(context.cacheDir, CONTRACT_TEMPLATE_ASSET)
        if (!target.exists() || target.length() == 0L) {
            context.assets.open(CONTRACT_TEMPLATE_ASSET).use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
        }
        return target
    }

    private fun field(canvas: Canvas, area: RectF, value: String, x: Float, baseline: Float, size: Float, bold: Boolean = false) {
        white(canvas, area)
        if (value.isBlank()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = size; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT }
        canvas.drawText(value.take(54), x, baseline, paint)
    }

    private fun white(canvas: Canvas, area: RectF) {
        canvas.drawRect(area, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })
    }

    private fun header(canvas: Canvas, title: String, number: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(11, 110, 105); style = Paint.Style.FILL }
        canvas.drawRect(40f, 40f, 145f, 135f, paint)
        paint.color = Color.WHITE; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = 22f; canvas.drawText("LOCA", 57f, 82f, paint); canvas.drawText("AUTO", 57f, 112f, paint)
        paint.color = Color.rgb(25, 35, 34); paint.textSize = 26f; canvas.drawText(title, 175f, 75f, paint); paint.textSize = 15f; paint.color = Color.DKGRAY; canvas.drawText(number, 175f, 105f, paint)
        paint.color = Color.LTGRAY; canvas.drawRect(40f, 160f, 555f, 162f, paint)
    }

    private fun section(canvas: Canvas, title: String, y: Float): Float { val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(11, 110, 105); textSize = 16f; typeface = Typeface.DEFAULT_BOLD }; canvas.drawText(title, 45f, y, p); return y + 32f }
    private fun line(canvas: Canvas, text: String, y: Float): Float { val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 14f }; canvas.drawText(text, 45f, y, p); return y + 25f }
    private fun amount(canvas: Canvas, label: String, value: Double, y: Float): Float { val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 14f }; canvas.drawText(label, 45f, y, p); canvas.drawText("${money(value)} MAD", 420f, y, p); return y + 25f }
    private fun footer(canvas: Canvas) { val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 10f; textAlign = Paint.Align.CENTER }; canvas.drawText("LocaAuto Offline • Document généré sur l'appareil", 297f, 800f, p) }
    private fun save(context: Context, document: PdfDocument, name: String): String { val dir = context.getExternalFilesDir("Documents") ?: context.filesDir; val file = File(dir, name); FileOutputStream(file).use { document.writeTo(it) }; return "Enregistré localement : ${file.absolutePath}" }
    private fun safeName(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun money(value: Double) = String.format(Locale.FRANCE, "%.2f", value)
    private fun date(value: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(value))
    private fun time(value: Long) = SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(value))
}
