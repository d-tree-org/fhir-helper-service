package org.dtree.fhir.server.plugins.scheduler

import io.github.cdimascio.dotenv.Dotenv
import org.dtree.fhir.server.plugins.scheduler.jobs.ClearTracingJob
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.util.*

class JobSchedulerManager(dotenv: Dotenv, jobFactory: JobFactory) {
    var scheduler: Scheduler

    init {
        val props = Properties()

        for (entry in dotenv.entries()) {
            if (entry.key.startsWith("org.quartz")) {
                props[entry.key] = entry.value
            }
        }

        val schedulerFactory: SchedulerFactory = StdSchedulerFactory(props)
        scheduler = schedulerFactory.scheduler

        scheduler.setJobFactory(jobFactory)

        scheduleJobs()
    }

    fun startScheduler() {
        scheduler.start()
    }

    private fun scheduleJobs() {
        val jobKey = JobKey("clear-tracing-list", "jobs")
        if (!scheduler.checkExists(jobKey)) {
            val job = JobBuilder.newJob(ClearTracingJob::class.java)
                .withIdentity(jobKey.name, jobKey.group)
                .build()

            val trigger = TriggerBuilder.newTrigger()
                .withIdentity("clear-tracing-list-trigger", "jobs")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 7,12,17 * * ?"))
                .build()

            scheduler.scheduleJob(job, trigger)
        }
    }
}