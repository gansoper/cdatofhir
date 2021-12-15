package org.noria.cdafhirlib.fhirconverter;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.openhealthtools.mdht.uml.hl7.datatypes.CD;
import org.openhealthtools.mdht.uml.hl7.datatypes.DatatypesFactory;

public class SimpleFHIRTypesConverter {

    public CD createCD(Coding coding){
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode(coding.getCode());
        cd.setCodeSystem(coding.getSystem());
        cd.setDisplayName(coding.getDisplay());
        return cd;
    }

    public CD createCD(CodeableConcept codeableConcept){
        Coding coding = codeableConcept.getCoding().get(0);
        CD cd = this.createCD(coding);
        codeableConcept.getCoding().stream().skip(1).forEach(c->cd.getTranslations().add(this.createCD(c)));
        return cd;
    }


}
