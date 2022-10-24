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

class CDAProblemSectionConverterTest {

    @Test
    public void testProblemAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/Problems_Author.xml");
        assertNotNull(resources);
        Resource condResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(condResource);
        assertNotNull(practResource);
        Condition condition = (Condition) condResource;
        assertFalse(condition.getRecorder().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), condition.getRecorder().getReference());
    }

    @Test
    public void testProblemNoAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/Problems_NoAuthor.xml");
        assertNotNull(resources);
        Resource condResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(condResource);
        assertNotNull(practResource);
        Condition condition = (Condition) condResource;
        assertFalse(condition.getRecorder().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), condition.getRecorder().getReference());
    }




    @Test
    public void testProblemStatusCodeNoObservation() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/ProblemsStatusCode.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasClinicalStatus());
        assertFalse(condition.getClinicalStatus().getCoding().isEmpty());
        assertEquals("resolved", condition.getClinicalStatus().getCodingFirstRep().getCode());
    }


    // Test failed due to inability of mdht library to detect the Problem Status Observation. For now it is commented.
    // TODO:revise after new mdht lib version
    @Test
    public void testProblemStatusCodeObservation() throws Exception {
       /*Map<String, Resource> resources = this.getAllResources("Tests/Problems/ProblemsStatusCodeObservation.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasClinicalStatus());
        assertFalse(condition.getClinicalStatus().getCoding().isEmpty());
        assertEquals("resolved", condition.getClinicalStatus().getCodingFirstRep().getCode());*/
    }


    @Test
    public void testProblemRecordedDate() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/ProblemsEffectiveTime.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasRecordedDate());
        assertEquals("2013-07-06T11:45:00-08:00", condition.getRecordedDateElement().getValueAsString());
    }

    @Test
    public void testProblemEffectiveTime() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/ProblemsEffectiveTime.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasOnsetPeriod());
        assertEquals("2013-07-03", condition.getOnsetPeriod().getStartElement().getValueAsString());
        assertEquals("2008-08-14", condition.getOnsetPeriod().getEndElement().getValueAsString());
        assertTrue(condition.hasAbatement());
        assertEquals("2008-08-14", condition.getAbatementDateTimeType().getValueAsString());
    }

    @Test
    public void testProblemCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/ProblemsCode.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasCategory());
        assertTrue(condition.getCategoryFirstRep().hasCoding());
        assertEquals("problem-list-item", condition.getCategoryFirstRep().getCodingFirstRep().getCode());
    }


    @Test
    public void testProblemValue() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/ProblemsValue.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasCode());
        assertTrue(condition.getCode().hasCoding());
        assertEquals("233604007", condition.getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void testProblemAge() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Problems/ProblemsAge.xml");
        assertNotNull(resources);
        Resource cResource = resources.values().stream().filter(r -> r instanceof Condition).findFirst().orElse(null);
        assertNotNull(cResource);
        Condition condition = (Condition) cResource;
        assertTrue(condition.hasOnsetAge());
        assertEquals(57, condition.getOnsetAge().getValue().doubleValue());
        assertEquals("a", condition.getOnsetAge().getUnit());
    }

    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAProblemsSectionConverter cdaProblemsSectionConverter = new CDAProblemsSectionConverter(basicCDAElementsConverter);
            return cdaProblemsSectionConverter.convertProblems(((ContinuityOfCareDocument2) cda).getProblemSection2(), new HashMap<>());
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