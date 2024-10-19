package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.form.FormService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class  TasksControllerImpl : TasksController, BaseController(), KoinComponent {
    val formService by inject<FormService>()
    override fun finishVisits() {
        formService.finishVisit()
    }
}

interface TasksController {
    fun finishVisits()
}
