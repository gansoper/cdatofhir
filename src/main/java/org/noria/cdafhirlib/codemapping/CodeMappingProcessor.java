package org.noria.cdafhirlib.codemapping;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Coding;
import org.noria.cdafhirlib.constants.BaseConstants;
import org.noria.cdafhirlib.model.*;

import java.util.regex.Pattern;

public class CodeMappingProcessor {

    private final CDAtoFHIRCodes codeMappings;
    private final SystemNamesMapping systemNamesMapping;

    public CodeMappingProcessor(CDAtoFHIRCodes codeMappings, SystemNamesMapping systemNamesMapping) {
        this.codeMappings = codeMappings;
        this.systemNamesMapping = systemNamesMapping;
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
                    coding.setSystem(this.getFHIRCodeSystem(codesMapping.getTargetCode().getSystem()));
                    coding.setDisplay(codesMapping.getTargetCode().getDisplay());
                }

            }
        }

        return coding;
    }

    public String getFHIRCodeSystem(String cdaCodeSystem) {

        String fhirCodeSystem = null;
        if (StringUtils.isNoneBlank(cdaCodeSystem)) {
            SystemMapping fhirSystem = this.systemNamesMapping.getSystems().stream().filter(e -> e.getOid().equals(cdaCodeSystem)).findFirst().orElse(null);
            if (fhirSystem != null) {
                fhirCodeSystem = fhirSystem.getUrl();
            } else {
                Pattern p = Pattern.compile(BaseConstants.OID_REGEX_PATTERN);
                if (p.matcher(cdaCodeSystem).matches()) {
                    fhirCodeSystem = BaseConstants.URN_OID + cdaCodeSystem;
                }
            }
        }
        return fhirCodeSystem;
    }


}
