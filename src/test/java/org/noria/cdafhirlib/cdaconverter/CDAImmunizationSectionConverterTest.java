package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;
import org.openhealthtools.mdht.uml.cda.consol.ConsolPackage;
import org.openhealthtools.mdht.uml.cda.consol.ContinuityOfCareDocument2;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CDAImmunizationSectionConverterTest {


    @Test
    public void testStatusCode() throws Exception {
        Resource resource = this.getImmunization("Tests/Immunization/ImmunizationStatusCode.xml");
        assertNotNull(resource);
        Immunization immunization = (Immunization) resource;
        assertEquals("completed", immunization.getStatus().toCode());

    }


    @Test
    public void testDate() throws Exception {
        Resource resource = this.getImmunization("Tests/Immunization/ImmunizationDateTime.xml");
        assertNotNull(resource);
        Immunization immunization = (Immunization) resource;
        assertEquals(immunization.getOccurrenceDateTimeType().getValueAsString(), "1998-12-15");

    }


    @Test
    public void testRouteCode() throws Exception {
        Resource resource = this.getImmunization("Tests/Immunization/ImmunizationRouteCode.xml");
        assertNotNull(resource);
        Immunization immunization = (Immunization) resource;
        assertFalse(immunization.getRoute().isEmpty());
        CodeableConcept route = immunization.getRoute();
        assertNotNull(route.getCodingFirstRep().getCode());
        assertEquals(route.getCodingFirstRep().getCode(), "C28161");
        assertEquals(route.getCodingFirstRep().getSystem(), "urn:oid:2.16.840.1.113883.3.26.1.1");
    }

    @Test
    public void testSiteCode() throws Exception {
        Resource resource = this.getImmunization("Tests/Immunization/ImmunizationSiteCode.xml");
        assertNotNull(resource);
        Immunization immunization = (Immunization) resource;
        assertFalse(immunization.getSite().isEmpty());
        CodeableConcept site = immunization.getSite();
        assertNotNull(site.getCodingFirstRep().getCode());
        assertEquals(site.getCodingFirstRep().getCode(), "10013000");
        assertEquals(site.getCodingFirstRep().getSystem(), "http://snomed.info/sct");
    }

    @Test
    public void testDoseQuantity() throws Exception {
        Resource resource = this.getImmunization("Tests/Immunization/ImmunizationDosage.xml");
        assertNotNull(resource);
        Immunization immunization = (Immunization) resource;
        assertFalse(immunization.getDoseQuantity().isEmpty());
        assertEquals(immunization.getDoseQuantity().getUnit(), "ug");
        assertEquals(immunization.getDoseQuantity().getValue(), new BigDecimal(50));
    }


    @Test
    public void testImmunizationConsumable() throws Exception {
        Resource resource = this.getImmunization("Tests/Immunization/ImmunizationVaccineCode.xml");
        assertNotNull(resource);
        Immunization immunization = (Immunization) resource;
        assertFalse(immunization.getVaccineCode().isEmpty());
        assertNotNull(immunization.getVaccineCode().getCodingFirstRep().getCode());
        assertEquals(immunization.getVaccineCode().getCodingFirstRep().getCode(), "33");
        assertEquals(immunization.getVaccineCode().getCodingFirstRep().getSystem(), "urn:oid:2.16.840.1.113883.6.59");
    }


    @Test
    public void testImmunizationPerformer() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Immunization/ImmunizationPerformer.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 5);
        Resource immunizationResource = resources.values().stream().filter(resource -> resource instanceof Immunization).findAny().orElse(null);
        Resource practResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(immunizationResource);
        assertNotNull(practResource);
        Immunization immunization = (Immunization) immunizationResource;
        assertFalse(immunization.getPerformer().isEmpty());
        assertNotNull(immunization.getPerformerFirstRep().getActor());
        assertTrue(immunization.getPerformerFirstRep().getActor().getReference().contains("Practitioner/"));
        assertTrue(immunization.getPerformerFirstRep().getActor().getReference().contains(practResource.getId()));

    }

    @Test
    public void testImmunizationReaction() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Immunization/ImmunizationReaction.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 2);
        Resource immunizationResource = resources.values().stream().filter(resource -> resource instanceof Immunization).findAny().orElse(null);
        Resource obsResource = resources.values().stream().filter(resource -> resource instanceof Observation).findAny().orElse(null);
        assertNotNull(immunizationResource);
        assertNotNull(obsResource);
        Immunization immunization = (Immunization) immunizationResource;
        assertFalse(immunization.getReaction().isEmpty());
        assertNotNull(immunization.getReactionFirstRep().getDetail());
        assertTrue(immunization.getReactionFirstRep().getDetail().getReference().contains("Observation/"));
        assertTrue(immunization.getReactionFirstRep().getDetail().getReference().contains(obsResource.getId()));

        Observation observation = (Observation) obsResource;
        assertNotNull(observation.getEffective());
        assertTrue(observation.getEffective() instanceof Period);

        assertNotNull(observation.getStatus());
        assertNotNull(observation.getValueCodeableConcept());
        assertFalse(observation.getValueCodeableConcept().isEmpty());
        assertNotNull(observation.getValueCodeableConcept().getCodingFirstRep());
        assertEquals("422587007", observation.getValueCodeableConcept().getCodingFirstRep().getCode());
    }


    private Resource getImmunization(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAImmunizationsSectionConverter immunizationsSectionConverter = new CDAImmunizationsSectionConverter(basicCDAElementsConverter);
            Map<String, Resource> resources = immunizationsSectionConverter.convertImmunizations(((ContinuityOfCareDocument2) cda).getImmunizationsSection2(), new HashMap<>());
            return resources.values().stream().filter(resource -> resource instanceof Immunization).findAny().orElse(null);
        }
        return null;
    }


    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAImmunizationsSectionConverter immunizationsSectionConverter = new CDAImmunizationsSectionConverter(basicCDAElementsConverter);
            return immunizationsSectionConverter.convertImmunizations(((ContinuityOfCareDocument2) cda).getImmunizationsSection2(), new HashMap<>());
        }

        return null;
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