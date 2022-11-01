package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.eclipse.mdht.uml.hl7.vocab.SetOperator;
import org.eclipse.mdht.uml.hl7.vocab.x_DocumentSubstanceMood;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
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

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAMedicationsSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertMedications(MedicationsSection2 medicationsSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        medicationsSection.getConsolMedicationActivity2s().forEach(medicationActivity -> resources.putAll(this.convertMedicationActivity(medicationActivity, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertMedicationActivity(MedicationActivity2 medicationActivity, Map<String, Resource> headerResources) {

        Map<String, Resource> resources = new HashMap<>();
        CDACommonElementsConverter commonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        if (medicationActivity.getMoodCode().equals(x_DocumentSubstanceMood.INT)) {
            resources.putAll(this.convertToMedicationRequest(medicationActivity, headerResources));
        } else if (medicationActivity.getMoodCode().equals(x_DocumentSubstanceMood.EVN)) {
            resources.putAll(commonElementsConverter.convertToMedicationStatement(medicationActivity, headerResources));
        }

        if (medicationActivity.getConsolMedicationDispense2s().size() != 0) {
            medicationActivity.getConsolMedicationDispense2s().forEach(md -> resources.putAll(this.convertMedicationDispense(md, headerResources)));
        }

        if (medicationActivity.getConsolMedicationSupplyOrder2() != null) {
            resources.putAll(this.convertMedicationSupply(medicationActivity.getConsolMedicationSupplyOrder2(), headerResources));
        }


        return resources;
    }

    private Map<String, Resource> convertMedicationDispense(MedicationDispense2 medicationDispenseCDA, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        MedicationDispense medicationDispense = new MedicationDispense();
        if (CollectionUtils.isNotEmpty(medicationDispenseCDA.getIds())) {
            medicationDispenseCDA.getIds().forEach(id -> medicationDispense.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        if ((medicationDispenseCDA.getQuantity() != null && medicationDispenseCDA.getQuantity().getValue() != null) || medicationDispenseCDA.getRepeatNumber() != null) {
            Dosage dosage = cdaBasicElementsConverter.convertQuantityToDosageRate(medicationDispenseCDA.getQuantity(), medicationDispenseCDA.getRepeatNumber());
            if (dosage != null) {
                medicationDispense.getDosageInstruction().add(dosage);
            }

            SimpleQuantity simpleQuantity = cdaBasicElementsConverter.createSimpleQuantity(medicationDispenseCDA.getQuantity());
            if (simpleQuantity != null) {
                medicationDispense.setQuantity(simpleQuantity);
            }
        }

        medicationDispenseCDA.getEffectiveTimes().forEach(et -> {
            DateTimeType recordedDate = cdaBasicElementsConverter.convertTSDate(et);
            if (recordedDate != null) {
                medicationDispense.setWhenPreparedElement(recordedDate);
            }

        });

        if (medicationDispenseCDA.getProduct() != null && medicationDispenseCDA.getProduct().getManufacturedProduct() != null && medicationDispenseCDA.getProduct().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationDispense.setMedication(cdaBasicElementsConverter.createFHIRCodeableConcept(medicationDispenseCDA.getProduct().getManufacturedProduct().getManufacturedMaterial().getCode(), null));
        }


        if (!medicationDispenseCDA.getPerformers().isEmpty()) {
            Map<String, Resource> medicationRequestPerformers = new HashMap<>();
            medicationDispenseCDA.getPerformers().forEach(performer -> medicationRequestPerformers.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertPerformer(performer, headerResources)));
            if (!medicationRequestPerformers.isEmpty()) {
                List<Resource> practitioners = medicationRequestPerformers.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations = medicationRequestPerformers.values().stream().filter(r -> r instanceof Organization).collect(Collectors.toList());
                MedicationDispense.MedicationDispensePerformerComponent medicationDispensePerformerComponent = new MedicationDispense.MedicationDispensePerformerComponent();
                if (!practitioners.isEmpty()) {
                    medicationDispensePerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioners.get(0).getId()));
                    medicationDispense.getPerformer().add(medicationDispensePerformerComponent);
                } else if (!organizations.isEmpty()) {
                    medicationDispensePerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organizations.get(0).getId()));
                    medicationDispense.getPerformer().add(medicationDispensePerformerComponent);
                }

                resources.putAll(medicationRequestPerformers);
            }
        }

        Coding coding = cdaBasicElementsConverter.createFHIRCoding(medicationDispenseCDA.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_DISPENSE_STATUS.toValue());
        if (coding != null) {
            try {
                medicationDispense.setStatus(MedicationDispense.MedicationDispenseStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (medicationDispenseCDA.getConsolMedicationSupplyOrder2() != null) {
            resources.putAll(this.convertMedicationSupply(medicationDispenseCDA.getConsolMedicationSupplyOrder2(), headerResources));
        }
        medicationDispense.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.MEDICATIONDISPENSE, medicationDispense.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            medicationDispense.setSubject(reference);
        }
        resources.put(medicationDispense.getId(), medicationDispense);
        return resources;
    }

    private Map<String, Resource> convertMedicationSupply(MedicationSupplyOrder2 supplyOrder, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        MedicationRequest medicationRequest = new MedicationRequest();
        if (CollectionUtils.isNotEmpty(supplyOrder.getIds())) {
            supplyOrder.getIds().forEach(id -> medicationRequest.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        supplyOrder.getEffectiveTimes().forEach(et -> {
            Type recordedDate = cdaBasicElementsConverter.convertIVLTSDate((IVL_TS) et);
            if (recordedDate instanceof DateTimeType) {
                medicationRequest.setAuthoredOnElement((DateTimeType) recordedDate);
            } else if (recordedDate instanceof Period) {
                medicationRequest.setAuthoredOnElement(((Period) recordedDate).getStartElement());
            }
        });

        if (!supplyOrder.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthors(medicationRequest, supplyOrder.getAuthors(), headerResources));
        }

        if (supplyOrder.getProduct() != null && supplyOrder.getProduct().getManufacturedProduct() != null && supplyOrder.getProduct().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationRequest.setMedication(cdaBasicElementsConverter.createFHIRCodeableConcept(supplyOrder.getProduct().getManufacturedProduct().getManufacturedMaterial().getCode(), null));
        }


        if ((supplyOrder.getQuantity() != null && supplyOrder.getQuantity().getValue() != null) || supplyOrder.getRepeatNumber() != null) {
            Dosage dosage = cdaBasicElementsConverter.convertQuantityToDosageRate(supplyOrder.getQuantity(), supplyOrder.getRepeatNumber());
            if (dosage != null) {
                medicationRequest.getDosageInstruction().add(dosage);
            }
        }

        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            medicationRequest.setSubject(reference);
        }

        Coding coding = cdaBasicElementsConverter.createFHIRCoding(supplyOrder.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_SUPPLY_ORDER_STATUS.toValue());
        if (coding != null) {
            try {
                medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        medicationRequest.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.MEDICATIONREQUEST, medicationRequest.getIdentifier()));
        resources.put(medicationRequest.getId(), medicationRequest);
        return resources;
    }

    private Map<String, Resource> convertToMedicationRequest(MedicationActivity2 medicationActivity, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        CDACommonElementsConverter commonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        MedicationRequest medicationRequest = new MedicationRequest();
        if (CollectionUtils.isNotEmpty(medicationActivity.getIds())) {
            medicationActivity.getIds().forEach(id -> medicationRequest.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        medicationRequest.getDosageInstruction().add(commonElementsConverter.processDosage(medicationActivity));

        medicationActivity.getEffectiveTimes().forEach(et -> {

            if (!et.isSetOperator() || !et.getOperator().equals(SetOperator.A)) {
                Type recordedDate = cdaBasicElementsConverter.convertIVLTSDate((IVL_TS) et);
                if (recordedDate instanceof DateTimeType) {
                    medicationRequest.setAuthoredOnElement((DateTimeType) recordedDate);
                } else if (recordedDate instanceof Period) {
                    medicationRequest.setAuthoredOnElement(((Period) recordedDate).getStartElement());
                }
            }
        });

        if (medicationActivity.getConsumable() != null && medicationActivity.getConsumable().getManufacturedProduct() != null && medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationRequest.setMedication(cdaBasicElementsConverter.createFHIRCodeableConcept(medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial().getCode(), null));
        }

        if (!medicationActivity.getPerformers().isEmpty()) {
            Map<String, Resource> medicationRequestPerformers = new HashMap<>();
            medicationActivity.getPerformers().forEach(performer -> medicationRequestPerformers.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertPerformer(performer, headerResources)));
            if (!medicationRequestPerformers.isEmpty()) {
                List<Resource> practitioners = medicationRequestPerformers.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations = medicationRequestPerformers.values().stream().filter(r -> r instanceof Organization).collect(Collectors.toList());
                if (!practitioners.isEmpty()) {
                    medicationRequest.setRequester(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioners.get(0).getId()));
                } else if (!organizations.isEmpty()) {
                    medicationRequest.setRequester(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organizations.get(0).getId()));
                }

                resources.putAll(medicationRequestPerformers);
            }
        }

        if (!medicationActivity.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthors(medicationRequest, medicationActivity.getAuthors(), headerResources));
        }

        medicationRequest.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.MEDICATIONREQUEST, medicationRequest.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            medicationRequest.setSubject(reference);
        }

        Coding coding = cdaBasicElementsConverter.createFHIRCoding(medicationActivity.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_ACTIVITY_STATUS.toValue());
        if (coding != null) {
            try {
                medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }


        resources.put(medicationRequest.getId(), medicationRequest);
        return resources;
    }


}
