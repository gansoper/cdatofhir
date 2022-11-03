package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.PQ;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
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

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAProblemsSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertProblems(ProblemSection2 problemSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        problemSection.getConsolProblemConcernAct2s().forEach(act -> {
            Map<String, Resource> actAuthors = new HashMap<>();
            act.getAuthors().forEach(author -> actAuthors.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertSectionAuthor(author, headerResources)));
            resources.putAll(actAuthors);
            resources.putAll(this.convertProblem(act, headerResources, actAuthors));
        });
        return resources;
    }

    private Map<String, Resource> convertProblem(ProblemConcernAct2 act, Map<String, Resource> headerResources, Map<String, Resource> actAuthors) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        if (!act.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthors(null, act.getAuthors(), headerResources));
        }
        Type recordedDate = cdaBasicElementsConverter.convertIVLTSDate(act.getEffectiveTime());
        for (ProblemObservation2 problemObservation : act.getConsolProblemObservation2s()) {
           resources.putAll(cdaCommonElementsConverter.convertObservationToCondition(problemObservation, recordedDate, actAuthors, headerResources));
        }

        return resources;
    }

}
