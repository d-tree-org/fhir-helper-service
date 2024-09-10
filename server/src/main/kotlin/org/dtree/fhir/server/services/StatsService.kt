package org.dtree.fhir.server.services

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.core.search.filters.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

object StatsService : KoinComponent {
    private val client by inject<FhirClient>()

    suspend fun getFacilityStats(id: String): FacilityResultData {
        val locationFilter = filterByLocation(id)
        val dateFilter = filterByDateCreated(LocalDate.now())
        val baseFiltersWithoutDates = listOf(locationFilter)
        val baseFilters = listOf(locationFilter, dateFilter)

        val newlyDiagnosed =
            patientTypeFilter(
                patients = listOf(PatientType.NEWLY_DIAGNOSED_CLIENT),
                baseFilters = baseFiltersWithoutDates
            ).copy(
                groupId = "totals",
                title = "Newly diagnosed clients"
            )
        val alreadyOnArt =
            patientTypeFilter(
                patients = listOf(PatientType.CLIENT_ALREADY_ON_ART),
                baseFilters = baseFiltersWithoutDates
            ).copy(
                groupId = "totals",
                title = "Already on Art"
            )
        val exposedInfants =
            patientTypeFilter(
                patients = listOf(PatientType.EXPOSED_INFANT),
                baseFilters = baseFiltersWithoutDates
            ).copy(
                groupId = "totals",
                title = "Exposed infant"
            )

        val newNewlyDiagnosed =
            patientTypeFilter(patients = listOf(PatientType.NEWLY_DIAGNOSED_CLIENT), baseFilters = baseFilters).copy(
                groupId = "newPatients",
                title = "Newly diagnosed clients"
            )
        val newAlreadyOnArt =
            patientTypeFilter(patients = listOf(PatientType.CLIENT_ALREADY_ON_ART), baseFilters = baseFilters).copy(
                groupId = "newPatients",
                title = "Already on Art"
            )
        val newExposedInfants =
            patientTypeFilter(patients = listOf(PatientType.EXPOSED_INFANT), baseFilters = baseFilters).copy(
                groupId = "newPatients",
                title = "Exposed infant"
            )

        val allVisits = questionnaireResponseFilters(
            questionnaire = "patient-finish-visit",
            baseFilters = baseFilters,
            hasCount = false,
            customParser = { uniquePatientQuestionnaireParser(it) }
        ).copy(
            groupId = "visits",
            title = "All finish visits"
        )
        val allVisitsExposed = questionnaireResponseFilters(
            questionnaire = "patient-finish-visit",
            baseFilters = (baseFilters + listOf(
                addPatientFilter(
                    listOf(PatientType.EXPOSED_INFANT),
                    inSubject = true
                )
            )),
            hasCount = false,
            customParser = { uniquePatientQuestionnaireParser(it) }
        ).copy(
            groupId = "visits",
            title = "Exposed Infants visits",
            filterId = "patient-finish-visit-exposed-infant",
        )
        val allVisitsArt = questionnaireResponseFilters(
            questionnaire = "patient-finish-visit",
            baseFilters = (baseFilters + listOf(
                addPatientFilter(
                    listOf(PatientType.CLIENT_ALREADY_ON_ART),
                    inSubject = true
                )
            )),
            hasCount = false,
            customParser = { uniquePatientQuestionnaireParser(it) }
        ).copy(
            groupId = "visits",
            title = "Client Already on Art visits",
            filterId = "patient-finish-visit-CLIENT_ALREADY_ON_ART",
        )

        val allVisitsNewly = questionnaireResponseFilters(
            questionnaire = "patient-finish-visit",
            baseFilters = (baseFilters + listOf(
                addPatientFilter(
                    listOf(PatientType.NEWLY_DIAGNOSED_CLIENT),
                    inSubject = true
                )
            )),
            hasCount = false,
            customParser = { uniquePatientQuestionnaireParser(it) }
        ).copy(
            groupId = "visits",
            title = "Newly Diagnosed visits",
            filterId = "patient-finish-visit-NEWLY_DIAGNOSED_CLIENT"
        )

        val milestone =
            questionnaireResponseFilters(
                questionnaire = "exposed-infant-milestone-hiv-test",
                baseFilters = baseFilters,
                hasCount = false,
                customParser = {
                    questionnaireParser(it, "able-to-conduct-test")
                },
            ).copy(
                groupId = "tasks",
                title = "Milestone conducted"
            )
        val viralLoad =
            questionnaireResponseFilters(
                questionnaire = "art-client-viral-load-collection",
                baseFilters = baseFilters,
                hasCount = false,
                customParser = {
                    questionnaireParser(it, "viral-load-collection-confirmation")
                },
            ).copy(
                groupId = "tasks",
                title = "Viral Load Collected"
            )

        return fetchDataTest(
            client, listOf(
                newlyDiagnosed, alreadyOnArt, exposedInfants,
                newExposedInfants, newNewlyDiagnosed, newAlreadyOnArt,
                allVisits, allVisitsExposed, allVisitsNewly, allVisitsArt,
                milestone, viralLoad
            )
        )
    }
}