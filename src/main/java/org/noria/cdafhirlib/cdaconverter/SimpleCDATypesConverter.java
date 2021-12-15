package org.noria.cdafhirlib.cdaconverter;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.openhealthtools.mdht.uml.hl7.datatypes.CD;

public class SimpleCDATypesConverter {

    public Coding createFHIRCoding(CD code) {
        Coding coding = new Coding();
        coding.setCode(code.getCode());
        coding.setSystem(code.getCodeSystem());
        coding.setDisplay(code.getDisplayName());
        return coding;
    }

    public CodeableConcept createFHIRCodeableConcept(CD code) {
        Coding coding = this.createFHIRCoding(code);
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(coding);
        if (code.getTranslations() != null) {
            code.getTranslations().forEach(e -> codeableConcept.addCoding(this.createFHIRCoding(e)));
        }
        return codeableConcept;
    }

}
