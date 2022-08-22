package org.noria.cdafhirlib.helper;

import org.hl7.fhir.r4.model.*;

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

  public static Reference getPateintReference(Map<String, Resource> resources){
        Reference reference  = null;
        if (resources != null){
            Resource resource = resources.values().stream().filter(r-> r instanceof Patient).findFirst().orElse(null);
            if (resource != null){
                reference = FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PATIENT, resource.getId());
            }
        }

        return reference;
  }
}
