package org.dtree.fhir.server.plugins.scheduler

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import org.dtree.fhir.server.data.JobHistoryRepository
import org.koin.ktor.ext.inject
import org.quartz.impl.matchers.GroupMatcher
import java.time.format.DateTimeFormatter

@OptIn(InternalAPI::class)
fun Route.schedulerRoutes() {
    val schedulerManager by inject<JobSchedulerManager>()
    val jobHistoryRepository by inject<JobHistoryRepository>()

    get("/jobs/active") {
        val activeJobs = schedulerManager.scheduler.jobGroupNames.flatMap { groupName ->
            schedulerManager.scheduler.getJobKeys(GroupMatcher.groupEquals(groupName)).map { jobKey ->
                val jobDetail = schedulerManager.scheduler.getJobDetail(jobKey)
                val triggers = schedulerManager.scheduler.getTriggersOfJob(jobKey)
                mapOf(
                    "jobName" to jobKey.name,
                    "jobGroup" to jobKey.group,
                    "description" to jobDetail.description,
                    "triggerCount" to triggers.size
                )
            }
        }
        call.respond(activeJobs)
    }

    get("/jobs/upcoming") {
        val upcomingJobs = schedulerManager.scheduler.jobGroupNames.flatMap { groupName ->
            schedulerManager.scheduler.getJobKeys(GroupMatcher.groupEquals(groupName)).flatMap { jobKey ->
                schedulerManager.scheduler.getTriggersOfJob(jobKey).map { trigger ->
                    mapOf(
                        "jobName" to jobKey.name,
                        "jobGroup" to jobKey.group,
                        "nextFireTime" to trigger.nextFireTime?.toZonedDateTime()
                            ?.format(DateTimeFormatter.ISO_INSTANT),
                        "previousFireTime" to trigger.previousFireTime?.toZonedDateTime()
                            ?.format(DateTimeFormatter.ISO_INSTANT)
                    )
                }
            }
        }
        call.respond(upcomingJobs)
    }

    get("/jobs/running") {
        val runningJobs = schedulerManager.scheduler.currentlyExecutingJobs.map { executingJob ->
            mapOf(
                "jobName" to executingJob.jobDetail.key.name,
                "jobGroup" to executingJob.jobDetail.key.group,
                "fireTime" to executingJob.fireTime.toString(),
                "scheduledFireTime" to executingJob.scheduledFireTime.toString()
            )
        }
        call.respond(runningJobs)
    }

    get("/jobs/{group}/{name}/history") {
        val groupName = call.parameters["group"] ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Group name is required")
        )

        val jobName = call.parameters["name"] ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Job name is required")
        )

        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

        val history = jobHistoryRepository.getJobHistory(jobName, groupName, limit)
        call.respond(history)
    }
}
