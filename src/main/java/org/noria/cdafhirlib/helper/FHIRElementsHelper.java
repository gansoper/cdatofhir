package org.noria.cdafhirlib.helper;

import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;

import java.util.List;
import java.util.UUID;

public class FHIRElementsHelper {

    public static String createFHIRID(Enumerations.FHIRAllTypes fhirAllTypes, List<Identifier> identifiers){
        String fhirID = CollectionUtils.isNotEmpty(identifiers) ? identifiers.get(0).getValue(): UUID.randomUUID().toString();
        return  fhirAllTypes.toCode() + "_" + fhirID;
    }

    public static Reference createReference(Enumerations.FHIRAllTypes fhirAllTypes, String id){
        Reference reference = new Reference();
        reference.setReference(fhirAllTypes.toCode() + "/" + id);
        return reference;
    }
}
