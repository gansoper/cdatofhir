package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAAdvancedDirectivesSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAAdvancedDirectivesSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertFucntionalStatusObservations(AdvanceDirectivesSection2 advanceDirectivesSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        advanceDirectivesSection.getConsolAdvanceDirectiveOrganizers().forEach(organizer -> {
            Map<String, Resource> organizerAuthors = new HashMap<>();
            organizer.getAuthors().forEach(author -> organizerAuthors.putAll(cdaCommonElementsConverter.convertSectionAuthor(author, headerResources)));
            resources.putAll(organizerAuthors);
            resources.putAll(this.convertAdvancedDirectiveOrganizer(organizer, headerResources, organizerAuthors));
        });

        advanceDirectivesSection.getConsolAdvanceDirectiveObservation2s().forEach(observation -> resources.putAll(cdaCommonElementsConverter.convertObservation(observation, ObservationCategory.ACTIVITY, null, headerResources)));

        return resources;
    }

    private Map<String, Resource> convertAdvancedDirectiveOrganizer(AdvanceDirectiveOrganizer organizer, Map<String, Resource> headerResources, Map<String, Resource> organizerAuthors) {
        Map<String, Resource> resources = new HashMap<>();
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        if (!organizer.getAuthors().isEmpty()) {
            resources.putAll(cdaCommonElementsConverter.convertAuthors(null, organizer.getAuthors(), headerResources));
        }

        for (AdvanceDirectiveObservation2 advanceDirectiveObservation : organizer.getAdvanceDirectiveObservation2s()) {
            resources.putAll(cdaCommonElementsConverter.convertObservation(advanceDirectiveObservation, ObservationCategory.ACTIVITY, organizerAuthors, headerResources));
        }

        return resources;
    }

}
