package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.EIVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.eclipse.mdht.uml.hl7.datatypes.PIVL_TS;
import org.eclipse.mdht.uml.hl7.vocab.SetOperator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAMedicationsSectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAMedicationsSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertMedications(MedicationsSection2 medicationsSection, Map<String, Resource> headerResources){
        Map<String, Resource> resources = new HashMap<>();
        medicationsSection.getConsolMedicationActivity2s().forEach(medicationActivity -> {
            medicationActivity.getAuthors().forEach(author -> resources.putAll(this.basicCDAElementsConverter.convertSectionAuthor(author, headerResources)));
            resources.putAll(this.convertMedicationActivity(medicationActivity, headerResources));
        });

        return resources;
    }

    private Map<String ,Resource> convertMedicationActivity(MedicationActivity2 medicationActivity, Map<String, Resource> headerResources){
        Map<String, Resource> resources = new HashMap<>();
        MedicationRequest medicationRequest = new MedicationRequest();
        if (CollectionUtils.isNotEmpty(medicationActivity.getIds())) {
            medicationActivity.getIds().forEach(id -> medicationRequest.addIdentifier(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRIdentifier(id)));
        }

        Dosage dosage = new Dosage();
        medicationActivity.getEffectiveTimes().forEach(et->{

            if (et.isSetOperator() && et.getOperator().equals(SetOperator.A)){
                if (et instanceof EIVL_TS ){
                    dosage.setTiming(this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertEIVL_TStoFHIRTiming((EIVL_TS) et));
                }
                else if (et instanceof PIVL_TS) {
                    dosage.setTiming(this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertPIVL_TStoFHIRTiming((PIVL_TS) et));
                }
            }
            else{
                Type recordedDate = this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertIVLTSDate((IVL_TS) et);
                if (recordedDate instanceof DateTimeType) {
                    medicationRequest.setAuthoredOnElement((DateTimeType)recordedDate);
                }
                else if (recordedDate instanceof Period){
                    medicationRequest.setAuthoredOnElement(((Period) recordedDate).getStartElement());
                }
            }
        });

        //TODO: COntinue to map medication Activity


        medicationRequest.getDosageInstruction().add(dosage);

        if (medicationActivity.getConsolMedicationDispense2s().size() != 0){
            medicationActivity.getConsolMedicationDispense2s().forEach(md -> resources.putAll(this.convertMedicationDispense(md)));
        }

        if (medicationActivity.getConsolMedicationSupplyOrder2() != null){
            resources.putAll(this.convertMedicationSupply(medicationActivity.getConsolMedicationSupplyOrder2()));
        }

        return resources;
    }

    private Map<String, Resource> convertMedicationDispense(MedicationDispense2 medicationDispense){
        Map<String, Resource> resources = new HashMap<>();
        return resources;
    }

    private Map<String, Resource> convertMedicationSupply(MedicationSupplyOrder2 supplyOrder){
        Map<String, Resource> resources = new HashMap<>();

        return resources;
    }

}
