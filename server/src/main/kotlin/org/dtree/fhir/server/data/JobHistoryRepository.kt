package org.dtree.fhir.server.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class JobHistoryRepository(database: Database) {

    init {
        transaction(database) {
            SchemaUtils.create(JobHistoryTable)
        }
    }

    fun logJobStart(jobName: String, jobGroup: String): Int = transaction {
        JobHistoryTable.insert {
            it[this.jobName] = jobName
            it[this.jobGroup] = jobGroup
            it[startTime] = LocalDateTime.now()
            it[status] = "RUNNING"
        } get JobHistoryTable.id
    }

    fun logJobEnd(historyId: Int, status: String, message: String? = null) = transaction {
        JobHistoryTable.update({ JobHistoryTable.id eq historyId }) {
            it[endTime] = LocalDateTime.now()
            it[this.status] = status
            it[this.message] = message
        }
    }

    fun updateRetryCount(historyId: Int, retryCount: Int) = transaction {
        JobHistoryTable.update({ JobHistoryTable.id eq historyId }) {
            it[this.retryCount] = retryCount
        }
    }

    fun getJobHistory(jobName: String, jobGroup: String, limit: Int = 100) = transaction {
        JobHistoryTable.selectAll()
            .where { (JobHistoryTable.jobName eq jobName) and (JobHistoryTable.jobGroup eq jobGroup) }
            .orderBy(JobHistoryTable.startTime to SortOrder.DESC)
            .limit(limit)
            .map {
                mapOf(
                    "id" to it[JobHistoryTable.id],
                    "startTime" to it[JobHistoryTable.startTime],
                    "endTime" to it[JobHistoryTable.endTime],
                    "status" to it[JobHistoryTable.status],
                    "message" to it[JobHistoryTable.message],
                    "retryCount" to it[JobHistoryTable.retryCount]
                )
            }
    }
}