package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.carePlan.CarePlanService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CarePlanControllerImpl() : CarePlanController, BaseController(), KoinComponent {
    private val service by inject<CarePlanService>()
    override fun getCarePlans() {
        return service.getCarePlans()
    }
}

interface CarePlanController {
    fun getCarePlans()
}