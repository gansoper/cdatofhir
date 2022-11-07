package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.eclipse.mdht.uml.cda.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.openhealthtools.mdht.uml.cda.consol.MedicalHistorySection;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAPastMedicalHistorySectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAPastMedicalHistorySectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertPastMedicalHistory(MedicalHistorySection medicalHistorySection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        for (Observation problemObservation : medicalHistorySection.getObservations()) {
            resources.putAll(cdaCommonElementsConverter.convertObservationToCondition(problemObservation, null, null, headerResources));
        }

        return resources;
    }

}
