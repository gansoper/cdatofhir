package org.noria.cdafhirlib.helper;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.List;
import java.util.Map;

public class ConvertedElementsHelper {

    public static Practitioner findPractitionerByAuthor(List<Identifier> authorIdentifiers, Map<String, IBaseResource> resources) {
        Practitioner foundPractitioner = null;
        String fhirID = FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, authorIdentifiers);
        IBaseResource resource = resources.get(fhirID);
        if (resource instanceof Practitioner) {
            foundPractitioner = (Practitioner) resource;
        }

        return foundPractitioner;
    }
}
