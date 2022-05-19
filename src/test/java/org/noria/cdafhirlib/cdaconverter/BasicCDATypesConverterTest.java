package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
class BasicCDATypesConverterTest {


    @Test
    public void testAuthorNoOrganization() throws Exception {
        File file = new File(this.getClass().getClassLoader().getResource("Tests/Author1.xml").getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        Author author  = cda.getAuthors().get(0);
        BasicCDATypesConverter basicCDATypesConverter = new BasicCDATypesConverter(getTestCodes(), new SimpleCDATypesConverter(getTestCodes()));
        Map<String, IBaseResource> resources = basicCDATypesConverter.convertAuthor(author);
        assertEquals(1, resources.size());
        resources.entrySet().stream().forEach(e->{
            assertTrue(e.getValue() instanceof Practitioner);
            Practitioner practitioner = (Practitioner) e.getValue();
            assertTrue(practitioner.getNameFirstRep().getGivenAsSingleString().equals("Patricia Patty"));
            assertTrue(practitioner.getNameFirstRep().getFamily().equals("Primary"));
            assertTrue(practitioner.getAddressFirstRep().getCity().equals("Portland"));
        });

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