package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.*;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
class BasicCDAElementsConverterTest {


    @Test
    public void testAuthorNoOrganization() throws Exception {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Author/Author1.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author = cda.getAuthors().get(0);
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertAuthor(author);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertAuthor(author);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertPerformer(performer, null);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertPerformer(performer, null);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertPerformer(performer, null);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertPatient(patientRole);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertPatient(patientRole);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertCustodian(custodian);
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
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, Resource> resources = basicCDAElementsConverter.convertParticipant(participant);
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