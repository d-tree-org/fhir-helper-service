package org.dtree.fhir.server.plugins

import io.ktor.server.application.*
import org.dtree.fhir.server.plugins.scheduler.JobSchedulerManager
import org.koin.ktor.ext.inject

fun Application.configureScheduler() {
    val jobSchedulerManager by inject<JobSchedulerManager>()

    jobSchedulerManager.startScheduler()
}