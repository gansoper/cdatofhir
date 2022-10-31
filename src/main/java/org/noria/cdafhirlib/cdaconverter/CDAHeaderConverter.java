package org.noria.cdafhirlib.cdaconverter;

import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.hl7.fhir.r4.model.Resource;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;

import java.util.HashMap;
import java.util.Map;

public class CDAHeaderConverter {
    private final CodeMappingProcessor codeMappingProcessor;

    public CDAHeaderConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertHeaderResources(ClinicalDocument cda) {
        Map<String, Resource> headerResources = new HashMap<>();
        if (cda.getAuthors().size() != 0) {
            cda.getAuthors().forEach(author -> headerResources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthor(author)));
        }

        if (cda.getPatientRoles().size() != 0) {
            cda.getPatientRoles().forEach(patientRole -> headerResources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertPatient(patientRole)));
        }

        if (cda.getCustodian() != null) {
            headerResources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertCustodian(cda.getCustodian()));
        }

        if (cda.getParticipants().size() != 0) {
            cda.getParticipants().forEach(participant -> headerResources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertParticipant(participant)));
        }

        return headerResources;
    }

}
