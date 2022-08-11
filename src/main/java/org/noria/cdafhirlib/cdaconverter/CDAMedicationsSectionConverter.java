package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.EIVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.PIVL_TS;
import org.eclipse.mdht.uml.hl7.vocab.SetOperator;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.MedicationActivity2;
import org.openhealthtools.mdht.uml.cda.consol.MedicationDispense2;
import org.openhealthtools.mdht.uml.cda.consol.MedicationSupplyOrder2;
import org.openhealthtools.mdht.uml.cda.consol.MedicationsSection2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class CDAMedicationsSectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAMedicationsSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertMedications(MedicationsSection2 medicationsSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        medicationsSection.getConsolMedicationActivity2s().forEach(medicationActivity -> {
            resources.putAll(this.convertMedicationActivity(medicationActivity, headerResources));
        });

        return resources;
    }

    private Map<String, Resource> convertMedicationActivity(MedicationActivity2 medicationActivity, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        MedicationRequest medicationRequest = new MedicationRequest();
        if (CollectionUtils.isNotEmpty(medicationActivity.getIds())) {
            medicationActivity.getIds().forEach(id -> medicationRequest.addIdentifier(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRIdentifier(id)));
        }

        Dosage dosage = new Dosage();
        medicationActivity.getEffectiveTimes().forEach(et -> {

            if (et.isSetOperator() && et.getOperator().equals(SetOperator.A)) {
                if (et instanceof EIVL_TS) {
                    dosage.setTiming(this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertEIVL_TStoFHIRTiming((EIVL_TS) et));
                } else if (et instanceof PIVL_TS) {
                    dosage.setTiming(this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertPIVL_TStoFHIRTiming((PIVL_TS) et));
                }
            } else {
                Type recordedDate = this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertIVLTSDate((IVL_TS) et);
                if (recordedDate instanceof DateTimeType) {
                    medicationRequest.setAuthoredOnElement((DateTimeType) recordedDate);
                } else if (recordedDate instanceof Period) {
                    medicationRequest.setAuthoredOnElement(((Period) recordedDate).getStartElement());
                }
            }
        });

        if (medicationActivity.getRepeatNumber() != null && medicationActivity.getRepeatNumber().getValue() != null) {
            dosage.getTiming().getRepeat().setCount(medicationActivity.getRepeatNumber().getValue().intValue());
        }

        dosage.setRoute(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCodeableConcept(medicationActivity.getRouteCode(), null));
        dosage.setSite(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCodeableConceptFromList(medicationActivity.getApproachSiteCodes(), null));

        Dosage.DosageDoseAndRateComponent dosageDoseAndRateComponent = new Dosage.DosageDoseAndRateComponent();


        if (medicationActivity.getRateQuantity() != null && medicationActivity.getRateQuantity().getValue() != null) {
            dosageDoseAndRateComponent.setRate(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createSimpleQuantity(medicationActivity.getRateQuantity()));
        } else if (medicationActivity.getRateQuantity() != null && medicationActivity.getRateQuantity().getLow() != null && medicationActivity.getRateQuantity().getHigh() != null) {
            dosageDoseAndRateComponent.setRate(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createRange(medicationActivity.getRateQuantity()));
        } else if (medicationActivity.getMaxDoseQuantity() != null) {
            dosageDoseAndRateComponent.setRate(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createRatio(medicationActivity.getMaxDoseQuantity()));
        }

        if (medicationActivity.getDoseQuantity() != null && medicationActivity.getDoseQuantity().getValue() != null) {
            dosageDoseAndRateComponent.setDose(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createSimpleQuantity(medicationActivity.getDoseQuantity()));
        } else if (medicationActivity.getDoseQuantity() != null) {
            dosageDoseAndRateComponent.setDose(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createRange(medicationActivity.getDoseQuantity()));
        }

        dosage.getDoseAndRate().add(dosageDoseAndRateComponent);
        medicationRequest.getDosageInstruction().add(dosage);

        if (medicationActivity.getConsumable() != null && medicationActivity.getConsumable().getManufacturedProduct() != null && medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationRequest.setMedication(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRCodeableConcept(medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial().getCode(), null));

        }

        if (!medicationActivity.getPerformers().isEmpty()) {
            Map<String, Resource> medicationRequestPerformers = new HashMap<>();
            medicationActivity.getPerformers().forEach(performer -> medicationRequestPerformers.putAll(this.basicCDAElementsConverter.convertPerformer(performer, headerResources)));
            if (!medicationRequestPerformers.isEmpty()) {
                List<Resource> practitioners =  medicationRequestPerformers.values().stream().filter(r->r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations =  medicationRequestPerformers.values().stream().filter(r->r instanceof Organization).collect(Collectors.toList());
                if (!practitioners.isEmpty()) {
                    medicationRequest.setRequester(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioners.get(0).getId()));
                }
                else if (!organizations.isEmpty()){
                    medicationRequest.setRequester(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organizations.get(0).getId()));
                }

                resources.putAll(medicationRequestPerformers);
            }
        }

        if (!medicationActivity.getAuthors().isEmpty()) {
            Map<String, Resource> medicationRequestAuthors = this.basicCDAElementsConverter.convertSectionAuthors(medicationActivity.getAuthors(), headerResources);
            if (!medicationRequestAuthors.isEmpty()) {
                medicationRequest.setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, medicationRequestAuthors.keySet().stream().findFirst().orElse(null)));
                resources.putAll(medicationRequestAuthors);
            }
        }

        if (medicationActivity.getConsolMedicationDispense2s().size() != 0) {
            medicationActivity.getConsolMedicationDispense2s().forEach(md -> resources.putAll(this.convertMedicationDispense(md)));
        }

        if (medicationActivity.getConsolMedicationSupplyOrder2() != null) {
            resources.putAll(this.convertMedicationSupply(medicationActivity.getConsolMedicationSupplyOrder2()));
        }

        medicationRequest.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.MEDICATIONREQUEST, medicationRequest.getIdentifier()));
        resources.put(medicationRequest.getId(), medicationRequest);
        return resources;
    }

    private Map<String, Resource> convertMedicationDispense(MedicationDispense2 medicationDispense) {
        Map<String, Resource> resources = new HashMap<>();
        return resources;
    }

    private Map<String, Resource> convertMedicationSupply(MedicationSupplyOrder2 supplyOrder) {
        Map<String, Resource> resources = new HashMap<>();

        return resources;
    }

}
