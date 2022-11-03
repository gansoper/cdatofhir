package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;
import org.openhealthtools.mdht.uml.cda.consol.AdvanceDirectivesSection2;
import org.openhealthtools.mdht.uml.cda.consol.AdvanceDirectivesSectionEntriesOptional2;
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

class CDAAdvancedDirectivesSectionConverterTest {


    @Test
    public void testADObservation() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/AdvancedDirectives/AdvancedDirectives_Observation.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
        assertTrue(observation.hasCategory());
        assertEquals("activity", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("304251008", observation.getCode().getCodingFirstRep().getCode());
        assertTrue(observation.hasValueCodeableConcept());
        assertEquals("304253006", observation.getValueCodeableConcept().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
        assertTrue(observation.hasEffectivePeriod());
        assertEquals("2011-02-19", observation.getEffectivePeriod().getStartElement().getValueAsString());
        assertFalse(observation.getEffectivePeriod().hasEnd());
    }


    @Test
    public void testADOOrganizer() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/AdvancedDirectives/AdvancedDirectives_Organizer.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
        assertTrue(observation.hasCategory());
        assertEquals("activity", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("304251008", observation.getCode().getCodingFirstRep().getCode());
        assertTrue(observation.hasValueCodeableConcept());
        assertEquals("304253006", observation.getValueCodeableConcept().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
        assertTrue(observation.hasEffectivePeriod());
        assertEquals("2011-02-19", observation.getEffectivePeriod().getStartElement().getValueAsString());
        assertFalse(observation.getEffectivePeriod().hasEnd());
    }


    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAAdvancedDirectivesSectionConverter adSectionConverter = new CDAAdvancedDirectivesSectionConverter(codeMappingProcessor);
           return adSectionConverter.convertAdvancedDirectives(((ContinuityOfCareDocument2) cda).getAdvanceDirectivesSectionEntriesOptional2(), new HashMap<>());

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