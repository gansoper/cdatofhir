package org.noria.cdafhirlib.cdaconverter;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.noria.cdafhirlib.model.CodeToCodeMappingElement;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.CodesMapping;
import org.openhealthtools.mdht.uml.hl7.datatypes.CD;

public class SimpleCDATypesConverter {

    private final CDAtoFHIRCodes codeMappings;

    public SimpleCDATypesConverter(CDAtoFHIRCodes codeMappings){
        this.codeMappings = codeMappings;
    }

    public Coding createFHIRCoding(CD code, String conversionType) {
        if (code !=null) {
           Coding coding = this.getCodeFromMapping(code.getCode(), conversionType);
            if (StringUtils.isAllBlank(coding.getCode())) {
                coding.setCode(code.getCode());
                coding.setSystem(code.getCodeSystem());
                coding.setDisplay(code.getDisplayName());
            }

            return coding;
        }

        return new Coding();
    }

    public CodeableConcept createFHIRCodeableConcept(CD code, String conversionType) {
        Coding coding = this.createFHIRCoding(code, conversionType);
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(coding);
        if (code != null && code.getTranslations() != null) {
            code.getTranslations().forEach(e -> codeableConcept.addCoding(this.createFHIRCoding(e, conversionType)));
        }
        return codeableConcept;
    }

    private Coding getCodeFromMapping(String sourceCode, String conversionType){

        Coding coding = new Coding();
        if (StringUtils.isNoneBlank(conversionType) && this.codeMappings != null) {
            CodeToCodeMappingElement cdAtoFHIRCodeElement = this.codeMappings.getCdaFhirMappings().stream()
                    .filter(e -> e.getType().equalsIgnoreCase(conversionType))
                    .findFirst().orElse(null);

            if (cdAtoFHIRCodeElement != null) {
                CodesMapping codesMapping = cdAtoFHIRCodeElement.getMapping().stream()
                        .filter(e -> e.getSourceCode().equalsIgnoreCase(sourceCode))
                        .findFirst().orElse(null);

                if (codesMapping != null) {
                    coding.setCode(codesMapping.getTargetCode().getCode());
                    coding.setSystem(codesMapping.getTargetCode().getSystem());
                    coding.setDisplay(codesMapping.getTargetCode().getDisplay());
                }

            }
        }

        return coding;
    }

/*
    public void testJSON() throws Exception {
        File file = new File(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json").getFile());
        ObjectMapper om = new ObjectMapper();
        CDAtoFHIRCodes cdAtoFHIRCodes = om.readValue(file, CDAtoFHIRCodes.class);
        cdAtoFHIRCodes.getCdaFhirMappings().forEach(e -> System.out.println(e.getType()));
    }
*/
}
