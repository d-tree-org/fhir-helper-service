package org.dtree.fhir.server.controller

import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.dtree.fhir.server.services.form.FormService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class  TasksControllerImpl : TasksController, BaseController(), KoinComponent {
    val formService by inject<FormService>()
    override fun finishVisits(body: List<FinishVisitRequest>) {
        formService.finishVisit(body)
    }
}

interface TasksController {
    fun finishVisits(body: List<FinishVisitRequest>)
}
