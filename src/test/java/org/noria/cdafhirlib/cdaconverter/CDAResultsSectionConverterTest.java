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

class CDAResultsSectionConverterTest {


    @Test
    public void testOrganizerCode() throws Exception {
        Resource resource = this.getResult("Tests/Result/ResultOrganizerCode.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof DiagnosticReport);
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        assertTrue(diagnosticReport.hasCode());
        assertEquals("57021-8", diagnosticReport.getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void testOrganizerStatus() throws Exception {
        Resource resource = this.getResult("Tests/Result/ResultOrganizerStatusCode.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof DiagnosticReport);
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        assertTrue(diagnosticReport.hasStatus());
        assertEquals("final", diagnosticReport.getStatus().toCode());
    }

    @Test
    public void testOrganizerEffectiveTime() throws Exception {
        Resource resource = this.getResult("Tests/Result/ResultOrganizerEffectiveTime.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof DiagnosticReport);
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        assertTrue(diagnosticReport.hasEffectivePeriod());
        assertEquals("2008-03-19T08:30:00-08:00", diagnosticReport.getEffectivePeriod().getStartElement().getValueAsString());
        assertEquals("2008-03-19T08:30:00-08:00", diagnosticReport.getEffectivePeriod().getEndElement().getValueAsString());
    }


    @Test
    public void testObservationReference() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationCode.xml");
        assertNotNull(resources);
        Resource drResource = resources.values().stream().filter(r -> r instanceof DiagnosticReport).findFirst().orElse(null);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(drResource);
        assertNotNull(obResource);
        DiagnosticReport diagnosticReport = (DiagnosticReport) drResource;
        assertFalse(diagnosticReport.getResult().isEmpty());
        assertEquals("Observation/" + obResource.getId(), diagnosticReport.getResultFirstRep().getReference());
    }

    @Test
    public void testObservationCategory() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasCategory());
        assertTrue(observation.getCategoryFirstRep().hasCoding());
        assertEquals("exam", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
    }


    @Test
    public void testObservationCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("4544-3", observation.getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void testObservationStatusCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationStatusCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("final", observation.getStatus().toCode());
    }


    @Test
    public void testObservationEffectiveTime() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationEffectiveTime.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasEffective());
        assertTrue(observation.getEffective() instanceof DateTimeType);
        assertEquals("2008-03-19T08:30:00-08:00", ((DateTimeType) observation.getEffective()).getValueAsString());
    }


    @Test
    public void testObservationValue() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationValue.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasValueQuantity());
        assertEquals(35.3, observation.getValueQuantity().getValue().doubleValue());
        assertEquals("%", observation.getValueQuantity().getUnit());
    }

    @Test
    public void testObservationInterpretationCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationInterpretationCode.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertFalse(observation.getInterpretation().isEmpty());
        assertFalse(observation.getInterpretationFirstRep().getCoding().isEmpty());
        assertEquals("LX", observation.getInterpretationFirstRep().getCodingFirstRep().getCode());
    }


    @Test
    public void testObservationAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationAuthor.xml");
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
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationNoAuthor.xml");
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
    public void testObservationReferenceRange() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationReferenceRange.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertFalse(observation.getReferenceRange().isEmpty());
        assertTrue(observation.getReferenceRangeFirstRep().hasHigh());
        assertTrue(observation.getReferenceRangeFirstRep().hasLow());
        assertEquals(34.9, observation.getReferenceRangeFirstRep().getLow().getValue().doubleValue());
        assertEquals("%", observation.getReferenceRangeFirstRep().getLow().getUnit());
        assertEquals(44.5, observation.getReferenceRangeFirstRep().getHigh().getValue().doubleValue());
        assertEquals("%", observation.getReferenceRangeFirstRep().getHigh().getUnit());
    }


    @Test
    public void testObservationNoValue() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Result/ResultObservationNoValue.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertFalse(observation.hasValueQuantity());
    }


    private Resource getResult(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAResultsSectionConverter resultsSectionConverter = new CDAResultsSectionConverter(CDABasicElementsConverter);
            Map<String, Resource> resources = resultsSectionConverter.convertResult(((ContinuityOfCareDocument2) cda).getResultsSection2(), new HashMap<>());
            return resources.values().stream().filter(resource -> resource instanceof DiagnosticReport).findAny().orElse(null);
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
            CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAResultsSectionConverter resultsSectionConverter = new CDAResultsSectionConverter(CDABasicElementsConverter);
            return resultsSectionConverter.convertResult(((ContinuityOfCareDocument2) cda).getResultsSection2(), new HashMap<>());
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