package org.dtree.fhir.server.plugins.scheduler

import org.dtree.fhir.server.data.JobHistoryRepository
import org.dtree.fhir.server.plugins.scheduler.jobs.ClearTracingJob
import org.dtree.fhir.server.services.tracing.TracingService
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import kotlin.reflect.jvm.jvmName

class JobFactory(private val tracingService: TracingService, private val historyService: JobHistoryRepository) : JobFactory {
    override fun newJob(bundle: TriggerFiredBundle?, scheduler: Scheduler?): Job {
        if (bundle != null) {
            val jobClass = bundle.jobDetail.jobClass
            if (jobClass.name == ClearTracingJob::class.jvmName) {
                return ClearTracingJob(tracingService, historyService)
            }
        }
        throw NotImplementedError("Job Factory error")
    }
}