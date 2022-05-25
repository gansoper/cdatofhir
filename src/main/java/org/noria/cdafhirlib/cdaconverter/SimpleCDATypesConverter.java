package org.noria.cdafhirlib.cdaconverter;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;

import java.util.stream.Collectors;

@Getter
public class SimpleCDATypesConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public SimpleCDATypesConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Coding createFHIRCoding(CD code, String conversionType) {
        Coding coding = new Coding();
        if (code != null) {
            if (conversionType != null) {
                coding = this.codeMappingProcessor.getCodeFromMapping(code.getCode(), conversionType);
            }
            if (StringUtils.isAllBlank(coding.getCode())) {
                coding.setCode(code.getCode());
                coding.setSystem(codeMappingProcessor.getFHIRCodeSystem(code.getCodeSystem()));
                coding.setDisplay(code.getDisplayName());
            }
        }

        return coding;
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
            address.setUse(Address.AddressUse.fromCode(this.codeMappingProcessor.getStringCodeFromMapping(cdaAddress.getUses().get(0).toString(), CDAtoFHIRCodeConversionType.ADDRESS_USE.toValue())));
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
        if (telecom != null) {
            if (telecom.getUses() != null && telecom.getUses().size() != 0) {
                contactPoint.setUse(ContactPoint.ContactPointUse.fromCode(this.codeMappingProcessor.getStringCodeFromMapping(telecom.getUses().get(0).toString(), CDAtoFHIRCodeConversionType.TELECOM_USE.toValue())));
            }

            contactPoint.setValue(telecom.getValue());
        }
        return contactPoint;
    }

    public HumanName createFHIRHumanName(PN cdaName) {
        HumanName fhirName = new HumanName();
        cdaName.getGivens().forEach(e -> fhirName.addGiven(e.getText()));
        fhirName.setFamily(cdaName.getFamilies().stream().map(ENXP::getText).collect(Collectors.joining(",")));
        return fhirName;
    }

    public Identifier createFHIRIdentifier(II cdaId) {
        Identifier identifier = new Identifier();
        identifier.setValue(cdaId.getExtension());
        identifier.setSystem(cdaId.getRoot());
        return identifier;
    }

    public Enumerations.AdministrativeGender getGender(CD genderCode) throws FHIRException {
        String fhirCode = this.codeMappingProcessor.getStringCodeFromMapping(genderCode.getCode(), CDAtoFHIRCodeConversionType.FAMILY_HISTORY_MEMBER_PERSON_RLT_SUBJ_GENDER.toValue());
        return Enumerations.AdministrativeGender.fromCode(fhirCode);
    }


    public Extension createExtension(CD coding, String url) {
        Extension extension = new Extension();
        extension.setUrl(url);
        extension.setValue(this.createFHIRCoding(coding, null));
        return extension;
    }

    public Extension createExtension(AD address, String url) {
        Extension extension = new Extension();
        extension.setUrl(url);
        extension.setValue(this.createFHIRAddress(address));
        return extension;
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
