package com.example.data.export

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.GabbaiUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxExporter {
    private val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun share(context: Context, state: GabbaiUiState) {
        val sheets = listOf(
            "Members" to listOf(listOf("ID","Name","Phone","Role","Status","Seat","Annual fee","Debt")) + state.members.map { listOf(it.id,it.fullName,it.phone,it.role.titleKa,it.status.titleKa,it.seatNumber,it.annualFeeAmount,it.outstandingDebt) },
            "Finances" to listOf(listOf("ID","Title","Type","Category","Amount","Person","Method","Date","Status")) + state.transactions.map { listOf(it.id,it.title,it.type.name,it.category.titleKa,it.amount,it.memberName,it.paymentMethod,date.format(Date(it.dateMillis)),it.status.name) },
            "Aliyot" to listOf(listOf("ID","Parasha","Aliyah","Winner","Amount","Date","Status")) + state.aliyot.map { listOf(it.id,it.parashaName,it.aliyahType.titleKa,it.winnerName,it.amount,date.format(Date(it.dateMillis)),it.status.name) },
            "Kollel" to listOf(listOf("ID","Student","Phone","Masechet","Stipend","Paid","Attendance")) + state.kollelStudents.map { listOf(it.id,it.fullName,it.phone,it.currentMasechet,it.monthlyStipend,it.stipendPaidThisMonth,"${it.totalSessionsPresent}/${it.totalSessionsExpected}") },
            "Attendance" to listOf(listOf("ID","Student","Date","Morning","Afternoon","Remarks")) + state.kollelAttendance.map { listOf(it.id,it.studentName,date.format(Date(it.dateMillis)),it.sederMorning,it.sederAfternoon,it.remarks) },
            "Tzedakah" to listOf(listOf("ID","Code","Category","Amount","Date","Emergency","Approved by")) + state.tzedakahFunds.map { listOf(it.id,it.recipientPrivacyCode,it.category.titleKa,it.amount,date.format(Date(it.dateMillis)),it.isEmergency,it.approvedByRabbi) },
            "Beneficiaries" to listOf(listOf("ID","Code","Name","Category","Family","Phone","Address","Need","Approved","Status")) + state.needyBeneficiaries.map { listOf(it.id,it.identifierCode,it.fullNameOrPseudonym,it.category.titleKa,it.familyMembersCount,it.phone,it.address,it.monthlyEstimatedNeed,it.monthlyApprovedAid,it.status.titleKa) },
            "Events" to listOf(listOf("ID","Title","Type","Date","Sponsor","Cost","Contribution","Guests","Completed")) + state.events.map { listOf(it.id,it.title,it.eventType,date.format(Date(it.dateMillis)),it.sponsorName,it.totalCost,it.sponsorContribution,it.expectedGuests,it.isCompleted) }
        )
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Gabbai_Cosmos_${System.currentTimeMillis()}.xlsx")
        writeWorkbook(file, sheets)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, L("Gabbai Cosmos — სრული მონაცემები"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Google Sheets / Excel"))
    }

    private fun writeWorkbook(file: File, sheets: List<Pair<String,List<List<Any?>>>>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun entry(name: String, content: String) { zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray()); zip.closeEntry() }
            entry("[Content_Types].xml", contentTypes(sheets.size))
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml", workbookXml(sheets))
            entry("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            entry("xl/styles.xml", stylesXml())
            sheets.forEachIndexed { i, (_, rows) -> entry("xl/worksheets/sheet${i+1}.xml", sheetXml(rows)) }
        }
    }

    private fun esc(v: Any?) = (v?.toString() ?: "").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    private fun col(n: Int): String { var x=n+1; var s=""; while(x>0){ x--; s=('A'.code+x%26).toChar()+s; x/=26 }; return s }
    private fun sheetXml(rows: List<List<Any?>>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews><cols>""")
        val count = rows.maxOfOrNull { it.size } ?: 1
        repeat(count) { append("<col min=\"${it+1}\" max=\"${it+1}\" width=\"20\" customWidth=\"1\"/>") }
        append("</cols><sheetData>")
        rows.forEachIndexed { r, row -> append("<row r=\"${r+1}\">"); row.forEachIndexed { c, value -> append("<c r=\"${col(c)}${r+1}\" t=\"inlineStr\" s=\"${if(r==0) 1 else 0}\"><is><t>${esc(value)}</t></is></c>") }; append("</row>") }
        if(rows.isNotEmpty()) append("</sheetData><autoFilter ref=\"A1:${col(count-1)}${rows.size}\"/></worksheet>") else append("</sheetData></worksheet>")
    }
    private fun contentTypes(n:Int) = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>${(1..n).joinToString(""){"<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"}}</Types>"""
    private fun workbookXml(s:List<Pair<String,*>>) = """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>${s.mapIndexed{i,p->"<sheet name=\"${esc(p.first)}\" sheetId=\"${i+1}\" r:id=\"rId${i+1}\"/>"}.joinToString("")}</sheets></workbook>"""
    private fun workbookRels(n:Int) = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">${(1..n).joinToString(""){"<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>"}}<Relationship Id="rId${n+1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Arial"/></font><font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Arial"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF087E8B"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border/></borders><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs></styleSheet>"""
}
