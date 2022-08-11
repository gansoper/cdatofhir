package org.noria.cdafhirlib.helper;

import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Map;

public class ConvertedElementsHelper {

    public static Practitioner findPractitionerByIdentifier(List<Identifier> authorIdentifiers, Map<String, Resource> resources) {

        Practitioner foundPractitioner = null;
        if (resources != null) {
            String fhirID = FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, authorIdentifiers);
            Resource resource = resources.get(fhirID);
            if (resource instanceof Practitioner) {
                foundPractitioner = (Practitioner) resource;
            }
        }

        return foundPractitioner;
    }
}
