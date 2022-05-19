package org.noria.cdafhirlib.codemapping;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Coding;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.CodeToCodeMappingElement;
import org.noria.cdafhirlib.model.CodesMapping;

public class CodeMappingProcessor {

    private static CodeMappingProcessor instance;
    private final CDAtoFHIRCodes codeMappings;

    private CodeMappingProcessor(CDAtoFHIRCodes codeMappings){
        this.codeMappings = codeMappings;
    }

    public static CodeMappingProcessor getInstance(CDAtoFHIRCodes codeMappings){
        if (instance == null){
            instance = new CodeMappingProcessor(codeMappings);
        }

        return instance;
    }


    public String getStringCodeFromMapping(String sourceCode, String conversionType) {
        if (StringUtils.isNoneBlank(conversionType) && this.codeMappings != null) {
            CodeToCodeMappingElement cdAtoFHIRCodeElement = this.codeMappings.getCdaFhirMappings().stream()
                    .filter(e -> e.getType().equalsIgnoreCase(conversionType))
                    .findFirst().orElse(null);

            if (cdAtoFHIRCodeElement != null) {
                CodesMapping codesMapping = cdAtoFHIRCodeElement.getMapping().stream()
                        .filter(e -> e.getSourceCode().equalsIgnoreCase(sourceCode))
                        .findFirst().orElse(null);

                if (codesMapping != null) {
                    return codesMapping.getTargetCode().getCode();
                }
            }
        }

        return sourceCode;
    }

    public Coding getCodeFromMapping(String sourceCode, String conversionType) {

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

}
