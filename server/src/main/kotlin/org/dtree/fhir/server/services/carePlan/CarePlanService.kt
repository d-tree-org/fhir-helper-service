package org.dtree.fhir.server.services.carePlan

import org.dtree.fhir.core.uploader.general.FhirClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CarePlanService : KoinComponent {
    private val client by inject<FhirClient>()
    fun getCarePlans() {

    }
}