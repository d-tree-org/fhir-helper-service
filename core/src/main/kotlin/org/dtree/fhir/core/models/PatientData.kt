package org.dtree.fhir.core.models

import org.dtree.fhir.core.utils.TracingHelpers
import org.hl7.fhir.r4.model.*

data class PatientData(
    var patient: Patient = Patient(),
    val guardians: MutableList<RelatedPerson> = mutableListOf(),
    val observations: MutableList<Observation> = mutableListOf(),
    val practitioners: MutableList<Practitioner> = mutableListOf(),
    val carePlans: MutableList<CarePlan> = mutableListOf(),
    val tasks: MutableList<Task> = mutableListOf(),
    val conditions: MutableList<Condition> = mutableListOf(),
    val appointments: MutableList<Appointment> = mutableListOf(),
    val lists: MutableList<ListResource> = mutableListOf()
) {
    fun isEmpty(): Boolean {
        return !patient.hasId() &&
                guardians.isEmpty() &&
                observations.isEmpty() &&
                practitioners.isEmpty() &&
                carePlans.isEmpty() &&
                tasks.isEmpty() &&
                conditions.isEmpty() &&
                appointments.isEmpty() &&
                lists.isEmpty()
    }

    fun toPopulationResource(): List<Resource> {
        val currentCarePlan = carePlans.firstOrNull()
        val resources = conditions + guardians + observations
        val resourcesAsBundle = Bundle().apply { resources.map { this.addEntry().resource = it } }
        val bundle = Bundle()
        bundle.id = TracingHelpers.tracingBundleId

        // TODO: filter tracing ones
        tasks.forEach { bundle.addEntry(Bundle.BundleEntryComponent().setResource(it)) }
        lists.forEach { bundle.addEntry(Bundle.BundleEntryComponent().setResource(it)) }
        appointments.forEach { bundle.addEntry(Bundle.BundleEntryComponent().setResource(it)) }

        resourcesAsBundle.addEntry().resource = bundle

        val list = arrayListOf(*practitioners.toTypedArray(), resourcesAsBundle, patient)
        if (currentCarePlan != null) {
            list.add(currentCarePlan)
        }

        return list
    }
}

fun Bundle.parsePatientResources(): PatientData {
    val patientData = PatientData()

    this.entry?.forEach { entry ->
        // For batch responses, we need to handle the response bundle
        val resources = when {
            entry.resource is Bundle -> (entry.resource as Bundle).entry?.map { it.resource } ?: emptyList()
            else -> listOf(entry.resource)
        }

        resources.forEach { resource ->
            when (resource) {
                is Patient -> patientData.patient = resource
                is RelatedPerson -> patientData.guardians.add(resource)
                is Observation -> patientData.observations.add(resource)
                is Practitioner -> patientData.practitioners.add(resource)
                is CarePlan -> patientData.carePlans.add(resource)
                is Task -> patientData.tasks.add(resource)
                is Condition -> patientData.conditions.add(resource)
                is Appointment -> patientData.appointments.add(resource)
                is ListResource -> patientData.lists.add(resource)
            }
        }
    }

    return patientData
}