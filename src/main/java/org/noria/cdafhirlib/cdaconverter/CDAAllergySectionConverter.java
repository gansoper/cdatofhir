package org.noria.cdafhirlib.cdaconverter;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.CE;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.AllergiesSection2;
import org.openhealthtools.mdht.uml.cda.consol.AllergyObservation;
import org.openhealthtools.mdht.uml.cda.consol.ReactionObservation;

import java.util.*;
import java.util.stream.Collectors;

public class CDAAllergySectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAAllergySectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, IBaseResource> convertAllergies(AllergiesSection2 allergiesSection, Map<String, IBaseResource> headerResources) {
        Map<String, IBaseResource> resources = new HashMap<>();
        List<AllergyObservation> allergyObservationList = new ArrayList<>();
        allergiesSection.getAllergyProblemActs().forEach(act -> {
            allergyObservationList.addAll(act.getAllergyObservations());
            act.getAuthors().forEach(author -> resources.putAll(this.basicCDAElementsConverter.convertAuthor(author)));
            act.getAllergyObservations().forEach(allergyObservation -> resources.putAll(this.convertCDAAllergyObservation(allergyObservation, act.getEffectiveTime(), headerResources)));
        });

        return resources;
    }

    private Map<String, IBaseResource> convertCDAAllergyObservation(AllergyObservation allergyObservation, IVL_TS recordedTime, Map<String, IBaseResource> headerResources) {
        Map<String, IBaseResource> resources = new HashMap<>();
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
        Map<String, IBaseResource> allergyAuthors = new HashMap<>();
        allergyObservation.getAuthors().forEach(author -> allergyAuthors.putAll(this.convertAllergyAuthor(author, headerResources)));
        if (!allergyAuthors.isEmpty()) {
            allergy.setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, allergyAuthors.keySet().stream().findFirst().orElse(null)));
            resources.putAll(allergyAuthors);
        }

        allergy.getCode().setCoding(allergyObservation.getParticipants().stream()
                .map(p -> basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCoding(p.getParticipantRole().getCode(), null))
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

        //TODO: Add Criticality Processing

        return resources;
    }

    private Map<String, IBaseResource> convertAllergyAuthor(Author cdaAuthor, Map<String, IBaseResource> headerResources) {
        if (cdaAuthor.getAssignedAuthor() != null && !cdaAuthor.getAssignedAuthor().isSetNullFlavor() && CollectionUtils.isNotEmpty(cdaAuthor.getAssignedAuthor().getIds())) {
            List<Identifier> identifiers = cdaAuthor.getAssignedAuthor().getIds().stream().map(this.basicCDAElementsConverter.getSimpleCDATypesConverter()::createFHIRIdentifier).collect(Collectors.toList());
            Practitioner existingPractitoner = ConvertedElementsHelper.findPractitionerByAuthor(identifiers, headerResources);
            if (existingPractitoner != null) {
                Map<String, IBaseResource> resources = new HashMap<>();
                resources.put(existingPractitoner.getId(), existingPractitoner);
                return resources;
            }
        }

        return this.basicCDAElementsConverter.convertAuthor(cdaAuthor);

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
