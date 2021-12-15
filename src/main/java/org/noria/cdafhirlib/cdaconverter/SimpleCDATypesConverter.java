package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.openhealthtools.mdht.uml.hl7.datatypes.CD;

import java.io.File;

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

    public void testJSON() throws Exception{
        File file = new File(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json").getFile());
        ObjectMapper om = new ObjectMapper();
        CDAtoFHIRCodes cdAtoFHIRCodes  = om.readValue(file,CDAtoFHIRCodes.class);
       cdAtoFHIRCodes.getCdaFhirMappings().stream().forEach(e-> System.out.println(e.getType()));
    }

}
