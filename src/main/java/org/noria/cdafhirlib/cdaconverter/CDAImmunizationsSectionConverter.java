package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.ImmunizationActivity2;
import org.openhealthtools.mdht.uml.cda.consol.ImmunizationsSection2;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class CDAImmunizationsSectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAImmunizationsSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertImmunizations(ImmunizationsSection2 immunizationsSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        immunizationsSection.getConsolImmunizationActivity2s().forEach(immunizationActivity -> resources.putAll(this.convertImmunizationActivity(immunizationActivity, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertImmunizationActivity(ImmunizationActivity2 immunizationActivity, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        Immunization fhirImmunization = new Immunization();
        if (CollectionUtils.isNotEmpty(immunizationActivity.getIds())) {
            immunizationActivity.getIds().forEach(id -> fhirImmunization.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        Coding coding = basicCDAElementsConverter.createFHIRCoding(immunizationActivity.getStatusCode(), CDAtoFHIRCodeConversionType.IMMUNIZATION_STATUS.toValue());
        if (coding != null) {
            try {
                fhirImmunization.setStatus(Immunization.ImmunizationStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        immunizationActivity.getEffectiveTimes().forEach(et -> {
            Type recordedDate = this.basicCDAElementsConverter.convertTSDate(et);
            fhirImmunization.setOccurrence(recordedDate);
        });

        fhirImmunization.setRoute(this.basicCDAElementsConverter.createFHIRCodeableConcept(immunizationActivity.getRouteCode(), null));
        fhirImmunization.setSite(this.basicCDAElementsConverter.createFHIRCodeableConceptFromList(immunizationActivity.getApproachSiteCodes(), null));

        if (immunizationActivity.getDoseQuantity() != null && immunizationActivity.getDoseQuantity().getValue() != null) {
            fhirImmunization.setDoseQuantity(this.basicCDAElementsConverter.createSimpleQuantity(immunizationActivity.getDoseQuantity()));
        }

        if (immunizationActivity.getConsumable() != null && immunizationActivity.getConsumable().getManufacturedProduct() != null && immunizationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
            fhirImmunization.setVaccineCode(this.basicCDAElementsConverter.createFHIRCodeableConcept(immunizationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial().getCode(), null));
        }

        if (!immunizationActivity.getPerformers().isEmpty()) {
            Map<String, Resource> immunizationPerformers = new HashMap<>();
            immunizationActivity.getPerformers().forEach(performer -> immunizationPerformers.putAll(this.basicCDAElementsConverter.convertPerformer(performer, headerResources)));
            if (!immunizationPerformers.isEmpty()) {
                List<Resource> practitioners = immunizationPerformers.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations = immunizationPerformers.values().stream().filter(r -> r instanceof Organization).collect(Collectors.toList());

                List< Immunization.ImmunizationPerformerComponent> immunizationPerformerComponents = new ArrayList<>();
                for(Resource practitioner: practitioners){
                    Immunization.ImmunizationPerformerComponent immunizationPerformerComponent = new Immunization.ImmunizationPerformerComponent();
                    immunizationPerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));
                    immunizationPerformerComponents.add(immunizationPerformerComponent);
                }

                for(Resource organization: organizations){
                    Immunization.ImmunizationPerformerComponent immunizationPerformerComponent = new Immunization.ImmunizationPerformerComponent();
                    immunizationPerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getId()));
                    immunizationPerformerComponents.add(immunizationPerformerComponent);
                }

                if (!immunizationPerformerComponents.isEmpty()){
                    fhirImmunization.setPerformer(immunizationPerformerComponents);
                    resources.putAll(immunizationPerformers);
                }
            }
        }

        if (immunizationActivity.getReactionObservation() != null) {
            Map<String, Resource> observationResources = this.basicCDAElementsConverter.createFHIRObservation(immunizationActivity.getReactionObservation(), ObservationCategory.EXAM, resources, headerResources);
            Resource observationResource = observationResources.values().stream().filter(r-> r instanceof Observation).findFirst().orElse(null);
            if (observationResource != null) {
                Observation observation = (Observation)observationResource;
                Immunization.ImmunizationReactionComponent immunizationReactionComponent = new Immunization.ImmunizationReactionComponent();
                if (observation.getEffective() != null) {
                    Type effective = observation.getEffective();
                    if (effective instanceof DateTimeType) {
                        immunizationReactionComponent.setDate(((DateTimeType) effective).getValue());
                    } else if (effective instanceof Period) {
                        immunizationReactionComponent.setDate(((Period) effective).getStartElement().getValue());
                    }
                }

                immunizationReactionComponent.setDetail(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.OBSERVATION, observation.getId()));
                fhirImmunization.setReaction(Collections.singletonList(immunizationReactionComponent));
                resources.putAll(observationResources);
            }
        }

        fhirImmunization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.IMMUNIZATION, fhirImmunization.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            fhirImmunization.setPatient(reference);
        }
        resources.put(fhirImmunization.getId(), fhirImmunization);
        return resources;
    }

/*
    private Observation createReactionObservation(ReactionObservation reactionObservation) {
        Observation observation = new Observation();
        if (CollectionUtils.isNotEmpty(reactionObservation.getIds())) {
            reactionObservation.getIds().forEach(id -> observation.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        if (reactionObservation.getEffectiveTime() != null) {
            Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate(reactionObservation.getEffectiveTime());
            observation.setEffective(recordedDate);
        }

        Coding coding = basicCDAElementsConverter.createFHIRCoding(reactionObservation.getStatusCode(), CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
        if (coding != null) {
            try {
                observation.setStatus(Observation.ObservationStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (!reactionObservation.getValues().isEmpty() && reactionObservation.getValues().get(0) instanceof CD) {
            observation.setValue(basicCDAElementsConverter.createFHIRCodeableConcept((CD) reactionObservation.getValues().get(0), null));
        }

        observation.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.OBSERVATION, observation.getIdentifier()));

        return observation;
    }
*/
}
