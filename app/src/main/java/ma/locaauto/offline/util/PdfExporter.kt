package ma.locaauto.offline.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import ma.locaauto.offline.data.Contract
import ma.locaauto.offline.data.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    fun exportContract(context: Context, contract: Contract, client: String, vehicle: String): String = runCatching {
        val document = PdfDocument(); val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas; header(canvas, "CONTRAT DE LOCATION", contract.number)
        var y = 205f
        y = section(canvas, "Informations du contrat", y)
        y = line(canvas, "Client : $client", y); y = line(canvas, "Véhicule : $vehicle", y); y = line(canvas, "État : ${contract.status}", y)
        y += 18; y = section(canvas, "Départ", y); y = line(canvas, "Kilométrage : ${contract.startMileage} km", y); y = line(canvas, "Carburant : ${contract.startFuel}%", y)
        y += 18; y = section(canvas, "Garantie", y); line(canvas, "Dépôt de garantie : ${money(contract.depositAmount)} MAD", y)
        footer(canvas); document.finishPage(page); save(context, document, "${contract.number}.pdf")
    }.getOrElse { "Export impossible : ${it.message}" }

    fun exportInvoice(context: Context, invoice: Invoice, client: String, vehicle: String): String = runCatching {
        val document = PdfDocument(); val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas; header(canvas, "FACTURE", invoice.number)
        var y = 205f
        y = section(canvas, "Détails", y); y = line(canvas, "Client : $client", y); y = line(canvas, "Véhicule : $vehicle", y); y = line(canvas, "Date : ${date(invoice.issuedAt)}", y)
        y += 18; y = section(canvas, "Montants", y); y = amount(canvas, "Sous-total HT", invoice.subtotal, y); y = amount(canvas, "TVA", invoice.tax, y); y = amount(canvas, "Total TTC", invoice.total, y)
        y += 18; line(canvas, "Paiement : ${invoice.paymentStatus} • ${invoice.paymentMethod}", y)
        footer(canvas); document.finishPage(page); save(context, document, "${invoice.number}.pdf")
    }.getOrElse { "Export impossible : ${it.message}" }

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
    private fun save(context: Context, document: PdfDocument, name: String): String { val dir = context.getExternalFilesDir("Documents") ?: context.filesDir; val file = File(dir, name); FileOutputStream(file).use { document.writeTo(it) }; document.close(); return "Enregistré localement : ${file.absolutePath}" }
    private fun money(value: Double) = String.format(Locale.FRANCE, "%.2f", value)
    private fun date(value: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(value))
}
