package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
class BasicCDATypesConverterTest {


    @Test
    public void testAuthorNoOrganization() throws Exception {
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Author1.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author = cda.getAuthors().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(getTestCodes(), new SimpleCDATypesConverter(getTestCodes()));
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
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Author2.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author = cda.getAuthors().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(getTestCodes(), new SimpleCDATypesConverter(getTestCodes()));
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


    private CDAtoFHIRCodes getTestCodes() {
        try {
            File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json")).getFile());
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, CDAtoFHIRCodes.class);
        } catch (Exception e) {
            return null;
        }
    }

}