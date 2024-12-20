package org.dtree.fhir.server.controller

import org.dtree.fhir.server.plugins.tasks.ChangeAppointmentData
import org.dtree.fhir.server.plugins.tasks.ChangeStatusType
import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.dtree.fhir.server.plugins.tasks.TracingRemovalType
import org.dtree.fhir.server.services.form.FormService
import org.dtree.fhir.server.services.util.UtilService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TasksControllerImpl : TasksController, BaseController(), KoinComponent {
    private val formService by inject<FormService>()
    private val utilService by inject<UtilService>()

    override fun finishVisits(finishVisitRequestList: List<FinishVisitRequest>) {
        formService.finishVisit(finishVisitRequestList)
    }

    override suspend fun changeAppointmentData(changeAppointmentDataList: List<ChangeAppointmentData>) {
        formService.changeAppointmentData(changeAppointmentDataList)
    }

    override suspend fun changeStatus(patients: List<String>, type: ChangeStatusType) {
        formService.changeStatus(patients, type)
    }

    override suspend fun tracingEnteredInError(patients: List<String>, type: TracingRemovalType) {
        formService.tracingEnteredInError(patients, type)
    }

    override suspend fun runUtil() {
        utilService.runCli()
    }
}

interface TasksController {
    fun finishVisits(finishVisitRequestList: List<FinishVisitRequest>)
    suspend fun changeAppointmentData(changeAppointmentDataList: List<ChangeAppointmentData>)
    suspend fun tracingEnteredInError(patients: List<String>, type: TracingRemovalType)
    suspend fun runUtil()
    suspend fun changeStatus(patients: List<String>, type: ChangeStatusType)
}
