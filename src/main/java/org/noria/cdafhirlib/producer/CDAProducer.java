package org.noria.cdafhirlib.producer;

import org.hl7.fhir.r4.model.Bundle;
import org.openhealthtools.mdht.uml.cda.consol.ConsolFactory;
import org.openhealthtools.mdht.uml.cda.consol.ContinuityOfCareDocument;
import org.openhealthtools.mdht.uml.cda.consol.UnstructuredDocument;

public class CDAProducer {
    // TODO: make variables for the FHIR elements processors



    public ContinuityOfCareDocument createCDAfromFHIRBundle(Bundle fhirBundle){

        UnstructuredDocument unstructuredDocument  = ConsolFactory.eINSTANCE.createUnstructuredDocument();
        ContinuityOfCareDocument ccd = ConsolFactory.eINSTANCE.createContinuityOfCareDocument();
        return null;
    }


}
