package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.*;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.eclipse.mdht.uml.hl7.vocab.PostalAddressUse;
import org.eclipse.mdht.uml.hl7.vocab.TelecommunicationAddressUse;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
class CDABasicElementsConverterTest {


    @Test
    void createFHIRCodingNull() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        Coding coding = simpleCDATypesConverter.createFHIRCoding(null, null);
        assertEquals(coding, null);
    }

    @Test
    void createFHIRCodingNotInJSONEmptyCD() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, "test");
        assertNotEquals(coding, null);
        assertNull(coding.getCode());
    }

    @Test
    void createFHIRCodingNotInJSONCDValue() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, "test");
        assertNotEquals(coding, null);
        assertEquals(coding.getCode(), "test");
    }

    @Test
    void createFHIRCodingNotInJSONCDValueFilled() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("completed");
        Coding coding = simpleCDATypesConverter.createFHIRCoding(cd, CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
        assertNotEquals(coding, null);
        assertEquals(coding.getCode(), "final");
    }


    @Test
    void codeabelConceptNull() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        CodeableConcept codeableConcept = simpleCDATypesConverter.createFHIRCodeableConcept(null, null);
        assertEquals(codeableConcept, null);

    }

    @Test
    void codeabelConceptNotNull() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(null, getSystems()));
        CD cd = DatatypesFactory.eINSTANCE.createCD();
        cd.setCode("test");
        CodeableConcept codeableConcept = simpleCDATypesConverter.createFHIRCodeableConcept(cd, null);
        assertNotEquals(codeableConcept, null);
        assertEquals(codeableConcept.getCoding().size(), 1);
        assertEquals(codeableConcept.getCoding().get(0).getCode(), "test");
    }

    @Test
    void codeabelConceptNotNullFromJson() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
    void addressEmptyConversionTest() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        AD ad = DatatypesFactory.eINSTANCE.createAD();
        Address address = simpleCDATypesConverter.createFHIRAddress(ad);
        assertNotNull(address);
        assertNull(address.getCity());
    }

    @Test
    void addressConversionTestLinesAndUses() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
    void addressConversionTestCities() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        ContactPoint contactPoint = simpleCDATypesConverter.createContactPoint(null);
        assertNotNull(contactPoint);
        assertNull(contactPoint.getUse());
    }

    @Test
    void createContactPointFilled() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        TEL telecom = DatatypesFactory.eINSTANCE.createTEL();
        telecom.getUses().add(TelecommunicationAddressUse.HP);
        telecom.setValue("test");
        ContactPoint contactPoint = simpleCDATypesConverter.createContactPoint(telecom);
        assertNotNull(contactPoint);
        assertNotNull(contactPoint.getUse());
        assertEquals(contactPoint.getUse(), ContactPoint.ContactPointUse.HOME);
        assertEquals(contactPoint.getValue(), "test");
    }

    @Test
    void convertEIVL_TStoFHIRTimingNull() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        EIVL_TS eventInterval = null;
        Timing timing = simpleCDATypesConverter.convertEIVL_TStoFHIRTiming(eventInterval);
        assertNull(timing);

    }

    @Test
    void convertEIVL_TStoFHIRTimingNotNull() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        PIVL_TS periodicInterval = null;
        Timing timing = simpleCDATypesConverter.convertPIVL_TStoFHIRTiming(periodicInterval);
        assertNull(timing);
    }


    @Test
    void convertPIVL_TStoFHIRTimingNotNullPeriod() {
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
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
        CDABasicElementsConverter simpleCDATypesConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.getTestCodes(), getSystems()));
        PIVL_TS periodicInterval = DatatypesFactory.eINSTANCE.createPIVL_TS();
        periodicInterval.setValue("20130311");
        IVL_TS phase = DatatypesFactory.eINSTANCE.createIVL_TS();
        IVXB_TS highTime = DatatypesFactory.eINSTANCE.createIVXB_TS();
        highTime.setValue("20130311");
        IVXB_TS lowTime = DatatypesFactory.eINSTANCE.createIVXB_TS();
        lowTime.setValue("20120311");
        phase.setHigh(highTime);
        phase.setLow(lowTime);
        periodicInterval.setPhase(phase);
        Timing timing = simpleCDATypesConverter.convertPIVL_TStoFHIRTiming(periodicInterval);
        assertNotNull(timing);
        assertNotNull(timing.getRepeat());
        assertNotNull(timing.getRepeat().getBounds());
        assertTrue(timing.getRepeat().getBounds() instanceof Period);
    }


    @Test
    public void testAuthorNoOrganization() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Author/Author1.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author = cda.getAuthors().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertAuthor(author);
        assertEquals(1, resources.size());
        resources.forEach((key, value) -> {
            assertTrue(key.contains("Practitioner"));
            assertTrue(value instanceof Practitioner);
            Practitioner practitioner = (Practitioner) value;
            assertEquals(practitioner.getNameFirstRep().getGivenAsSingleString(), "Patricia Patty");
            assertEquals(practitioner.getNameFirstRep().getFamily(), "Primary");
            assertEquals(practitioner.getAddressFirstRep().getCity(), "Portland");
            assertEquals(practitioner.getIdentifierFirstRep().getValue(), "5555555555");
            assertEquals(practitioner.getId(), "Practitioner_5555555555");
        });

    }

    @Test
    public void testAuthorOrganization() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Author/Author2.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author = cda.getAuthors().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertAuthor(author);
        assertEquals(3, resources.size());
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);
        resources.forEach((key, value) -> {
            if (value instanceof Organization) {
                assertTrue(key.contains("Organization"));
                Organization org = (Organization) value;
                assertEquals(org.getName(), "Good Health Insurance");
                assertEquals(org.getAddressFirstRep().getCity(), "Blue Bell");
                assertEquals(org.getTelecomFirstRep().getValue(), "tel:+(555)555-1515");

            }
        });

    }

    @Test
    public void testPerformerOnlyFC() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Performer/Performer0.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Performer1 performer = cda.getDocumentationOfs().get(0).getServiceEvent().getPerformers().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertPerformer(performer, null);
        assertEquals(resources.size(), 2);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Practitioner).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof PractitionerRole).findAny().orElse(null), null);
        assertNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Location).findAny().orElse(null));
        assertNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null));
        resources.forEach((key, value) -> {
            if (value instanceof PractitionerRole) {
                assertTrue(key.contains("PractitionerRole"));
                PractitionerRole practitionerRole = (PractitionerRole) value;
                assertNotNull(practitionerRole.getPractitioner().getReference());
                assertTrue(CollectionUtils.isNotEmpty(practitionerRole.getSpecialty()));
                assertTrue(CollectionUtils.isNotEmpty(practitionerRole.getSpecialtyFirstRep().getCoding()));
                assertEquals(practitionerRole.getSpecialtyFirstRep().getCodingFirstRep().getCode(), "PCP");
                assertEquals(practitionerRole.getSpecialtyFirstRep().getCodingFirstRep().getSystem(), "urn:oid:2.16.840.1.113883.5.88");
            }
        });
    }

    @Test
    public void testPerformerWithoutFC() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Performer/Performer1.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Performer1 performer = cda.getDocumentationOfs().get(0).getServiceEvent().getPerformers().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertPerformer(performer, null);
        assertEquals(resources.size(), 4);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Practitioner).findAny().orElse(null), null);
        assertNotNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof PractitionerRole).findAny().orElse(null));
        assertNotNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Location).findAny().orElse(null));
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);
        resources.forEach((key, value) -> {
            if (value instanceof Practitioner) {
                assertTrue(key.contains("Practitioner"));
                Practitioner practitioner = (Practitioner) value;
                assertTrue(CollectionUtils.isNotEmpty(practitioner.getIdentifier()));
                assertTrue(CollectionUtils.isNotEmpty(practitioner.getTelecom()));
                assertTrue(CollectionUtils.isNotEmpty(practitioner.getAddress()));
                assertEquals(practitioner.getId(), "Practitioner_5555555555");
                assertEquals(practitioner.getIdentifierFirstRep().getValue(), "5555555555");
                assertEquals(practitioner.getTelecomFirstRep().getValue(), "tel:+1(555)555-1004");
                assertEquals(practitioner.getAddressFirstRep().getCity(), "Portland");
            }

            if (value instanceof Organization) {
                assertTrue(key.contains("Organization"));
                Organization organization = (Organization) value;
                assertTrue(CollectionUtils.isNotEmpty(organization.getIdentifier()));
                assertTrue(CollectionUtils.isNotEmpty(organization.getTelecom()));
                assertTrue(CollectionUtils.isNotEmpty(organization.getAddress()));
                assertEquals(organization.getId(), "Organization_219BX");
                assertEquals(organization.getIdentifierFirstRep().getValue(), "219BX");
                assertEquals(organization.getTelecomFirstRep().getValue(), "tel: +1(555)555-5000");
                assertEquals(organization.getAddressFirstRep().getCity(), "Portland");
                assertEquals(organization.getName(), "The DoctorsTogether Physician Group");
            }

            if (value instanceof PractitionerRole) {
                assertTrue(key.contains("PractitionerRole"));
                PractitionerRole practitionerRole = (PractitionerRole) value;
                assertNotNull(practitionerRole.getPractitioner().getReference());
                assertTrue(CollectionUtils.isEmpty(practitionerRole.getSpecialty()));
            }

        });
    }


    @Test
    public void testPerformerAllItems() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Performer/Performer2.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Performer1 performer = cda.getDocumentationOfs().get(0).getServiceEvent().getPerformers().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertPerformer(performer, null);
        assertEquals(resources.size(), 4);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Practitioner).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof PractitionerRole).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Location).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);
        resources.forEach((key, value) -> {

            if (value instanceof PractitionerRole) {
                assertTrue(key.contains("PractitionerRole"));
                PractitionerRole practitionerRole = (PractitionerRole) value;
                assertTrue(CollectionUtils.isNotEmpty(practitionerRole.getLocation()));
                assertNotNull(practitionerRole.getOrganization());
                assertNotNull(practitionerRole.getPractitioner());
            }
            if (value instanceof Location) {
                assertTrue(key.contains("Location"));
                Location location = (Location) value;
                assertNotNull(location.getAddress());
                assertEquals(location.getAddress().getCity(), "Portland");
            }

        });
    }


    @Test
    public void testPatientBaseConversion() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Patient/Patient1.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        PatientRole patientRole = cda.getPatientRoles().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertPatient(patientRole);
        assertEquals(resources.size(), 2);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Patient).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);

        resources.forEach((key, value) -> {
            if (value instanceof Patient) {
                assertTrue(key.contains("Patient"));
                Patient patient = (Patient) value;
                assertEquals(patient.getExtension().size(), 3);
                assertTrue(CollectionUtils.isNotEmpty(patient.getIdentifier()));
                assertEquals(patient.getId(), "Patient_444222222");
                assertTrue(CollectionUtils.isNotEmpty(patient.getIdentifier()));
                assertEquals(patient.getIdentifierFirstRep().getValue(), "444222222");
                assertEquals(patient.getTelecomFirstRep().getValue(), "tel:+1(555)555-2003");
                assertEquals(patient.getAddressFirstRep().getCity(), "Beaverton");
                assertEquals(patient.getNameFirstRep().getFamily(), "Betterhalf");
                assertEquals(patient.getNameFirstRep().getGivenAsSingleString(), "Eve");
                assertEquals(patient.getCommunicationFirstRep().getLanguage().getCodingFirstRep().getCode(), "en");
                assertEquals(patient.getGender(), Enumerations.AdministrativeGender.FEMALE);
                assertEquals(patient.getMaritalStatus().getCodingFirstRep().getCode(), "M");

            }

            if (value instanceof Organization) {
                assertTrue(key.contains("Organization"));
                Organization organization = (Organization) value;
                assertTrue(CollectionUtils.isNotEmpty(organization.getIdentifier()));
                assertTrue(CollectionUtils.isNotEmpty(organization.getTelecom()));
                assertTrue(CollectionUtils.isNotEmpty(organization.getAddress()));
                assertEquals(organization.getId(), "Organization_219BX");
                assertEquals(organization.getIdentifierFirstRep().getValue(), "219BX");
                assertEquals(organization.getTelecomFirstRep().getValue(), "tel: +1(555)555-5000");
                assertEquals(organization.getAddressFirstRep().getCity(), "Portland");
                assertEquals(organization.getName(), "The DoctorsTogether Physician Group");
            }

        });


    }

    @Test
    public void testPatientWrongGenderNoOrg() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Patient/Patient2.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        PatientRole patientRole = cda.getPatientRoles().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertPatient(patientRole);
        assertEquals(resources.size(), 1);
        assertNotNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Patient).findAny().orElse(null));
        assertNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null));

        resources.forEach((key, value) -> {
            if (value instanceof Patient) {
                assertTrue(key.contains("Patient"));
                Patient patient = (Patient) value;
                assertNull(patient.getGender());
            }

        });


    }

    @Test
    public void testCustodian() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Custodian/Custodian1.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Custodian custodian = cda.getCustodian();
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertCustodian(custodian);
        assertEquals(resources.size(), 1);
        resources.forEach((key, value) -> {
            if (value instanceof Organization) {
                assertTrue(key.contains("Organization"));
                Organization organization = (Organization) value;
                assertTrue(CollectionUtils.isNotEmpty(organization.getIdentifier()));
                assertTrue(CollectionUtils.isNotEmpty(organization.getTelecom()));
                assertTrue(CollectionUtils.isNotEmpty(organization.getAddress()));
                assertEquals(organization.getId(), "Organization_321CX");
                assertEquals(organization.getIdentifierFirstRep().getValue(), "321CX");
                assertEquals(organization.getTelecomFirstRep().getValue(), "tel:+1(555)555-1009");
                assertEquals(organization.getAddressFirstRep().getCity(), "Portland");
                assertEquals(organization.getName(), "Good Health HIE");
            }

        });

    }

    @Test
    public void testParticipant() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Participant/Participant1.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Participant1 participant = cda.getParticipants().get(0);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        Map<String, Resource> resources = CDACommonElementsConverter.getInstance(CDABasicElementsConverter).convertParticipant(participant);
        assertEquals(resources.size(), 1);
        resources.forEach((key, value) -> {
            if (value instanceof Practitioner) {
                assertTrue(key.contains("Practitioner"));
                Practitioner practitioner = (Practitioner) value;
                assertTrue(CollectionUtils.isEmpty(practitioner.getIdentifier()));
                assertTrue(CollectionUtils.isNotEmpty(practitioner.getTelecom()));
                assertTrue(CollectionUtils.isNotEmpty(practitioner.getAddress()));
                assertTrue(CollectionUtils.isNotEmpty(practitioner.getName()));
                assertEquals(practitioner.getTelecomFirstRep().getValue(), "tel:+1(555)555-2008");
                assertEquals(practitioner.getAddressFirstRep().getCity(), "Beaverton");
                assertEquals(practitioner.getNameFirstRep().getFamily(), "Betterhalf");
            }

        });

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