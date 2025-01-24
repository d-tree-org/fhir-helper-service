package org.dtree.fhir.server.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object JobHistoryTable : Table("job_history") {
    val id = integer("id").autoIncrement()
    val jobName = varchar("job_name", 100)
    val jobGroup = varchar("job_group", 100)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time").nullable()
    val status = varchar("status", 20)
    val message = text("message").nullable()
    val retryCount = integer("retry_count").default(0)

    override val primaryKey = PrimaryKey(id)
}