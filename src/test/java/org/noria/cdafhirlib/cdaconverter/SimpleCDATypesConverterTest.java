package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.eclipse.mdht.uml.hl7.vocab.PostalAddressUse;
import org.eclipse.mdht.uml.hl7.vocab.TelecommunicationAddressUse;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;

import javax.xml.crypto.Data;
import java.io.File;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SimpleCDATypesConverterTest {

    @Test
    void createFHIRCodingNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        Coding coding = simpleCDATypesConverter.createFHIRCoding(null, null);
        assertEquals(coding, null);
    }

    @Test
    void createFHIRCodingNotInJSONEmptyCD() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, "test");
        assertNotEquals(coding, null);
        assertNull(coding.getCode());
    }

    @Test
    void createFHIRCodingNotInJSONCDValue() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, "test");
        assertNotEquals(coding, null);
        assertEquals(coding.getCode(), "test");
    }

    @Test
    void createFHIRCodingNotInJSONCDValueFilled() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("completed");
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
        assertNotEquals(coding, null);
        assertEquals(coding.getCode(), "final");
    }


    @Test
    void codeabelConceptNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        CodeableConcept codeableConcept = simpleCDATypesConverter.createFHIRCodeableConcept(null, null);
        assertEquals(codeableConcept, null);

    }

    @Test
    void codeabelConceptNotNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        CodeableConcept codeableConcept = simpleCDATypesConverter.createFHIRCodeableConcept(cd, null);
        assertNotEquals(codeableConcept, null);
        assertEquals(codeableConcept.getCoding().size(), 1);
        assertEquals(codeableConcept.getCoding().get(0).getCode(), "test");
    }

    @Test
    void codeabelConceptNotNullFromJson() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        AD ad = DatatypesFactory.eINSTANCE.createAD();
        Address address = simpleCDATypesConverter.createFHIRAddress(ad);
        assertNotNull(address);
        assertNull(address.getCity());
    }

    @Test
    void addressConversionTestLinesAndUses(){
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        ContactPoint contactPoint  = simpleCDATypesConverter.createContactPoint(null);
        assertNotNull(contactPoint);
        assertNull(contactPoint.getUse());
    }

    @Test
    void createContactPointFilled() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        TEL telecom = DatatypesFactory.eINSTANCE.createTEL();
        telecom.getUses().add(TelecommunicationAddressUse.HP);
        telecom.setValue("test");
        ContactPoint contactPoint  = simpleCDATypesConverter.createContactPoint(telecom);
        assertNotNull(contactPoint);
        assertNotNull(contactPoint.getUse());
        assertEquals(contactPoint.getUse(), ContactPoint.ContactPointUse.HOME);
        assertEquals(contactPoint.getValue(), "test");
    }

    @Test
    void convertEIVL_TStoFHIRTimingNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        EIVL_TS eventInterval = null;
        Timing timing = simpleCDATypesConverter.convertEIVL_TStoFHIRTiming(eventInterval);
        assertNotNull(timing);
        assertNotNull(timing.getRepeat());
        assertTrue(timing.getCode().getCoding().isEmpty());

    }

    @Test
    void convertEIVL_TStoFHIRTimingNotNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        EIVL_TS eventInterval = DatatypesFactory.eINSTANCE.createEIVL_TS();
        IVL_PQ offsetValue = DatatypesFactory.eINSTANCE.createIVL_PQ();
        offsetValue.setValue(new BigDecimal(22));
        eventInterval.setOffset(offsetValue);
        EIVL_event eventCode = DatatypesFactory.eINSTANCE.createEIVL_event();
        eventCode.setCode("test");
        eventCode.setCodeSystem("2.16.840.1.113883.5.139");
        eventCode.setDisplayName("TEST");
        eventInterval.setEvent(eventCode);
        Timing timing = simpleCDATypesConverter.convertEIVL_TStoFHIRTiming(eventInterval);

        assertNotNull(timing);
        assertNotNull(timing.getRepeat());
        assertEquals(timing.getRepeat().getOffset(), 22);
        assertTrue(timing.getCode().getCoding().size() == 1);
        assertEquals(timing.getCode().getCoding().get(0).getSystem(), "urn:oid:2.16.840.1.113883.5.139");
        assertEquals(timing.getCode().getCoding().get(0).getCode(), "test");
    }

    @Test
    void convertPIVL_TStoFHIRTimingNull() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        PIVL_TS periodicInterval = null;
        Timing timing = simpleCDATypesConverter.convertPIVL_TStoFHIRTiming(periodicInterval);
        assertNotNull(timing);
        assertNotNull(timing.getRepeat());
        assertTrue(timing.getCode().getCoding().isEmpty());
    }


    @Test
    void convertPIVL_TStoFHIRTimingNotNullPeriod() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        PIVL_TS periodicInterval = DatatypesFactory.eINSTANCE.createPIVL_TS();
        periodicInterval.setValue("20130311");
        PQ period = DatatypesFactory.eINSTANCE.createIVL_PQ();
        period.setUnit("h");
        period.setValue(new BigDecimal(6));
        periodicInterval.setPeriod(period);
        Timing timing = simpleCDATypesConverter.convertPIVL_TStoFHIRTiming(periodicInterval);
        assertNotNull(timing);
        assertNotNull(timing.getRepeat());
        assertEquals(timing.getRepeat().getPeriod().intValue(), 6);
        assertEquals(timing.getRepeat().getPeriodUnit(), Timing.UnitsOfTime.H);
        assertFalse(timing.getEvent().isEmpty());
        assertEquals(timing.getEvent().get(0).getValueAsString(), "2013-03-11");
    }

    @Test
    void convertPIVL_TStoFHIRTimingNotNullPhase() {
        SimpleCDATypesConverter simpleCDATypesConverter = new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        PIVL_TS periodicInterval = DatatypesFactory.eINSTANCE.createPIVL_TS();
        periodicInterval.setValue("200130311");
        IVL_TS phase = DatatypesFactory.eINSTANCE.createIVL_TS();
        IVXB_TS highTime = DatatypesFactory.eINSTANCE.createIVXB_TS();
        highTime.setValue("200130311");
        IVXB_TS lowTime = DatatypesFactory.eINSTANCE.createIVXB_TS();
        lowTime.setValue("200120311");
        phase.setHigh(highTime);
        phase.setLow(lowTime);
        periodicInterval.setPhase(phase);
        Timing timing = simpleCDATypesConverter.convertPIVL_TStoFHIRTiming(periodicInterval);
        assertNotNull(timing);
        assertNotNull(timing.getRepeat());
        assertNotNull(timing.getRepeat().getBounds());
        assertTrue(timing.getRepeat().getBounds() instanceof Period);
    }



    private CDAtoFHIRCodes getTestCodes() {
        try {
            String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json")).getPath();
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(decodedPath);
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, CDAtoFHIRCodes.class);
        } catch (Exception e) {
            return null;
        }
    }

    private SystemNamesMapping getSystems() {
        try {
            String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("OIDtoURL.json")).getPath();
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(decodedPath);
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, SystemNamesMapping.class);
        } catch (Exception e) {
            return null;
        }
    }



}