package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.eclipse.mdht.uml.hl7.vocab.SetOperator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.ImmunizationActivity2;
import org.openhealthtools.mdht.uml.cda.consol.ImmunizationsSection2;
import org.openhealthtools.mdht.uml.cda.consol.MedicationsSection2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Map<String, Resource> convertImmunizationActivity(ImmunizationActivity2 immunizationActivity, Map<String, Resource> headerResources){
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
                Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate((IVL_TS) et);
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
                Immunization.ImmunizationPerformerComponent immunizationPerformerComponent = new Immunization.ImmunizationPerformerComponent();
                if (!practitioners.isEmpty()) {
                    immunizationPerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioners.get(0).getId()));
                } else if (!organizations.isEmpty()) {
                    immunizationPerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organizations.get(0).getId()));
                }

                if (immunizationPerformerComponent.getActor() != null){
                    fhirImmunization.setPerformer(Collections.singletonList(immunizationPerformerComponent));
                    resources.putAll(immunizationPerformers);
                }

            }
        }

        if (immunizationActivity.getReactionObservation() != null){
            Immunization.ImmunizationReactionComponent immunizationReactionComponent = new Immunization.ImmunizationReactionComponent();

            if (immunizationActivity.getReactionObservation().getEffectiveTime() != null){
                Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate(immunizationActivity.getReactionObservation().getEffectiveTime());
                if (recordedDate instanceof DateTimeType) {
                    immunizationReactionComponent.setDate(((DateTimeType) recordedDate).getValue());
                } else if (recordedDate instanceof Period) {
                    immunizationReactionComponent.setDate(((Period) recordedDate).getStartElement().getValue());
                }
            }

            //TODO: add OBservation Creation from Reaction
        }


        return resources;
    }

}
