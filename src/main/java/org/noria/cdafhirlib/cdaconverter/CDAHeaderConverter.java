package org.noria.cdafhirlib.cdaconverter;

import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class CDAHeaderConverter {
    private final CDABasicElementsConverter CDABasicElementsConverter;

    public CDAHeaderConverter(CDABasicElementsConverter CDABasicElementsConverter) {
        this.CDABasicElementsConverter = CDABasicElementsConverter;
    }

    public Map<String, Resource> convertHeaderResources(ClinicalDocument cda) {
        Map<String, Resource> headerResources = new HashMap<>();
        if (cda.getAuthors().size() != 0) {
            cda.getAuthors().forEach(author -> headerResources.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertAuthor(author)));
        }

        if (cda.getPatientRoles().size() != 0) {
            cda.getPatientRoles().forEach(patientRole -> headerResources.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertPatient(patientRole)));
        }

        if (cda.getCustodian() != null) {
            headerResources.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertCustodian(cda.getCustodian()));
        }

        if (cda.getParticipants().size() != 0) {
            cda.getParticipants().forEach(participant -> headerResources.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertParticipant(participant)));
        }

        return headerResources;
    }

}
