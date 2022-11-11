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

class CDAEncounterSectionConverterTest {

    @Test
    public void testEncounterCode() throws Exception {
        Resource resource = this.getEncounter("Tests/Encounter/EncounterCode.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof Encounter);
        Encounter encounter = (Encounter) resource;
        assertTrue(encounter.hasClass_() && encounter.getClass_().hasCode());
        assertEquals("99213", encounter.getClass_().getCode());
    }

    @Test
    public void testEncounterET() throws Exception {
        Resource resource = this.getEncounter("Tests/Encounter/EncounterEffectiveTime.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof Encounter);
        Encounter encounter = (Encounter) resource;
        assertTrue(encounter.hasPeriod() && encounter.getPeriod().hasStart());
        assertEquals("2012-09-27T13:00:00+05:00", encounter.getPeriod().getStartElement().getValueAsString());
    }

    @Test
    public void testEncounterPerformer() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Encounter/EncounterPerformer.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 3);
        Resource eResource = resources.values().stream().filter(resource -> resource instanceof Encounter).findAny().orElse(null);
        Resource practResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(eResource);
        assertNotNull(practResource);
        Encounter encounter = (Encounter) eResource;
        assertTrue(encounter.hasParticipant());
        assertTrue(encounter.getParticipantFirstRep().getIndividual().getReference().contains("Practitioner/"));
        assertTrue(encounter.getParticipantFirstRep().getIndividual().getReference().contains(practResource.getId()));

    }

    @Test
    public void testEncounterLocation() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Encounter/EncounterLocation.xml");
        assertNotNull(resources);
        assertEquals(2, resources.size());
        Resource eResource = resources.values().stream().filter(resource -> resource instanceof Encounter).findAny().orElse(null);
        Resource lResource = resources.values().stream().filter(resource -> resource instanceof Location).findAny().orElse(null);
        assertNotNull(eResource);
        assertNotNull(lResource);
        Encounter encounter = (Encounter) eResource;
        assertTrue(encounter.hasLocation());
        assertTrue(encounter.getLocationFirstRep().getLocation().getReference().contains("Location/"));
        assertTrue(encounter.getLocationFirstRep().getLocation().getReference().contains(lResource.getId()));
        Location location = (Location) lResource;
        assertTrue(location.hasAddress());
        assertTrue(location.hasTelecom());
        assertTrue(location.hasName());
        assertEquals("Good Health Urgent Care", location.getName());
    }

    @Test
    public void testEncounterDiagosis() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Encounter/EncounterDiagnosis.xml");
        assertNotNull(resources);
        assertEquals(3, resources.size());
        Resource eResource = resources.values().stream().filter(resource -> resource instanceof Encounter).findAny().orElse(null);
        Resource cResource = resources.values().stream().filter(resource -> resource instanceof Condition).findAny().orElse(null);
        assertNotNull(eResource);
        assertNotNull(cResource);
        Encounter encounter = (Encounter) eResource;
        assertTrue(encounter.hasDiagnosis());
        assertTrue(encounter.getDiagnosisFirstRep().getCondition().getReference().contains("Condition/"));
        assertTrue(encounter.getDiagnosisFirstRep().getCondition().getReference().contains(cResource.getId()));
    }


    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAEncountersSectionConverter encountersSectionConverter = new CDAEncountersSectionConverter(codeMappingProcessor);
            return encountersSectionConverter.convertEncounters(((ContinuityOfCareDocument2) cda).getEncountersSectionEntriesOptional2(), new HashMap<>());

        }

        return null;
    }


    private Resource getEncounter(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAEncountersSectionConverter encountersSectionConverter = new CDAEncountersSectionConverter(codeMappingProcessor);
            Map<String, Resource> resources = encountersSectionConverter.convertEncounters(((ContinuityOfCareDocument2) cda).getEncountersSectionEntriesOptional2(), new HashMap<>());
            return resources.values().stream().filter(resource -> resource instanceof Encounter).findAny().orElse(null);
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