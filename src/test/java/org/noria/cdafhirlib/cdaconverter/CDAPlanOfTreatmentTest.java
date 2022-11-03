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

class CDAPlanOfTreatmentTest {


    @Test
    public void testProcedure() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/PlanOfTreatment/PlanOfTreatment_Procedure.xml");
        assertFalse(resources.isEmpty());
        Resource pResource = resources.values().stream().filter(r -> r instanceof Procedure).findFirst().orElse(null);
        assertNotNull(pResource);
        Procedure procedure = (Procedure) pResource;
        assertTrue(procedure.hasStatus());
        assertEquals("preparation", procedure.getStatus().toCode());
        assertTrue(procedure.hasCode());
        assertFalse(procedure.getCode().getCoding().isEmpty());
        assertEquals("73761001", procedure.getCode().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertNotNull(procedure.getRecorder());
        assertEquals("Practitioner/" + practResource.getId(), procedure.getRecorder().getReference());
        assertTrue(procedure.hasPerformedDateTimeType());
        assertEquals("2013-06-13", (procedure.getPerformedDateTimeType()).getValueAsString());
    }


    @Test
    public void testObservation() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/PlanOfTreatment/PlanOfTreatment_Observation.xml");
        assertNotNull(resources);
        Resource obResource = resources.values().stream().filter(r -> r instanceof Observation).findFirst().orElse(null);
        assertNotNull(obResource);
        Observation observation = (Observation) obResource;
        assertTrue(observation.hasStatus());
        assertEquals("preliminary", observation.getStatus().toCode());
        assertTrue(observation.hasCategory());
        assertEquals("activity", observation.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertTrue(observation.hasCode());
        assertFalse(observation.getCode().getCoding().isEmpty());
        assertEquals("59408-5", observation.getCode().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertFalse(observation.getPerformer().isEmpty());
        assertEquals("Practitioner/" + practResource.getId(), observation.getPerformerFirstRep().getReference());
        assertTrue(observation.hasEffectiveDateTimeType());
        assertEquals("2013-09-03", observation.getEffectiveDateTimeType().getValueAsString());
    }


    @Test
    public void testMedicationStatement() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/PlanOfTreatment/PlanOfTreatment_SubstanceAdministration.xml");
        assertNotNull(resources);
        Resource msResource = resources.values().stream().filter(r -> r instanceof MedicationStatement).findFirst().orElse(null);
        assertNotNull(msResource);
        MedicationStatement medicationStatement = (MedicationStatement) msResource;
        assertTrue(medicationStatement.hasStatus());
        assertEquals("intended", medicationStatement.getStatus().toCode());
        assertTrue(medicationStatement.hasMedicationCodeableConcept());
        assertEquals("745679", medicationStatement.getMedicationCodeableConcept().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertNotNull(medicationStatement.getInformationSource());
        assertEquals("Practitioner/" + practResource.getId(), medicationStatement.getInformationSource().getReference());
        assertTrue(medicationStatement.hasEffectiveDateTimeType());
        assertEquals("2013-09-05", medicationStatement.getEffectiveDateTimeType().getValueAsString());
    }

    @Test
    public void testCarePlan() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/PlanOfTreatment/PlanOfTreatment_Act.xml");
        assertNotNull(resources);
        Resource cpResource = resources.values().stream().filter(r -> r instanceof CarePlan).findFirst().orElse(null);
        assertNotNull(cpResource);
        CarePlan carePlan = (CarePlan) cpResource;
        assertTrue(carePlan.hasStatus());
        assertEquals("active", carePlan.getStatus().toCode());
        assertTrue(carePlan.hasCategory());
        assertEquals("423171007", carePlan.getCategoryFirstRep().getCodingFirstRep().getCode());
        Resource practResource = resources.values().stream().filter(r -> r instanceof Practitioner).findFirst().orElse(null);
        assertNotNull(practResource);
        assertNotNull(carePlan.getAuthor());
        assertEquals("Practitioner/" + practResource.getId(), carePlan.getAuthor().getReference());
        assertTrue(carePlan.hasPeriod());
        assertEquals("2013-09-02", carePlan.getPeriod().getStartElement().getValueAsString());
    }

    @Test
    public void testCarePlanReferences() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/PlanOfTreatment/PlanOfTreatment_Full.xml");
        assertNotNull(resources);
        assertEquals(5, resources.size());
        Resource cpResource = resources.values().stream().filter(r -> r instanceof CarePlan).findFirst().orElse(null);
        assertNotNull(cpResource);
        CarePlan carePlan = (CarePlan) cpResource;
        assertTrue(carePlan.hasActivity());
        assertEquals(3, carePlan.getActivity().size());
        assertTrue(carePlan.getActivity().stream().anyMatch(a -> a.getReference().getReference().contains("Procedure")));
        assertTrue(carePlan.getActivity().stream().anyMatch(a -> a.getReference().getReference().contains("Observation")));
        assertTrue(carePlan.getActivity().stream().anyMatch(a -> a.getReference().getReference().contains("MedicationStatement")));
    }

    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAPlanOfTreatmentSectionConverter potSectionConverter = new CDAPlanOfTreatmentSectionConverter(codeMappingProcessor);
            return potSectionConverter.convertPlanOfTreatment(((ContinuityOfCareDocument2) cda).getPlanOfTreatmentSection2(), new HashMap<>());
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