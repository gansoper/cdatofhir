package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.Performer1;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
class BasicCDATypesConverterTest {


    @Test
    public void testAuthorNoOrganization() throws Exception {
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Author/Author1.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author = cda.getAuthors().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, IBaseResource> resources = basicCDATypesConverter.convertAuthor(author);
        assertEquals(1, resources.size());
        resources.forEach((key, value) -> {
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
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Author/Author2.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author = cda.getAuthors().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, IBaseResource> resources = basicCDATypesConverter.convertAuthor(author);
        assertEquals(3, resources.size());
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);
        resources.forEach((key, value) -> {
            if (value instanceof Organization) {
                Organization org = (Organization) value;
                assertEquals(org.getName(), "Good Health Insurance");
                assertEquals(org.getAddressFirstRep().getCity(), "Blue Bell");
                assertEquals(org.getTelecomFirstRep().getValue(), "tel:+(555)555-1515");

            }
        });

    }

    @Test
    public void testPerformerOnlyFC() throws Exception {
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Performer/Performer0.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Performer1 performer = cda.getDocumentationOfs().get(0).getServiceEvent().getPerformers().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, IBaseResource> resources = basicCDATypesConverter.convertPerformer(performer);
        assertEquals(resources.size(), 2);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Practitioner).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof PractitionerRole).findAny().orElse(null), null);
        assertNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Location).findAny().orElse(null));
        assertNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null));
        resources.forEach((key, value) -> {
            if (value instanceof PractitionerRole) {
                PractitionerRole practitionerRole = (PractitionerRole) value;
                assertTrue(CollectionUtils.isNotEmpty(practitionerRole.getSpecialty()));
                assertTrue(CollectionUtils.isNotEmpty(practitionerRole.getSpecialtyFirstRep().getCoding()));
                assertEquals(practitionerRole.getSpecialtyFirstRep().getCodingFirstRep().getCode(), "PCP");
                assertEquals(practitionerRole.getSpecialtyFirstRep().getCodingFirstRep().getSystem(), "urn:oid:2.16.840.1.113883.5.88");
            }
        });
    }

    @Test
    public void testPerformerWithoutFC() throws Exception {
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Performer/Performer1.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Performer1 performer = cda.getDocumentationOfs().get(0).getServiceEvent().getPerformers().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, IBaseResource> resources = basicCDATypesConverter.convertPerformer(performer);
        assertEquals(resources.size(), 2);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Practitioner).findAny().orElse(null), null);
        assertNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof PractitionerRole).findAny().orElse(null));
        assertNull(resources.entrySet().stream().filter(k -> k.getValue() instanceof Location).findAny().orElse(null));
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);
        resources.forEach((key, value) -> {
            if (value instanceof Practitioner) {
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
    public void testPerformerAllItems() throws Exception {
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Performer/Performer2.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Performer1 performer = cda.getDocumentationOfs().get(0).getServiceEvent().getPerformers().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
        Map<String, IBaseResource> resources = basicCDATypesConverter.convertPerformer(performer);
        assertEquals(resources.size(), 4);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Practitioner).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof PractitionerRole).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Location).findAny().orElse(null), null);
        assertNotEquals(resources.entrySet().stream().filter(k -> k.getValue() instanceof Organization).findAny().orElse(null), null);
        resources.forEach((key, value) -> {

            if (value instanceof PractitionerRole) {
                PractitionerRole practitionerRole = (PractitionerRole) value;
                assertTrue(CollectionUtils.isNotEmpty(practitionerRole.getLocation()));
                assertNotNull(practitionerRole.getOrganization());
                assertNotNull(practitionerRole.getPractitioner());
            }
            if (value instanceof Location) {
                Location location = (Location) value;
                assertNotNull(location.getAddress());
                assertEquals(location.getAddress().getCity(), "Portland");
            }

        });
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