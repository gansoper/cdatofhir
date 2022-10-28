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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CDASocialHistorySectionConverterTest {


    @Test
    public void testSmokingStatus() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/SocialHistory/SocialHistorySmokingStatus.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
        assertTrue(observation.hasCategory());
        assertEquals("social-history", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("72166-2", observation.getCode().getCodingFirstRep().getCode());
        assertTrue(observation.hasValueCodeableConcept());
        assertEquals("8517006", observation.getValueCodeableConcept().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
        assertTrue(observation.hasEffective());
        assertTrue(observation.getEffective() instanceof DateTimeType);
        assertEquals("2012-09-10", ((DateTimeType) observation.getEffective()).getValueAsString());
    }


    @Test
    public void testTobaccoUse() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/SocialHistory/SocialHistoryTobaccoUse.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
        assertTrue(observation.hasCategory());
        assertEquals("social-history", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("11367-0", observation.getCode().getCodingFirstRep().getCode());
        assertTrue(observation.hasValueCodeableConcept());
        assertEquals("160604004", observation.getValueCodeableConcept().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
        assertTrue(observation.hasEffectivePeriod());
        assertEquals("2009-02-14", observation.getEffectivePeriod().getStartElement().getValueAsString());
        assertEquals("2011-02-15", observation.getEffectivePeriod().getEndElement().getValueAsString());
    }

    @Test
    public void testSH() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/SocialHistory/SocialHistorySH.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
        assertTrue(observation.hasCategory());
        assertEquals("social-history", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("74013-4", observation.getCode().getCodingFirstRep().getCode());
        assertTrue(observation.hasValueQuantity());
        assertEquals(12, observation.getValueQuantity().getValue().intValue());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
        assertTrue(observation.hasEffectivePeriod());
        assertEquals("2012-02-15", observation.getEffectivePeriod().getStartElement().getValueAsString());
    }


    @Test
    public void testPregnancy() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/SocialHistory/SocialHistoryPregnancy.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
        assertTrue(observation.hasCategory());
        assertEquals("exam", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("10163-4", observation.getCode().getCodingFirstRep().getCode());
        assertTrue(observation.hasValueCodeableConcept());
        assertEquals("77386006", observation.getValueCodeableConcept().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNull(practResource);
        assertTrue(observation.hasEffectivePeriod());
        assertEquals("2011-04-10", observation.getEffectivePeriod().getStartElement().getValueAsString());
        assertFalse(observation.hasHasMember());
    }


    @Test
    public void testPregnancyEDOD() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/SocialHistory/SocialHistoryPregnancyEDOD.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation && !((Observation) r).hasValueDateTimeType()).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasHasMember());
        Reference reference  = observation.getHasMemberFirstRep();
        Resource edodResource = resources.values().stream().filter(r -> r instanceof Observation && r.getId() != observation.getId()).findFirst().orElse(null);
        assertNotNull(edodResource);
        assertTrue(reference.getReference().contains(edodResource.getId()));
        Observation edodObservation = (Observation) edodResource;
        assertTrue(edodObservation.hasCode());
        assertEquals("11778-8", edodObservation.getCode().getCodingFirstRep().getCode());
        assertTrue(edodObservation.hasValueDateTimeType());
        assertEquals("2011-09-19", edodObservation.getValueDateTimeType().getValueAsString());
    }

    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDASocialHistorySectionConverter shSectionConverter = new CDASocialHistorySectionConverter(basicCDAElementsConverter);
            return shSectionConverter.convertSocialHistory(((ContinuityOfCareDocument2) cda).getSocialHistorySection2(), new HashMap<>());
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