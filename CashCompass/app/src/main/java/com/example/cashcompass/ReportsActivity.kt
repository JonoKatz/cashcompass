package com.example.cashcompass

import ExpenseFirebase
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var firebaseDatabase: DatabaseReference
    private val categoryTotals = mutableMapOf<String, Double>()

    private var minGoal: Float = 0f
    private var maxGoal: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        minGoal = prefs.getFloat("minGoal", 0f)
        maxGoal = prefs.getFloat("budget", 0f)

        barChart = findViewById(R.id.barChart)
        barChart.setNoDataText("Loading chart...")
        barChart.setNoDataTextColor(Color.WHITE)

        val backButton: Button = findViewById(R.id.buttonBack)
        backButton.setOnClickListener { finish() }

        val downloadButton: Button = findViewById(R.id.buttonDownloadStatement)
        downloadButton.setOnClickListener { generateBankStatementPdf() }

        firebaseDatabase = FirebaseDatabase.getInstance().getReference("expenses")
        loadExpensesFromFirebase()
    }

    private fun loadExpensesFromFirebase() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val username = prefs.getString("loggedInUser", "defaultUser") ?: "defaultUser"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        firebaseDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryTotals.clear()

                val thirtyDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }.time

                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(ExpenseFirebase::class.java)
                    if (expense != null && expense.userId == username) {
                        try {
                            val expenseDate = dateFormat.parse(expense.date)
                            if (expenseDate != null && expenseDate.after(thirtyDaysAgo)) {
                                val category = expense.category
                                val amount = expense.amount
                                categoryTotals[category] = (categoryTotals[category] ?: 0.0) + amount
                            }
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "Invalid date format: ${expense.date}")
                        }
                    }
                }

                if (categoryTotals.isEmpty()) {
                    Toast.makeText(this@ReportsActivity, "No expenses in the last 30 days.", Toast.LENGTH_SHORT).show()
                }

                populateBarChart()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ReportsActivity", "Firebase read failed", error.toException())
            }
        })
    }

    private fun populateBarChart() {
        val entries = mutableListOf<BarEntry>()
        val categories = categoryTotals.keys.toList()

        for ((index, category) in categories.withIndex()) {
            entries.add(BarEntry(index.toFloat(), categoryTotals[category]?.toFloat() ?: 0f))
        }

        val dataSet = BarDataSet(entries, "Spending by Category")
        dataSet.color = resources.getColor(android.R.color.holo_orange_light, null)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f

        val data = BarData(dataSet)
        data.barWidth = 0.6f

        barChart.data = data
        barChart.setFitBars(true)
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setPinchZoom(false)
        barChart.animateY(1500)

        val formattedCategories = categories.map {
            if (it.length > 10) it.chunked(10).joinToString("\n") else it
        }

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(formattedCategories)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 12f
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = categories.size
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isGranularityEnabled = true
        xAxis.labelRotationAngle = 0f

        val yAxis = barChart.axisLeft
        yAxis.textColor = Color.WHITE
        yAxis.textSize = 12f
        yAxis.axisMinimum = 0f
        yAxis.setDrawGridLines(true)

        // 🧮 Get totals for lines
        val totalSpent = categoryTotals.values.sum().toFloat()

        // 🎯 Min Goal Line
        val minLine = LimitLine(minGoal, "Min Goal").apply {
            lineColor = Color.GREEN
            lineWidth = 2f
            textColor = Color.GREEN
            textSize = 12f
        }

        // 🛑 Max Goal Line
        val maxLine = LimitLine(maxGoal, "Max Goal").apply {
            lineColor = Color.RED
            lineWidth = 2f
            textColor = Color.RED
            textSize = 12f
        }

        // 📊 Total Spent Line
        val totalLine = LimitLine(totalSpent, "Total Spent").apply {
            lineColor = Color.YELLOW
            lineWidth = 2f
            textColor = Color.YELLOW
            textSize = 12f
        }

        // Clear and apply goal lines
        yAxis.removeAllLimitLines()
        yAxis.addLimitLine(minLine)
        yAxis.addLimitLine(maxLine)
        yAxis.addLimitLine(totalLine)

        // Adjust y-axis max if needed
        val highestValue = (categoryTotals.values.maxOrNull() ?: 0.0).toFloat()
        val suggestedMax = maxOf(highestValue, maxGoal, totalSpent) * 1.2f
        yAxis.axisMaximum = suggestedMax

        barChart.axisRight.isEnabled = false

        val legend = barChart.legend
        legend.textColor = Color.WHITE
        legend.textSize = 14f
        legend.isWordWrapEnabled = true

        barChart.invalidate()

        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e is BarEntry) {
                    val index = e.x.toInt()
                    val category = categoryTotals.keys.toList().getOrNull(index)
                    if (category != null) {
                        showExpenseDrillDownDialog(category)
                    }
                }
            }

            override fun onNothingSelected() {}
        })

        val currency = getSharedPreferences("settings", MODE_PRIVATE).getString("currency", "R") ?: "R"
        Toast.makeText(
            this,
            "Total spent: $currency${"%.2f".format(totalSpent)} / $currency${"%.2f".format(maxGoal)}",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun showExpenseDrillDownDialog(category: String) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val username = prefs.getString("loggedInUser", "defaultUser") ?: "defaultUser"

        val dialogView = layoutInflater.inflate(R.layout.dialog_expense_details, null)
        val textViewDetails = dialogView.findViewById<TextView>(R.id.textViewExpenseDetails)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Expenses in $category")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.show()

        FirebaseDatabase.getInstance().getReference("expenses")
            .orderByChild("category")
            .equalTo(category)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allEntries = snapshot.children.mapNotNull { it.getValue(ExpenseFirebase::class.java) }
                        .filter { it.userId == username }

                    val message = if (allEntries.isNotEmpty()) {
                        allEntries.joinToString("\n\n") {
                            "\uD83D\uDCB5 Amount: R${"%.2f".format(it.amount)}\n\uD83D\uDCC5 Date: ${it.date}\n\uD83D\uDCDD ${it.description}"
                        }
                    } else {
                        "No expenses found."
                    }

                    textViewDetails.text = message
                }

                override fun onCancelled(error: DatabaseError) {
                    textViewDetails.text = "Failed to load data."
                }
            })
    }

    private fun generateBankStatementPdf() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val username = prefs.getString("loggedInUser", "defaultUser") ?: "defaultUser"

        FirebaseDatabase.getInstance().getReference("expenses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = snapshot.children.mapNotNull { it.getValue(ExpenseFirebase::class.java) }
                        .filter { it.userId == username }

                    if (entries.isEmpty()) {
                        Toast.makeText(this@ReportsActivity, "No expenses to include in statement.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val pdfDoc = PdfDocument()
                    val paint = android.graphics.Paint()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    var page = pdfDoc.startPage(pageInfo)
                    var canvas = page.canvas
                    var y = 50
                    var pageCount = 1

                    fun drawHeader() {
                        paint.textSize = 16f
                        paint.isFakeBoldText = true
                        canvas.drawText("CashCompass Bank Statement", 180f, y.toFloat(), paint)
                        paint.isFakeBoldText = false
                        y += 30
                        canvas.drawText("User: $username", 40f, y.toFloat(), paint)
                        y += 30
                        canvas.drawLine(40f, y.toFloat(), 550f, y.toFloat(), paint)
                        y += 30
                    }

                    drawHeader()

                    entries.forEach {
                        if (y > 750) {
                            paint.textSize = 12f
                            canvas.drawText("Page $pageCount", 500f, 820f, paint)
                            pdfDoc.finishPage(page)

                            page = pdfDoc.startPage(pageInfo)
                            canvas = page.canvas
                            y = 50
                            pageCount++
                            drawHeader()
                        }

                        paint.textSize = 14f
                        canvas.drawText("Date: ${it.date}", 40f, y.toFloat(), paint)
                        y += 20
                        canvas.drawText("Category: ${it.category}", 40f, y.toFloat(), paint)
                        y += 20
                        canvas.drawText("Amount: R${"%.2f".format(it.amount)}", 40f, y.toFloat(), paint)
                        y += 20
                        canvas.drawText("Description: ${it.description}", 40f, y.toFloat(), paint)
                        y += 30
                        canvas.drawLine(40f, y.toFloat(), 550f, y.toFloat(), paint)
                        y += 20
                    }

                    paint.textSize = 12f
                    canvas.drawText("Page $pageCount", 500f, 820f, paint)
                    pdfDoc.finishPage(page)

                    val path = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "BankStatement.pdf")
                    pdfDoc.writeTo(FileOutputStream(path))
                    pdfDoc.close()

                    Toast.makeText(this@ReportsActivity, "PDF saved to: ${path.absolutePath}", Toast.LENGTH_LONG).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ReportsActivity, "Error loading data", Toast.LENGTH_SHORT).show()
                }
            })
    }
}