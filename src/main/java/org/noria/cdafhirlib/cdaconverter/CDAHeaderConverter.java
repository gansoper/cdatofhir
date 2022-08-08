package org.noria.cdafhirlib.cdaconverter;

import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class CDAHeaderConverter {
    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAHeaderConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertHeaderResources(ClinicalDocument cda){
        Map<String, Resource> headerResources = new HashMap<>();
        if (cda.getAuthors().size() != 0){
            cda.getAuthors().forEach(author -> headerResources.putAll(basicCDAElementsConverter.convertAuthor(author)));
        }

        if (cda.getPatientRoles().size() != 0){
            cda.getPatientRoles().forEach(patientRole -> headerResources.putAll(basicCDAElementsConverter.convertPatient(patientRole)));
        }

        if (cda.getCustodian() != null){
            headerResources.putAll(basicCDAElementsConverter.convertCustodian(cda.getCustodian()));
        }

        if (cda.getParticipants().size() != 0){
            cda.getParticipants().forEach(participant -> headerResources.putAll(basicCDAElementsConverter.convertParticipant(participant)));
        }

        return headerResources;
    }

}
