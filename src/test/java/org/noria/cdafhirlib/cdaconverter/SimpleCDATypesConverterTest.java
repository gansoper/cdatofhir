package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.hl7.datatypes.AD;
import org.eclipse.mdht.uml.hl7.datatypes.ADXP;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.DatatypesFactory;
import org.eclipse.mdht.uml.hl7.vocab.PostalAddressUse;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SimpleCDATypesConverterTest {

    @Test
    void createFHIRCodingNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(null);
        Coding coding = simpleCDATypesConverter.createFHIRCoding(null, null);
        assertNotEquals(coding, null);
        assertNull(coding.getCode());
    }

    @Test
    void createFHIRCodingNotInJSONEmptyCD() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(null);
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, "test");
        assertNotEquals(coding, null);
        assertNull(coding.getCode());
    }

    @Test
    void createFHIRCodingNotInJSONCDValue() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(null);
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, "test");
        assertNotEquals(coding, null);
        assertEquals(coding.getCode(), "test");
    }

    @Test
    void createFHIRCodingNotInJSONCDValueFilled() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(this.getTestCodes());
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("completed");
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
        assertNotEquals(coding, null);
        assertEquals(coding.getCode(), "final");
    }


    @Test
    void codeabelConceptNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(null);
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        CodeableConcept codeableConcept = simpleCDATypesConverter.createFHIRCodeableConcept(null, null);
        assertNotEquals(codeableConcept, null);
        assertEquals(codeableConcept.getCoding().size(), 1);

    }

    @Test
    void codeabelConceptNotNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(null);
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        CodeableConcept codeableConcept = simpleCDATypesConverter.createFHIRCodeableConcept(cd, null);
        assertNotEquals(codeableConcept, null);
        assertEquals(codeableConcept.getCoding().size(), 1);
        assertEquals(codeableConcept.getCoding().get(0).getCode(), "test");
    }

    @Test
    void codeabelConceptNotNullFromJson() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(this.getTestCodes());
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("completed");
        CD translation = DatatypesFactory.eINSTANCE.createCD();
        translation.setCode("active");
        cd.getTranslations().add(translation);
        CodeableConcept codeableConcept = simpleCDATypesConverter.createFHIRCodeableConcept(cd, CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
        assertNotEquals(codeableConcept, null);
        assertEquals(codeableConcept.getCoding().size(), 2);
        assertEquals(codeableConcept.getCoding().get(0).getCode(), "final");
        assertEquals(codeableConcept.getCoding().get(1).getCode(), "registered");
    }


    @Test
    void addressEmptyConversionTest(){
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(this.getTestCodes());
        AD ad = DatatypesFactory.eINSTANCE.createAD();
        Address address = simpleCDATypesConverter.createFHIRAddress(ad);
        assertNotNull(address);
        assertNull(address.getCity());
    }

    @Test
    void addressConversionTestLinesAndUses(){
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(this.getTestCodes());
        AD ad = DatatypesFactory.eINSTANCE.createAD();
        ad.getUses().add(PostalAddressUse.BAD);
        ADXP adxp = DatatypesFactory.eINSTANCE.createADXP();
        adxp.addText("test1");
        ADXP adxp2 = DatatypesFactory.eINSTANCE.createADXP();
        adxp2.addText("test2");
        ad.getStreetAddressLines().add(adxp);
        ad.getStreetAddressLines().add(adxp2);
        Address address = simpleCDATypesConverter.createFHIRAddress(ad);
        assertEquals(address.getUse(), Address.AddressUse.OLD);
        assertEquals(address.getLine().get(0).toString(), "test1");
        assertEquals(address.getLine().get(1).toString(), "test2");

    }

    @Test
    void addressConversionTestCities(){
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(this.getTestCodes());
        AD ad = DatatypesFactory.eINSTANCE.createAD();
        ADXP adxp = DatatypesFactory.eINSTANCE.createADXP();
        adxp.addText("test1");
        ADXP adxp2 = DatatypesFactory.eINSTANCE.createADXP();
        adxp2.addText("test2");
        ad.getCities().add(adxp);
        ad.getCities().add(adxp2);
        Address address = simpleCDATypesConverter.createFHIRAddress(ad);
        assertEquals(address.getCity(), "test1,test2");

    }

    @Test
    void createContactPoint() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(this.getTestCodes());
        ContactPoint contactPoint  = simpleCDATypesConverter.createContactPoint(null);
        assertNotNull(contactPoint);
        assertNull(contactPoint.getUse());
    }


    private CDAtoFHIRCodes getTestCodes() {
        try {
            File file = new File(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json").getFile());
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, CDAtoFHIRCodes.class);
        } catch (Exception e) {
            return null;
        }
    }


}