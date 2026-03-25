package com.emreyildirim.matchhuntv1.data.repository

import com.emreyildirim.matchhuntv1.data.model.Report
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("reports")

    suspend fun createReport(report: Report) {
        val id = if (report.id.isBlank()) UUID.randomUUID().toString() else report.id
        val reportWithId = report.copy(id = id)
        reportsCollection.document(id).set(reportWithId).await()
    }
}

