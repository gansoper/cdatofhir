package org.noria.cdafhirlib.fhirconverter;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.noria.cdafhirlib.model.CodeToCodeMappingElement;
import org.noria.cdafhirlib.model.CodesMapping;
import org.noria.cdafhirlib.model.FHIRtoCDACodes;
import org.openhealthtools.mdht.uml.hl7.datatypes.CD;
import org.openhealthtools.mdht.uml.hl7.datatypes.DatatypesFactory;
import org.openhealthtools.mdht.uml.hl7.vocab.NullFlavor;

public class SimpleFHIRTypesConverter {

    private final FHIRtoCDACodes codeMappings;

    public SimpleFHIRTypesConverter(FHIRtoCDACodes fhirToCDACodes) {
        this.codeMappings = fhirToCDACodes;
    }

    public CD createCD(Coding coding, String conversionType) {
        if (coding != null) {
            CD cd = this.getCodeFromMapping(coding.getCode(), conversionType);
            if (StringUtils.isAllBlank(cd.getCode()) && StringUtils.isNotBlank(coding.getCode())) {
                cd.setCode(coding.getCode());
                cd.setCodeSystem(coding.getSystem());
                cd.setDisplayName(coding.getDisplay());
            }
            else{
                cd.setNullFlavor(NullFlavor.UNK);
            }

            return cd;
        }

        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setNullFlavor(NullFlavor.UNK);
        return cd;
    }

    public CD createCDWithTranslation(CodeableConcept codeableConcept, String conversionType) {
        if (codeableConcept != null && codeableConcept.getCoding() != null && codeableConcept.getCoding().size() > 0) {
            Coding coding = codeableConcept.getCoding().get(0);
            CD cd = this.createCD(coding, conversionType);
            codeableConcept.getCoding().stream().skip(1).forEach(c -> cd.getTranslations().add(this.createCD(c, conversionType)));
            return cd;
        }

        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setNullFlavor(NullFlavor.UNK);
        return cd;
    }

    private CD getCodeFromMapping(String sourceCode, String conversionType) {
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        if (StringUtils.isNoneBlank(conversionType) && this.codeMappings != null) {
            CodeToCodeMappingElement codeToCodeMappingElement = this.codeMappings.getFhirCdaMappings().stream()
                    .filter(e -> e.getType().equalsIgnoreCase(conversionType))
                    .findFirst().orElse(null);

            if (codeToCodeMappingElement != null) {
                CodesMapping codesMapping = codeToCodeMappingElement.getMapping().stream()
                        .filter(e -> e.getSourceCode().equalsIgnoreCase(sourceCode))
                        .findFirst().orElse(null);

                if (codesMapping != null) {
                    cd.setCode(codesMapping.getTargetCode().getCode());
                    cd.setCodeSystem(codesMapping.getTargetCode().getSystem());
                    cd.setDisplayName(codesMapping.getTargetCode().getDisplay());
                }

            }
        }

        return cd;
    }


}
