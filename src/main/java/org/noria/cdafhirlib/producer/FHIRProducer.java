package org.noria.cdafhirlib.producer;

import org.hl7.fhir.r4.model.Bundle;
import java.io.InputStream;
import java.util.List;

public class FHIRProducer {

    //TODO: make variables for the section processors


    public FHIRProducer(){

    }

    public FHIRProducer(List<Object> cdaSectionProcessor){

    }

    public Bundle produceFHIRBudnle(InputStream cdaInputStream) {
        return null;
    }
}
