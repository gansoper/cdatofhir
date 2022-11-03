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

class CDAFunctionalStatusSectionConverterTest {




    @Test
    public void testObservation() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/FunctionalStatus/FunctionalStatus_Observation.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasClinicalStatus());
        assertEquals("resolved", condition.getClinicalStatus().getCodingFirstRep().getCode());
        assertTrue(condition.hasCode());
        assertFalse(condition.getCode().getCoding().isEmpty());
        assertEquals("165245003", condition.getCode().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertNotNull(condition.getRecorder());
        assertEquals("Practitioner/" + practResource.getId(), condition.getRecorder().getReference());
        assertTrue(condition.hasOnsetDateTimeType());
        assertEquals("2013-03-11", condition.getOnsetDateTimeType().getValueAsString());
        assertFalse(condition.hasRecordedDate());
    }


    @Test
    public void testOrganizer() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/FunctionalStatus/FunctionalStatus_Organizer.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasClinicalStatus());
        assertEquals("resolved", condition.getClinicalStatus().getCodingFirstRep().getCode());
        assertTrue(condition.hasCode());
        assertFalse(condition.getCode().getCoding().isEmpty());
        assertEquals("165245003", condition.getCode().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertNotNull(condition.getRecorder());
        assertEquals("Practitioner/" + practResource.getId(), condition.getRecorder().getReference());
        assertTrue(condition.hasOnsetDateTimeType());
        assertEquals("2013-03-11", condition.getOnsetDateTimeType().getValueAsString());
    }



    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAFunctionalStatusSectionConverter fsSectionConverter = new CDAFunctionalStatusSectionConverter(codeMappingProcessor);
            return fsSectionConverter.convertFucntionalStatusObservations(((ContinuityOfCareDocument2) cda).getFunctionalStatusSection2(), new HashMap<>());
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