package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.openhealthtools.mdht.uml.cda.consol.FunctionalStatusObservation2;
import org.openhealthtools.mdht.uml.cda.consol.FunctionalStatusOrganizer2;
import org.openhealthtools.mdht.uml.cda.consol.FunctionalStatusSection2;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAFunctionalStatusSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAFunctionalStatusSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertFucntionalStatusObservations(FunctionalStatusSection2 functionalStatusSection2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        functionalStatusSection2.getConsolFunctionalStatusOrganizer2s().forEach(organizer -> {
            Map<String, Resource> organizerAuthors = new HashMap<>();
            organizer.getAuthors().forEach(author -> organizerAuthors.putAll(cdaCommonElementsConverter.convertSectionAuthor(author, headerResources)));
            resources.putAll(organizerAuthors);
            resources.putAll(this.convertFunctionalStatusOrganizer(organizer, headerResources, organizerAuthors));
        });

        functionalStatusSection2.getConsolFunctionalStatusObservation2s().forEach(observation -> resources.putAll(cdaCommonElementsConverter.convertObservationToCondition(observation, null, null, headerResources)));


        return resources;
    }

    private Map<String, Resource> convertFunctionalStatusOrganizer(FunctionalStatusOrganizer2 organizer2, Map<String, Resource> headerResources, Map<String, Resource> organizerAuthors) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        if (!organizer2.getAuthors().isEmpty()) {
            resources.putAll(cdaCommonElementsConverter.convertAuthors(null, organizer2.getAuthors(), headerResources));
        }
        Type recordedDate = cdaBasicElementsConverter.convertIVLTSDate(organizer2.getEffectiveTime());
        for (FunctionalStatusObservation2 functionalStatusObservation : organizer2.getConsolFunctionalStatusObservation2s()) {
            resources.putAll(cdaCommonElementsConverter.convertObservationToCondition(functionalStatusObservation, recordedDate, organizerAuthors, headerResources));
        }

        return resources;
    }

}
