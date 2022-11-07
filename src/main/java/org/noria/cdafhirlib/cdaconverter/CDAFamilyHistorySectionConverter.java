package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.RelatedSubject;
import org.eclipse.mdht.uml.cda.SubjectPerson;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.PQ;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.FamilyHistoryObservation2;
import org.openhealthtools.mdht.uml.cda.consol.FamilyHistoryOrganizer2;
import org.openhealthtools.mdht.uml.cda.consol.FamilyHistorySection2;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAFamilyHistorySectionConverter {

    private final static String FAMILY_MEMBER_CODE = "FAMMEMB";
    private final static String FAMILY_MEMBER_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-RoleCode";
    private final CodeMappingProcessor codeMappingProcessor;

    public CDAFamilyHistorySectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertFamilyHistories(FamilyHistorySection2 familyHistorySection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        familyHistorySection.getConsolFamilyHistoryOrganizer2s().forEach(
                organizer -> resources.putAll(this.processFamilyHistoryOrganizer(organizer, headerResources))
        );

        return resources;
    }

    private Map<String, Resource> processFamilyHistoryOrganizer(FamilyHistoryOrganizer2 organizer, Map<String, Resource> headerResources) {
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        Map<String, Resource> resources = new HashMap<>();
        FamilyMemberHistory familyMemberHistory = new FamilyMemberHistory();
        if (CollectionUtils.isNotEmpty(organizer.getIds())) {
            organizer.getIds().forEach(id -> familyMemberHistory.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        Coding fhirStatusCoding = null;
        if (organizer.getStatusCode() != null && !organizer.getStatusCode().isSetNullFlavor()) {
            try {
                fhirStatusCoding = cdaBasicElementsConverter.createFHIRCoding(organizer.getStatusCode(), CDAtoFHIRCodeConversionType.FAMILY_HISTORY_STATUS.toValue());
                familyMemberHistory.setStatus(FamilyMemberHistory.FamilyHistoryStatus.fromCode(fhirStatusCoding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }
        if (fhirStatusCoding == null) {
            familyMemberHistory.setStatus(FamilyMemberHistory.FamilyHistoryStatus.HEALTHUNKNOWN);
        }

        if (organizer.getSubject() != null && organizer.getSubject().getRelatedSubject() != null) {
            RelatedSubject relatedSubject = organizer.getSubject().getRelatedSubject();
            if (!relatedSubject.isSetNullFlavor()) {
                if (relatedSubject.getCode() != null && !relatedSubject.getCode().isSetNullFlavor()) {
                    familyMemberHistory.setRelationship(cdaBasicElementsConverter.createFHIRCodeableConcept(relatedSubject.getCode(), null));
                } else {
                    Coding coding = new Coding();
                    coding.setCode(FAMILY_MEMBER_CODE);
                    coding.setSystem(FAMILY_MEMBER_SYSTEM);
                    CodeableConcept codeableConcept = new CodeableConcept();
                    codeableConcept.getCoding().add(coding);
                    familyMemberHistory.setRelationship(codeableConcept);
                }

                if (relatedSubject.getSubject() != null && !relatedSubject.getSubject().isSetNullFlavor()) {
                    SubjectPerson subjectPerson = relatedSubject.getSubject();
                    if (subjectPerson.getAdministrativeGenderCode() != null && !subjectPerson.getAdministrativeGenderCode().isSetNullFlavor()) {
                        familyMemberHistory.setSex(cdaBasicElementsConverter.createFHIRCodeableConcept(subjectPerson.getAdministrativeGenderCode(), CDAtoFHIRCodeConversionType.FAMILY_HISTORY_MEMBER_PERSON_RLT_SUBJ_GENDER.toValue()));
                    }
                    if (subjectPerson.getBirthTime() != null && !subjectPerson.getBirthTime().isSetNullFlavor()) {
                        familyMemberHistory.setBorn(cdaBasicElementsConverter.convertTSDate(subjectPerson.getBirthTime()));
                    }
                }
            }
        }

        for (FamilyHistoryObservation2 fhObs : organizer.getConsolFamilyHistoryObservation2s()) {
            FamilyMemberHistory.FamilyMemberHistoryConditionComponent familyMemberHistoryConditionComponent = new FamilyMemberHistory.FamilyMemberHistoryConditionComponent();
            if (!fhObs.getValues().isEmpty()) {
                fhObs.getValues().stream()
                        .filter(v -> v != null && !v.isSetNullFlavor() && v instanceof CD)
                        .findAny()
                        .ifPresent(value -> familyMemberHistoryConditionComponent.setCode(cdaBasicElementsConverter.createFHIRCodeableConcept((CD) value, null)));
            }
            if (fhObs.getEffectiveTime() != null && !fhObs.getEffectiveTime().isSetNullFlavor()) {
                familyMemberHistoryConditionComponent.setOnset(cdaBasicElementsConverter.convertTSDate(fhObs.getEffectiveTime()));
            }

            if (familyMemberHistoryConditionComponent.getOnset() != null && fhObs.getAgeObservation() != null && !fhObs.getAgeObservation().getValues().isEmpty()) {
                familyMemberHistoryConditionComponent.setOnset(cdaBasicElementsConverter.createAge((PQ) fhObs.getAgeObservation().getValues().get(0)));
            }

            if (fhObs.getFamilyHistoryDeathObservation() != null && !fhObs.getFamilyHistoryDeathObservation().getValues().isEmpty()) {
                familyMemberHistory.setDeceased(cdaBasicElementsConverter.createAge((PQ) fhObs.getFamilyHistoryDeathObservation().getValues().get(0)));
            }

        }

        familyMemberHistory.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.FAMILYMEMBERHISTORY, familyMemberHistory.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            familyMemberHistory.setPatient(reference);
        }
        resources.put(familyMemberHistory.getId(), familyMemberHistory);

        return resources;
    }

}
