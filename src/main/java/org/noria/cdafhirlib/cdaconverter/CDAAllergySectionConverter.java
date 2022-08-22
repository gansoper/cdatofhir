package org.noria.cdafhirlib.cdaconverter;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.CE;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.AllergiesSection2;
import org.openhealthtools.mdht.uml.cda.consol.AllergyObservation2;
import org.openhealthtools.mdht.uml.cda.consol.ReactionObservation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CDAAllergySectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAAllergySectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertAllergies(AllergiesSection2 allergiesSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        allergiesSection.getConsolAllergyConcernAct2s().forEach(act -> {
            act.getAuthors().forEach(author -> resources.putAll(this.basicCDAElementsConverter.convertSectionAuthor(author, headerResources)));
            act.getConsolAllergyObservation2s().forEach(allergyObservation -> resources.putAll(this.convertCDAAllergyObservation(allergyObservation, act.getEffectiveTime(), headerResources)));
        });

        return resources;
    }

    private Map<String, Resource> convertCDAAllergyObservation(AllergyObservation2 allergyObservation, IVL_TS recordedTime, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        AllergyIntolerance allergy = new AllergyIntolerance();
        Type recordedDate = this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertIVLTSDate(recordedTime);
        if (recordedDate instanceof DateTimeType) {
            allergy.setRecordedDateElement((DateTimeType) recordedDate);
        } else if (recordedDate instanceof Period) {
            allergy.setRecordedDateElement(((Period) recordedDate).getStartElement());
        }

        if (CollectionUtils.isNotEmpty(allergyObservation.getIds())) {
            allergyObservation.getIds().forEach(id -> allergy.addIdentifier(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRIdentifier(id)));
        }

        allergy.setOnset(this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertIVLTSDate(allergyObservation.getEffectiveTime()));
        Map<String, Resource> allergyAuthors = this.basicCDAElementsConverter.convertSectionAuthors(allergyObservation.getAuthors(), headerResources);
        if (!allergyAuthors.isEmpty()) {
            allergy.setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, allergyAuthors.keySet().stream().findFirst().orElse(null)));
            resources.putAll(allergyAuthors);
        }

        allergy.getCode().setCoding(allergyObservation.getParticipants().stream()
                .filter(p -> p.getParticipantRole() != null && p.getParticipantRole().getPlayingEntity() != null)
                .map(p -> basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCoding(p.getParticipantRole().getPlayingEntity().getCode(), null))
                .collect(Collectors.toList()));

        if (allergyObservation.getAllergyStatusObservation() != null && CollectionUtils.isNotEmpty(allergyObservation.getAllergyStatusObservation().getValues())) {
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.setCoding(allergyObservation.getAllergyStatusObservation().getValues().stream()
                    .map(v -> this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCoding((CE) v, CDAtoFHIRCodeConversionType.ALLERGY_CLINICAL_STATUS.toValue()))
                    .collect(Collectors.toList()));
            allergy.setClinicalStatus(codeableConcept);

        }
        allergy.getVerificationStatus().setCoding(Collections.singletonList(basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCoding(allergyObservation.getStatusCode(), CDAtoFHIRCodeConversionType.ALLERGY_VERIFICATION_STATUS.toValue())));
        allergy.setReaction(allergyObservation.getReactionObservations().stream().map(this::convertAllergyReaction).collect(Collectors.toList()));

        if (allergyObservation.getCriticalityObservation() != null && CollectionUtils.isNotEmpty(allergyObservation.getCriticalityObservation().getValues())) {
            Coding coding = this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCoding((CD) allergyObservation.getCriticalityObservation().getValues().get(0), CDAtoFHIRCodeConversionType.ALLERGY_CRITICALITY.toValue());
            allergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.fromCode(coding.getCode()));
        }

        allergy.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ALLERGYINTOLERANCE, allergy.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPateintReference(headerResources);
        if (reference != null) {
            allergy.setPatient(reference);
        }
        resources.put(allergy.getId(), allergy);
        return resources;
    }


    private AllergyIntolerance.AllergyIntoleranceReactionComponent convertAllergyReaction(ReactionObservation reactionObservation) {
        if (CollectionUtils.isNotEmpty(reactionObservation.getValues())) {
            AllergyIntolerance.AllergyIntoleranceReactionComponent allergyIntoleranceReactionComponent = new AllergyIntolerance.AllergyIntoleranceReactionComponent();
            List<Coding> reactionCodes = reactionObservation.getValues().stream().map(value -> this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCoding((CD) value, null)).collect(Collectors.toList());
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.setCoding(reactionCodes);
            allergyIntoleranceReactionComponent.setManifestation(Collections.singletonList(codeableConcept));
            if (reactionObservation.getSeverityObservation() != null && CollectionUtils.isNotEmpty(reactionObservation.getSeverityObservation().getValues())) {
                Coding coding = this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCoding((CD) reactionObservation.getSeverityObservation().getValues().get(0), CDAtoFHIRCodeConversionType.ALLERGY_SEVERITY.toValue());
                allergyIntoleranceReactionComponent.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.fromCode(coding.getCode()));
            }
            return allergyIntoleranceReactionComponent;
        }

        return null;
    }

}
