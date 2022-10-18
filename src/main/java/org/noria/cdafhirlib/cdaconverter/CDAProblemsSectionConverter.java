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

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAProblemsSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertProblems(ProblemSection2 problemSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        problemSection.getConsolProblemConcernAct2s().forEach(act -> resources.putAll(this.convertProblem(act, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertProblem(ProblemConcernAct2 act, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();

        if (!act.getAuthors().isEmpty()) {
            resources.putAll(this.basicCDAElementsConverter.convertAuthors(null, act.getAuthors(), headerResources));
        }

        for (ProblemObservation2 problemObservation : act.getConsolProblemObservation2s()) {
            Condition condition = new Condition();
            if (CollectionUtils.isNotEmpty(problemObservation.getIds())) {
                problemObservation.getIds().forEach(id -> condition.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
            }

            if (problemObservation.getEffectiveTime() != null) {
                Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate(problemObservation.getEffectiveTime());
                condition.setOnset(recordedDate);
            }

            if (problemObservation.getConsolProblemStatus() != null && !problemObservation.getConsolProblemStatus().getValues().isEmpty()) {
                condition.setClinicalStatus(this.basicCDAElementsConverter.createFHIRCodeableConcept((CD) problemObservation.getConsolProblemStatus().getValues().get(0), CDAtoFHIRCodeConversionType.PROBLEM_STATUS.toValue()));
            } else {
                condition.setClinicalStatus(this.basicCDAElementsConverter.createFHIRCodeableConcept(problemObservation.getStatusCode(), CDAtoFHIRCodeConversionType.PROBLEM_STATUS.toValue()));
            }

            if (problemObservation.getCode() != null) {
                condition.setCategory(Collections.singletonList(this.basicCDAElementsConverter.createFHIRCodeableConcept(problemObservation.getCode(), CDAtoFHIRCodeConversionType.PROBLEM_TYPE.toValue())));
            }

            if (!problemObservation.getValues().isEmpty()) {
                condition.setCode(this.basicCDAElementsConverter.createFHIRCodeableConcept((CD) problemObservation.getValues().get(0), null));
            }

            if (!problemObservation.getAuthors().isEmpty()) {
                resources.putAll(this.basicCDAElementsConverter.convertAuthors(condition, problemObservation.getAuthors(), headerResources));
            }

            if (problemObservation.getAgeObservation() != null && !problemObservation.getAgeObservation().getValues().isEmpty()) {
                condition.setOnset(this.basicCDAElementsConverter.createAge((PQ) problemObservation.getAgeObservation().getValues().get(0)));
                ;
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
