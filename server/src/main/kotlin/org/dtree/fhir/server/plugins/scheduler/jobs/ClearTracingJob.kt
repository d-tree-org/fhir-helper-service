package org.dtree.fhir.server.plugins.scheduler.jobs

import kotlinx.coroutines.runBlocking
import org.dtree.fhir.server.data.JobHistoryRepository
import org.dtree.fhir.server.services.tracing.TracingService
import org.quartz.*
import org.slf4j.LoggerFactory

@PersistJobDataAfterExecution
class ClearTracingJob(private val tracingService: TracingService, private val historyService: JobHistoryRepository) : Job {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun execute(context: JobExecutionContext) {
        val jobKey = context.jobDetail.key
        val data = context.jobDetail.jobDataMap
        val retryCount = data.getIntValue("retryCount")
        val historyId = data.getIntOrNull("historyId")
        try {
            runBlocking {
                if (historyId == null) {
                    // First attempt
                    val newHistoryId = historyService.logJobStart(jobKey.name, jobKey.group)
                    data.put("historyId", newHistoryId)
                }
                tracingService.cleanFutureDateMissedAppointmentAll()
                data.put("retryCount", 0)
                historyService.logJobEnd(data.getInt("historyId"), "SUCCESS")
            }
        } catch (e: Exception) {
            logger.error("Job failed", e)

            if (retryCount < 3) {
                // Schedule retry
                data.put("retryCount", retryCount + 1)
                historyService.updateRetryCount(data.getInt("historyId"), retryCount + 1)

                val trigger = TriggerBuilder.newTrigger()
                    .startAt(DateBuilder.futureDate(5, DateBuilder.IntervalUnit.MINUTE))
                    .build()

                context.scheduler.scheduleJob(context.jobDetail, trigger)

            } else {
                // Max retries reached
                data.put("retryCount", 0)
                historyService.logJobEnd(
                    data.getInt("historyId"),
                    "FAILED",
                    "Max retries reached. Error: ${e.message}"
                )
            }

            throw e
        }
    }
}

private fun JobDataMap.getIntOrNull(key: String): Int? =
    if (containsKey(key)) getInt(key) else null