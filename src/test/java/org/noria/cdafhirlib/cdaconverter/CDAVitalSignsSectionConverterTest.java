package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
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

class CDAVitalSignsSectionConverterTest {


    @Test
    public void testObservationCategory() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasCategory());
        assertTrue(observation.getCategoryFirstRep().hasCoding());
        assertEquals("vital-signs", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
    }


    @Test
    public void testObservationCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("8302-2", observation.getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void testObservationStatusCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsStatusCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
    }


    @Test
    public void testObservationEffectiveTime() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsEffectiveTime.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasEffective());
        assertTrue(observation.getEffective() instanceof DateTimeType);
        assertEquals("2012-09-10", ((DateTimeType) observation.getEffective()).getValueAsString());
    }


    @Test
    public void testObservationValue() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsValue.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasValueQuantity());
        assertEquals(177, observation.getValueQuantity().getValue().doubleValue());
        assertEquals("cm", observation.getValueQuantity().getUnit());
    }

    @Test
    public void testObservationInterpretationCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsInterpretationCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertFalse(observation.getInterpretation().isEmpty());
        assertFalse(observation.getInterpretationFirstRep().getCoding().isEmpty());
        assertEquals("N", observation.getInterpretationFirstRep().getCodingFirstRep().getCode());
    }


    @Test
    public void testObservationAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsAuthor.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(obResource);
        assertNotNull(practResource);
        Observation observation = (Observation) obResource;
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
    }


    @Test
    public void testObservationNoAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsNoAuthor.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(obResource);
        assertNotNull(practResource);
        Observation observation = (Observation) obResource;
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
    }

    @Test
    public void testObservationNoValue() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/VitalSigns/VitalSignsAuthor.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertFalse(observation.hasValueQuantity());
    }


    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAVitalSignsSectionConverter vsSectionConverter = new CDAVitalSignsSectionConverter(codeMappingProcessor);
            return vsSectionConverter.convertVitalSigns(((ContinuityOfCareDocument2) cda).getVitalSignsSection2(), new HashMap<>());
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