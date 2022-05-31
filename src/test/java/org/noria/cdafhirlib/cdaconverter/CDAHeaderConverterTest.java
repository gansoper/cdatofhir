package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.Custodian;
import org.eclipse.mdht.uml.cda.Participant1;
import org.eclipse.mdht.uml.cda.PatientRole;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.helper.CDAPrimitiveTypesConverter;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CDAHeaderConverterTest {

    @Test
    public void testPatientBaseConversion() throws Exception {
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Patient/Patient1.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        PatientRole patientRole = cda.getPatientRoles().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        CDAHeaderConverter cdaHeaderConverter = new CDAHeaderConverter(basicCDATypesConverter);
        Map<String, IBaseResource> resources = cdaHeaderConverter.convertPatient(patientRole);
        assertEquals(resources.size(), 2);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Patient).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);

        resources.forEach((key, value) -> {
            if (value instanceof Patient) {
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
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Patient/Patient2.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        PatientRole patientRole = cda.getPatientRoles().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        CDAHeaderConverter cdaHeaderConverter = new CDAHeaderConverter(basicCDATypesConverter);
        Map<String, IBaseResource> resources = cdaHeaderConverter.convertPatient(patientRole);
        assertEquals(resources.size(), 1);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Patient).findAny().orElse(null), null);
        assertEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);

        resources.forEach((key, value) -> {
            if (value instanceof Patient) {
                Patient patient = (Patient) value;
                assertNull(patient.getGender());
            }

        });


    }

    @Test
    public void testCustodian() throws Exception {
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Custodian/Custodian1.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Custodian custodian = cda.getCustodian();
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        CDAHeaderConverter cdaHeaderConverter = new CDAHeaderConverter(basicCDATypesConverter);
        Map<String, IBaseResource> resources = cdaHeaderConverter.convertCustodian(custodian);
        assertEquals(resources.size(), 1);
        resources.forEach((key, value) -> {
            if (value instanceof Organization) {
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
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Participant/Participant1.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Participant1 participant1 = cda.getParticipants().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        CDAHeaderConverter cdaHeaderConverter = new CDAHeaderConverter(basicCDATypesConverter);
        /*Map<String, IBaseResource> resources = cdaHeaderConverter.convertCustodian(custodian);
        assertEquals(resources.size(), 1);
        resources.forEach((key, value) -> {
            if (value instanceof Organization) {
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

        });*/

    }

    @Test
    public void testDate(){
        CDAPrimitiveTypesConverter ecdaConverterUtils = new CDAPrimitiveTypesConverter();
        String date = ecdaConverterUtils.convertCDAToXmlDate("201308011235-0800");
        assertEquals(date, "2013");
    }

    private CDAtoFHIRCodes getTestCodes() {
        try {
            File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json")).getFile());
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, CDAtoFHIRCodes.class);
        } catch (Exception e) {
            return null;
        }
    }


    private SystemNamesMapping getSystems() {
        try {
            File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("OIDtoURL.json")).getFile());
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, SystemNamesMapping.class);
        } catch (Exception e) {
            return null;
        }
    }
}