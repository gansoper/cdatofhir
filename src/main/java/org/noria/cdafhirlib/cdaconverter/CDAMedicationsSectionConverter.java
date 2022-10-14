package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.eclipse.mdht.uml.hl7.vocab.SetOperator;
import org.eclipse.mdht.uml.hl7.vocab.x_DocumentSubstanceMood;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
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

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAMedicationsSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertMedications(MedicationsSection2 medicationsSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        medicationsSection.getConsolMedicationActivity2s().forEach(medicationActivity -> resources.putAll(this.convertMedicationActivity(medicationActivity, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertMedicationActivity(MedicationActivity2 medicationActivity, Map<String, Resource> headerResources) {

        Map<String, Resource> resources = new HashMap<>();
        if (medicationActivity.getMoodCode().equals(x_DocumentSubstanceMood.INT)) {
            resources.putAll(this.convertToMedicationRequest(medicationActivity, headerResources));
        } else if (medicationActivity.getMoodCode().equals(x_DocumentSubstanceMood.EVN)) {
            resources.putAll(this.convertToMedicationStatement(medicationActivity, headerResources));
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
        MedicationDispense medicationDispense = new MedicationDispense();
        if (CollectionUtils.isNotEmpty(medicationDispenseCDA.getIds())) {
            medicationDispenseCDA.getIds().forEach(id -> medicationDispense.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        if ((medicationDispenseCDA.getQuantity() != null && medicationDispenseCDA.getQuantity().getValue() != null) || medicationDispenseCDA.getRepeatNumber() != null) {
            Dosage dosage = this.convertQuantityToDosageRate(medicationDispenseCDA.getQuantity(), medicationDispenseCDA.getRepeatNumber());
            if (dosage != null) {
                medicationDispense.getDosageInstruction().add(dosage);
            }

            SimpleQuantity simpleQuantity = this.basicCDAElementsConverter.createSimpleQuantity(medicationDispenseCDA.getQuantity());
            if (simpleQuantity != null) {
                medicationDispense.setQuantity(simpleQuantity);
            }
        }

        medicationDispenseCDA.getEffectiveTimes().forEach(et -> {
            DateTimeType recordedDate = this.basicCDAElementsConverter.convertSXMTSDate(et);
            if (recordedDate != null){
                medicationDispense.setWhenPreparedElement(recordedDate);
            }

        });

        if (medicationDispenseCDA.getProduct() != null && medicationDispenseCDA.getProduct().getManufacturedProduct() != null && medicationDispenseCDA.getProduct().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationDispense.setMedication(this.basicCDAElementsConverter.createFHIRCodeableConcept(medicationDispenseCDA.getProduct().getManufacturedProduct().getManufacturedMaterial().getCode(), null));
        }


        if (!medicationDispenseCDA.getPerformers().isEmpty()) {
            Map<String, Resource> medicationRequestPerformers = new HashMap<>();
            medicationDispenseCDA.getPerformers().forEach(performer -> medicationRequestPerformers.putAll(this.basicCDAElementsConverter.convertPerformer(performer, headerResources)));
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

        Coding coding = basicCDAElementsConverter.createFHIRCoding(medicationDispenseCDA.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_DISPENSE_STATUS.toValue());
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
        Reference reference = ConvertedElementsHelper.getPateintReference(headerResources);
        if (reference != null) {
            medicationDispense.setSubject(reference);
        }
        resources.put(medicationDispense.getId(), medicationDispense);
        return resources;
    }

    private Map<String, Resource> convertMedicationSupply(MedicationSupplyOrder2 supplyOrder, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        MedicationRequest medicationRequest = new MedicationRequest();
        if (CollectionUtils.isNotEmpty(supplyOrder.getIds())) {
            supplyOrder.getIds().forEach(id -> medicationRequest.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        supplyOrder.getEffectiveTimes().forEach(et -> {
            Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate((IVL_TS) et);
            if (recordedDate instanceof DateTimeType) {
                medicationRequest.setAuthoredOnElement((DateTimeType) recordedDate);
            } else if (recordedDate instanceof Period) {
                medicationRequest.setAuthoredOnElement(((Period) recordedDate).getStartElement());
            }
        });

        if (!supplyOrder.getAuthors().isEmpty()) {
            Map<String, Resource> medicationRequestAuthors = this.basicCDAElementsConverter.convertSectionAuthors(supplyOrder.getAuthors(), headerResources);
            if (!medicationRequestAuthors.isEmpty()) {
                Resource practitioner = medicationRequestAuthors.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
                if (practitioner != null){
                    medicationRequest.setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));

                }

                resources.putAll(medicationRequestAuthors);
            }
        }

        if (supplyOrder.getProduct() != null && supplyOrder.getProduct().getManufacturedProduct() != null && supplyOrder.getProduct().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationRequest.setMedication(this.basicCDAElementsConverter.createFHIRCodeableConcept(supplyOrder.getProduct().getManufacturedProduct().getManufacturedMaterial().getCode(), null));
        }


        if ((supplyOrder.getQuantity() != null && supplyOrder.getQuantity().getValue() != null) || supplyOrder.getRepeatNumber() != null) {
            Dosage dosage = this.convertQuantityToDosageRate(supplyOrder.getQuantity(), supplyOrder.getRepeatNumber());
            if (dosage != null) {
                medicationRequest.getDosageInstruction().add(dosage);
            }
        }

        Reference reference = ConvertedElementsHelper.getPateintReference(headerResources);
        if (reference != null) {
            medicationRequest.setSubject(reference);
        }

        Coding coding = basicCDAElementsConverter.createFHIRCoding(supplyOrder.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_SUPPLY_ORDER_STATUS.toValue());
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
        MedicationRequest medicationRequest = new MedicationRequest();
        if (CollectionUtils.isNotEmpty(medicationActivity.getIds())) {
            medicationActivity.getIds().forEach(id -> medicationRequest.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        medicationRequest.getDosageInstruction().add(this.processDosage(medicationActivity));

        medicationActivity.getEffectiveTimes().forEach(et -> {

            if (!et.isSetOperator() || !et.getOperator().equals(SetOperator.A)) {
                Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate((IVL_TS) et);
                if (recordedDate instanceof DateTimeType) {
                    medicationRequest.setAuthoredOnElement((DateTimeType) recordedDate);
                } else if (recordedDate instanceof Period) {
                    medicationRequest.setAuthoredOnElement(((Period) recordedDate).getStartElement());
                }
            }
        });

        if (medicationActivity.getConsumable() != null && medicationActivity.getConsumable().getManufacturedProduct() != null && medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationRequest.setMedication(this.basicCDAElementsConverter.createFHIRCodeableConcept(medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial().getCode(), null));
        }

        if (!medicationActivity.getPerformers().isEmpty()) {
            Map<String, Resource> medicationRequestPerformers = new HashMap<>();
            medicationActivity.getPerformers().forEach(performer -> medicationRequestPerformers.putAll(this.basicCDAElementsConverter.convertPerformer(performer, headerResources)));
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
            Map<String, Resource> medicationRequestAuthors = this.basicCDAElementsConverter.convertSectionAuthors(medicationActivity.getAuthors(), headerResources);
            if (!medicationRequestAuthors.isEmpty()) {
                Resource practitioner = medicationRequestAuthors.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
                if (practitioner != null){
                    medicationRequest.setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));

                }

                resources.putAll(medicationRequestAuthors);
            }
        }

        medicationRequest.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.MEDICATIONREQUEST, medicationRequest.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPateintReference(headerResources);
        if (reference != null) {
            medicationRequest.setSubject(reference);
        }

        Coding coding = basicCDAElementsConverter.createFHIRCoding(medicationActivity.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_ACTIVITY_STATUS.toValue());
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

    private Map<String, Resource> convertToMedicationStatement(MedicationActivity2 medicationActivity, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        MedicationStatement medicationStatement = new MedicationStatement();
        if (CollectionUtils.isNotEmpty(medicationActivity.getIds())) {
            medicationActivity.getIds().forEach(id -> medicationStatement.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        medicationStatement.getDosage().add(this.processDosage(medicationActivity));

        if (medicationActivity.getConsumable() != null && medicationActivity.getConsumable().getManufacturedProduct() != null && medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationStatement.setMedication(this.basicCDAElementsConverter.createFHIRCodeableConcept(medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial().getCode(), null));

        }

        if (!medicationActivity.getPerformers().isEmpty()) {
            Map<String, Resource> medicationRequestPerformers = new HashMap<>();
            medicationActivity.getPerformers().forEach(performer -> medicationRequestPerformers.putAll(this.basicCDAElementsConverter.convertPerformer(performer, headerResources)));
            if (!medicationRequestPerformers.isEmpty()) {
                List<Resource> practitioners = medicationRequestPerformers.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations = medicationRequestPerformers.values().stream().filter(r -> r instanceof Organization).collect(Collectors.toList());
                if (!practitioners.isEmpty()) {
                    medicationStatement.setInformationSource(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioners.get(0).getId()));
                } else if (!organizations.isEmpty()) {
                    medicationStatement.setInformationSource(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organizations.get(0).getId()));
                }

                resources.putAll(medicationRequestPerformers);
            }
        } else if (!medicationActivity.getAuthors().isEmpty()) {
            Map<String, Resource> medicationStatementAuthors = this.basicCDAElementsConverter.convertSectionAuthors(medicationActivity.getAuthors(), headerResources);
            if (!medicationStatementAuthors.isEmpty()) {
                Resource practitioner = medicationStatementAuthors.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
                if (practitioner != null){
                    medicationStatement.setInformationSource(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));
                }

                resources.putAll(medicationStatementAuthors);
            }
        }

        medicationStatement.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.MEDICATIONSTATEMENT, medicationStatement.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPateintReference(headerResources);
        if (reference != null) {
            medicationStatement.setSubject(reference);
        }

        Coding coding = basicCDAElementsConverter.createFHIRCoding(medicationActivity.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_ACTIVITY_STATEMENT_STATUS.toValue());
        if (coding != null) {
            try {
                medicationStatement.setStatus(MedicationStatement.MedicationStatementStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        resources.put(medicationStatement.getId(), medicationStatement);
        return resources;
    }

    private Dosage processDosage(MedicationActivity2 medicationActivity) {
        Dosage dosage = new Dosage();
        medicationActivity.getEffectiveTimes().forEach(et -> {

            if (et.isSetOperator() && et.getOperator().equals(SetOperator.A)) {
                if (et instanceof EIVL_TS) {
                    dosage.setTiming(this.basicCDAElementsConverter.convertEIVL_TStoFHIRTiming((EIVL_TS) et));
                } else if (et instanceof PIVL_TS) {
                    dosage.setTiming(this.basicCDAElementsConverter.convertPIVL_TStoFHIRTiming((PIVL_TS) et));
                }
            }
        });

        if (medicationActivity.getRepeatNumber() != null && medicationActivity.getRepeatNumber().getValue() != null) {
            dosage.getTiming().getRepeat().setCount(medicationActivity.getRepeatNumber().getValue().intValue());
        }

        dosage.setRoute(this.basicCDAElementsConverter.createFHIRCodeableConcept(medicationActivity.getRouteCode(), null));
        dosage.setSite(this.basicCDAElementsConverter.createFHIRCodeableConceptFromList(medicationActivity.getApproachSiteCodes(), null));

        Dosage.DosageDoseAndRateComponent dosageDoseAndRateComponent = new Dosage.DosageDoseAndRateComponent();


        if (medicationActivity.getRateQuantity() != null && medicationActivity.getRateQuantity().getValue() != null) {
            dosageDoseAndRateComponent.setRate(this.basicCDAElementsConverter.createSimpleQuantity(medicationActivity.getRateQuantity()));
        } else if (medicationActivity.getRateQuantity() != null && medicationActivity.getRateQuantity().getLow() != null && medicationActivity.getRateQuantity().getHigh() != null) {
            dosageDoseAndRateComponent.setRate(this.basicCDAElementsConverter.createRange(medicationActivity.getRateQuantity()));
        } else if (medicationActivity.getMaxDoseQuantity() != null) {
            dosageDoseAndRateComponent.setRate(this.basicCDAElementsConverter.createRatio(medicationActivity.getMaxDoseQuantity()));
        }

        if (medicationActivity.getDoseQuantity() != null && medicationActivity.getDoseQuantity().getValue() != null) {
            dosageDoseAndRateComponent.setDose(this.basicCDAElementsConverter.createSimpleQuantity(medicationActivity.getDoseQuantity()));
        } else if (medicationActivity.getDoseQuantity() != null) {
            dosageDoseAndRateComponent.setDose(this.basicCDAElementsConverter.createRange(medicationActivity.getDoseQuantity()));
        }

        dosage.getDoseAndRate().add(dosageDoseAndRateComponent);

        return dosage;
    }

    private Dosage convertQuantityToDosageRate(PQ quantity, IVL_INT repeatNumber) {
        Dosage dosage = null;
        if (repeatNumber != null && repeatNumber.getValue() != null) {
            dosage = new Dosage();
            Timing timing = new Timing();
            Timing.TimingRepeatComponent repeatComponent = new Timing.TimingRepeatComponent();
            repeatComponent.setCount(repeatNumber.getValue().intValue());
            timing.setRepeat(repeatComponent);
            dosage.setTiming(timing);
        }
        if (quantity != null) {
            if (dosage == null) {
                dosage = new Dosage();
            }
            Dosage.DosageDoseAndRateComponent dosageDoseAndRateComponent = new Dosage.DosageDoseAndRateComponent();
            dosageDoseAndRateComponent.setRate(this.basicCDAElementsConverter.createSimpleQuantity(quantity));
            dosage.getDoseAndRate().add(dosageDoseAndRateComponent);
        }

        return dosage;
    }


}
