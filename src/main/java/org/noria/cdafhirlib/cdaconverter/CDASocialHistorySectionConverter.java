package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.TS;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDASocialHistorySectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    private final static String  PREGNANCIES_HISTORY_CODE =   "10163-4";
    private final static String  PREGNANCIES_HISTORY_SYSTEM =   "http://loinc.org";
    private final static String  PREGNANCIES_HISTORY_DISPLAY = "History of pregnancies";


    public CDASocialHistorySectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertSocialHistory(SocialHistorySection2 socialHistorySection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();

        for (SocialHistoryObservation2 so : socialHistorySection.getConsolSocialHistoryObservation2s()) {
            resources.putAll(this.basicCDAElementsConverter.createFHIRObservation(so, ObservationCategory.SOCIALHISTORY, new HashMap<>(), headerResources));
        }

        for (PregnancyObservation po : socialHistorySection.getPregnancyObservations()) {
            resources.putAll(this.createPregnancyObservation(po,headerResources));
        }

        for (SmokingStatusMeaningfulUse2 smsmu : socialHistorySection.getConsolCurrentSmokingStatus2s()) {
            resources.putAll(this.basicCDAElementsConverter.createFHIRObservation(smsmu, ObservationCategory.SOCIALHISTORY, new HashMap<>(), headerResources));
        }

        for (TobaccoUse2 tu : socialHistorySection.getConsolTobaccoUse2s()) {
            resources.putAll(this.basicCDAElementsConverter.createFHIRObservation(tu, ObservationCategory.SOCIALHISTORY, new HashMap<>(), headerResources));
        }

        return resources;
    }

    private Map<String, Resource> createPregnancyObservation(PregnancyObservation pregnancyObservation, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        Observation observation = new Observation();
        if (CollectionUtils.isNotEmpty(pregnancyObservation.getIds())) {
            pregnancyObservation.getIds().forEach(id -> observation.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem(ObservationCategory.EXAM.getSystem());
        coding.setCode(ObservationCategory.EXAM.toCode());
        coding.setDisplay(ObservationCategory.EXAM.getDisplay());
        codeableConcept.setCoding(Collections.singletonList(coding));
        observation.setCategory(Collections.singletonList(codeableConcept));

        codeableConcept = new CodeableConcept();
        coding = new Coding();
        coding.setSystem(PREGNANCIES_HISTORY_SYSTEM);
        coding.setCode(PREGNANCIES_HISTORY_CODE);
        coding.setDisplay(PREGNANCIES_HISTORY_DISPLAY);
        codeableConcept.setCoding(Collections.singletonList(coding));
        observation.setCode(codeableConcept);

        if (pregnancyObservation.getStatusCode() != null && !pregnancyObservation.getStatusCode().isSetNullFlavor()) {
            coding = this.basicCDAElementsConverter.createFHIRCoding(pregnancyObservation.getStatusCode(), CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
            if (coding != null) {
                try {
                    observation.setStatus(Observation.ObservationStatus.fromCode(coding.getCode()));
                } catch (FHIRException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        if (pregnancyObservation.getEffectiveTime() != null) {
            Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate(pregnancyObservation.getEffectiveTime());
            observation.setEffective(recordedDate);
        }

        if (!pregnancyObservation.getValues().isEmpty()){
            observation.setValue(this.basicCDAElementsConverter.createFHIRCodeableConcept((CD)pregnancyObservation.getValues().get(0),null));
        }

        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);

        if (pregnancyObservation.getEstimatedDateOfDelivery() != null){
            EstimatedDateOfDelivery edod = pregnancyObservation.getEstimatedDateOfDelivery();
            Observation edodObservation  = new Observation();
            if (CollectionUtils.isNotEmpty(edod.getIds())) {
                edod.getIds().forEach(id -> edodObservation.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
            }
            edodObservation.setCode(this.basicCDAElementsConverter.createFHIRCodeableConcept(edod.getCode(), null));
            if (reference != null) {
                observation.setSubject(reference);
            }
            if (!edod.getValues().isEmpty()) {
                edodObservation.setValue(this.basicCDAElementsConverter.convertTSDate((TS) edod.getValues().get(0)));
            }
            edodObservation.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.OBSERVATION, edodObservation.getIdentifier()));
            observation.addHasMember(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.OBSERVATION, edodObservation.getId()));
            resources.put(edodObservation.getId(), edodObservation);

        }

        if (reference != null) {
            observation.setSubject(reference);
        }

        observation.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.OBSERVATION, observation.getIdentifier()));
        resources.put(observation.getId(), observation);
        return resources;
    }
}
