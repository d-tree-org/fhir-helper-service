package org.dtree.fhir.server.controller

import org.dtree.fhir.server.plugins.tasks.ChangeAppointmentData
import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.dtree.fhir.server.services.form.FormService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class  TasksControllerImpl : TasksController, BaseController(), KoinComponent {
    private val formService by inject<FormService>()

    override fun finishVisits(body: List<FinishVisitRequest>) {
        formService.finishVisit(body)
    }

    override suspend fun changeAppointmentData(body: List<ChangeAppointmentData>) {
        formService.changeAppointmentData(body)
    }

    override suspend fun tracingEnteredInError(body: List<String>) {
        formService.tracingEnteredInError(body)
    }
}

interface TasksController {
    fun finishVisits(body: List<FinishVisitRequest>)
    suspend fun changeAppointmentData(body: List<ChangeAppointmentData>)
    suspend fun tracingEnteredInError(body: List<String>)
}
