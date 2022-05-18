package org.noria.cdafhirlib.cdaconverter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.hl7.datatypes.AD;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.ED;
import org.eclipse.mdht.uml.hl7.datatypes.TEL;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.CodeToCodeMappingElement;
import org.noria.cdafhirlib.model.CodesMapping;

import java.util.stream.Collectors;


public class SimpleCDATypesConverter {

    private final CDAtoFHIRCodes codeMappings;

    public SimpleCDATypesConverter(CDAtoFHIRCodes codeMappings) {
        this.codeMappings = codeMappings;
    }

    public Coding createFHIRCoding(CD code, String conversionType) {
        if (code != null) {
            Coding coding = this.getCodeFromMapping(code.getCode(), conversionType);
            if (!StringUtils.isAllBlank(coding.getCode())) {
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

    public Address createFHIRAddress(AD cdaAddress) {
        Address address = new Address();
        if (cdaAddress.getUses() != null && cdaAddress.getUses().size() != 0) {
            address.setUse(Address.AddressUse.fromCode(this.getStringCodeFromMapping(cdaAddress.getUses().get(0).toString(), CDAtoFHIRCodeConversionType.ADDRESS_USE.toValue())));
        }

        address.setLine(cdaAddress.getStreetAddressLines().stream().map(e -> new StringType(e.getText())).collect(Collectors.toList()));
        address.setCity(cdaAddress.getCities().stream().map(ED::getText).collect(Collectors.joining(",")));
        address.setCountry(cdaAddress.getCounties().stream().map(ED::getText).collect(Collectors.joining(",")));
        address.setPostalCode(cdaAddress.getPostalCodes().stream().map(ED::getText).collect(Collectors.joining(",")));
        address.setState(cdaAddress.getStates().stream().map(ED::getText).collect(Collectors.joining(",")));
        return address;
    }

    public ContactPoint createContactPoint(TEL telecom) {
        ContactPoint contactPoint = new ContactPoint();
        return contactPoint;
    }


    private String getStringCodeFromMapping(String sourceCode, String conversionType) {
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

    private Coding getCodeFromMapping(String sourceCode, String conversionType) {

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
