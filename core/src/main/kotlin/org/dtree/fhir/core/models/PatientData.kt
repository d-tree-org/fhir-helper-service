package org.dtree.fhir.core.models

import org.dtree.fhir.core.utils.TracingHelpers
import org.dtree.fhir.core.utils.isHomeTracingTask
import org.dtree.fhir.core.utils.logicalId
import org.hl7.fhir.r4.model.*

enum class TracingType {
    home, phone, none
}

data class PatientData(
    var patient: Patient = Patient(),
    val guardians: MutableList<RelatedPerson> = mutableListOf(),
    val linkedPatients: MutableList<Patient> = mutableListOf(),
    val observations: MutableList<Observation> = mutableListOf(),
    val practitioners: MutableList<Practitioner> = mutableListOf(),
    val carePlans: MutableList<CarePlan> = mutableListOf(),
    val tasks: MutableList<Task> = mutableListOf(),
    val conditions: MutableList<Condition> = mutableListOf(),
    val appointments: MutableList<Appointment> = mutableListOf(),
    val lists: MutableList<ListResource> = mutableListOf(),
    val tracingTasks: MutableList<Task> = mutableListOf(),
    val currentCarePlan: CarePlan? = null,
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

        val tracingBundle = Bundle()
        tracingBundle.id = TracingHelpers.tracingBundleId

        // TODO: filter tracing ones
        tracingTasks.forEach { tracingBundle.addEntry(Bundle.BundleEntryComponent().setResource(it)) }
        lists.forEach { tracingBundle.addEntry(Bundle.BundleEntryComponent().setResource(it)) }
        appointments.forEach { tracingBundle.addEntry(Bundle.BundleEntryComponent().setResource(it)) }

        resourcesAsBundle.addEntry(
            Bundle.BundleEntryComponent().setResource(tracingBundle).apply {
                id = TracingHelpers.tracingBundleId
            },
        )

        val list = arrayListOf(*practitioners.toTypedArray(), resourcesAsBundle, patient)
        if (currentCarePlan != null) {
            list.add(currentCarePlan)
        }

        return list
    }

    fun getTracingType(): TracingType {
        val task = tracingTasks.firstOrNull() ?: return TracingType.none
        return if (task.isHomeTracingTask()) TracingType.home else TracingType.phone
    }

    fun toLaunchContextMap(): Map<String, Resource>? {
        return null
    }
}

fun Bundle.parsePatientResources(patientId: String): PatientData {
    val patientData = PatientData()
    val tasks = mutableListOf<Task>()
    val tracingTasks = mutableListOf<Task>()

    this.entry?.forEach { entry ->
        // For batch responses, we need to handle the response bundle
        val resources = when {
            entry.resource is Bundle -> (entry.resource as Bundle).entry?.map { it.resource } ?: emptyList()
            else -> listOf(entry.resource)
        }

        resources.forEach { resource ->
            when (resource) {
                is Patient -> {
                    if (resource.logicalId != patientId) {
                        patientData.linkedPatients.add(resource)
                    } else {
                        patientData.patient = resource
                    }
                }

                is RelatedPerson -> patientData.guardians.add(resource)
                is Observation -> patientData.observations.add(resource)
                is Practitioner -> patientData.practitioners.add(resource)
                is CarePlan -> patientData.carePlans.add(resource)
                is Task -> {
                    if (resource.code.codingFirstRep.code == "225368008") {
                        tracingTasks.add(resource)
                    } else {
                        tasks.add(resource)
                    }
                }

                is Condition -> patientData.conditions.add(resource)
                is Appointment -> patientData.appointments.add(resource)
                is ListResource -> patientData.lists.add(resource)
            }
        }
    }

    return patientData.copy(
        tasks = tasks,
        tracingTasks = tracingTasks,
        carePlans = patientData.carePlans.sortedByDescending { it.period.start }.toMutableList()
    )
}