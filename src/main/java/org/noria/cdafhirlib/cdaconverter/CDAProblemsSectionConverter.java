package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.PQ;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.ProblemConcernAct2;
import org.openhealthtools.mdht.uml.cda.consol.ProblemObservation2;
import org.openhealthtools.mdht.uml.cda.consol.ProblemSection2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAProblemsSectionConverter {

    private final CDABasicElementsConverter CDABasicElementsConverter;

    public CDAProblemsSectionConverter(CDABasicElementsConverter CDABasicElementsConverter) {
        this.CDABasicElementsConverter = CDABasicElementsConverter;
    }

    public Map<String, Resource> convertProblems(ProblemSection2 problemSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();

        problemSection.getConsolProblemConcernAct2s().forEach(act -> {
            Map<String, Resource> actAuthors = new HashMap<>();
            act.getAuthors().forEach(author -> actAuthors.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertSectionAuthor(author, headerResources)));
            resources.putAll(actAuthors);
            resources.putAll(this.convertProblem(act, headerResources, actAuthors));
        });
        return resources;
    }

    private Map<String, Resource> convertProblem(ProblemConcernAct2 act, Map<String, Resource> headerResources, Map<String, Resource> actAuthors) {
        Map<String, Resource> resources = new HashMap<>();

        if (!act.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertAuthors(null, act.getAuthors(), headerResources));
        }
        Type recordedDate = this.CDABasicElementsConverter.convertIVLTSDate(act.getEffectiveTime());
        for (ProblemObservation2 problemObservation : act.getConsolProblemObservation2s()) {
            Condition condition = new Condition();
            if (CollectionUtils.isNotEmpty(problemObservation.getIds())) {
                problemObservation.getIds().forEach(id -> condition.addIdentifier(this.CDABasicElementsConverter.createFHIRIdentifier(id)));
            }

            if (recordedDate != null){
                if (recordedDate instanceof DateTimeType) {
                    condition.setRecordedDateElement((DateTimeType) recordedDate);
                }
                else if (recordedDate instanceof Period){
                    condition.setRecordedDateElement(((Period) recordedDate).getStartElement());
                }
            }

            if (problemObservation.getEffectiveTime() != null) {
                Type onSetDate = this.CDABasicElementsConverter.convertIVLTSDate(problemObservation.getEffectiveTime());
                condition.setOnset(onSetDate);
                if (onSetDate instanceof Period){
                    Period period = (Period) onSetDate;
                    if (!period.getEndElement().isEmpty()) {
                        condition.setAbatement(period.getEndElement());
                    }
                }
            }

            if (problemObservation.getConsolProblemStatus() != null && !problemObservation.getConsolProblemStatus().getValues().isEmpty()) {
                condition.setClinicalStatus(this.CDABasicElementsConverter.createFHIRCodeableConcept((CD) problemObservation.getConsolProblemStatus().getValues().get(0), CDAtoFHIRCodeConversionType.PROBLEM_STATUS.toValue()));
            } else {
                condition.setClinicalStatus(this.CDABasicElementsConverter.createFHIRCodeableConcept(problemObservation.getStatusCode(), CDAtoFHIRCodeConversionType.PROBLEM_STATUS.toValue()));
            }

            if (problemObservation.getCode() != null) {
                condition.setCategory(Collections.singletonList(this.CDABasicElementsConverter.createFHIRCodeableConcept(problemObservation.getCode(), CDAtoFHIRCodeConversionType.PROBLEM_TYPE.toValue())));
            }

            if (!problemObservation.getValues().isEmpty()) {
                condition.setCode(this.CDABasicElementsConverter.createFHIRCodeableConcept((CD) problemObservation.getValues().get(0), null));
            }

            if (!problemObservation.getAuthors().isEmpty()) {
                resources.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertAuthors(condition, problemObservation.getAuthors(), headerResources));
            } else if (!actAuthors.isEmpty()){
                actAuthors.values().stream().filter(r -> r instanceof Practitioner).findFirst().ifPresent(practitioner ->
                        condition.setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId())));

            }

            if (problemObservation.getAgeObservation() != null && !problemObservation.getAgeObservation().getValues().isEmpty() && !condition.hasOnset()) {
                condition.setOnset(this.CDABasicElementsConverter.createAge((PQ) problemObservation.getAgeObservation().getValues().get(0)));
            }

            Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
            if (reference != null) {
                condition.setSubject(reference);
            }

            condition.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.CONDITION, condition.getIdentifier()));

            resources.put(condition.getId(), condition);
        }

        return resources;
    }

}
