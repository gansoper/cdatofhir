package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.openhealthtools.mdht.uml.cda.consol.VitalSignObservation2;
import org.openhealthtools.mdht.uml.cda.consol.VitalSignsOrganizer2;
import org.openhealthtools.mdht.uml.cda.consol.VitalSignsSection2;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAVitalSignsSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAVitalSignsSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertVitalSigns(VitalSignsSection2 vitalSignsSection2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        vitalSignsSection2.getConsolVitalSignsOrganizer2s().forEach(vsOrganizer -> resources.putAll(this.convertVitalSignOrganizer(vsOrganizer, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertVitalSignOrganizer(VitalSignsOrganizer2 vitalSignsOrganizer2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();


        if (!vitalSignsOrganizer2.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthors(null, vitalSignsOrganizer2.getAuthors(), headerResources));
        }

        if (!vitalSignsOrganizer2.getConsolVitalSignObservation2s().isEmpty()) {
            for (VitalSignObservation2 vso : vitalSignsOrganizer2.getConsolVitalSignObservation2s()) {
                resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).createFHIRObservation(vso, ObservationCategory.VITALSIGNS, resources, headerResources));
            }
        }

        return resources;
    }
}
