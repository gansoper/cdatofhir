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

class CDAMedicationsSectionConverterTest {

    @Test
    public void testDate() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationDate.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertEquals(medicationRequest.getAuthoredOnElement().getValueAsString(), "2012-03-18");

    }

    @Test
    public void testEIVL_TS() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationEIVL.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertFalse(medicationRequest.getDosageInstruction().isEmpty());
        Timing timing = medicationRequest.getDosageInstructionFirstRep().getTiming();
        assertNotNull(timing.getCode().getCodingFirstRep().getCode());
        assertEquals(timing.getCode().getCodingFirstRep().getCode(), "AC");
        assertEquals(timing.getRepeat().getOffset(), 10);
    }

    @Test
    public void testPIVL_TS() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationPIVL.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertFalse(medicationRequest.getDosageInstruction().isEmpty());
        Timing timing = medicationRequest.getDosageInstructionFirstRep().getTiming();
        assertNotNull(timing);
        assertEquals(timing.getRepeat().getPeriod(), new BigDecimal(6));
        assertEquals(timing.getRepeat().getPeriodUnit(), Timing.UnitsOfTime.H);
    }

    @Test
    public void testRouteCode() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationRouteCode.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertFalse(medicationRequest.getDosageInstruction().isEmpty());
        CodeableConcept route = medicationRequest.getDosageInstructionFirstRep().getRoute();
        assertNotNull(route.getCodingFirstRep().getCode());
        assertEquals(route.getCodingFirstRep().getCode(), "C38216");
        assertEquals(route.getCodingFirstRep().getSystem(), "urn:oid:2.16.840.1.113883.3.26.1.1");
    }

    @Test
    public void testDoseQuantity() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationDoseSimpleQuantity.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertFalse(medicationRequest.getDosageInstruction().isEmpty());
        Type dose = medicationRequest.getDosageInstructionFirstRep().getDoseAndRateFirstRep().getDose();
        assertNotNull(dose);
        assertTrue(dose instanceof SimpleQuantity);
        SimpleQuantity sq = (SimpleQuantity) dose;
        assertEquals(sq.getUnit(), "mg");
        assertEquals(sq.getValue(), new BigDecimal(2));
    }

    @Test
    public void testDoseRangeQuantity() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationDoseRangeQuantity.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertFalse(medicationRequest.getDosageInstruction().isEmpty());
        Type dose = medicationRequest.getDosageInstructionFirstRep().getDoseAndRateFirstRep().getDose();
        assertNotNull(dose);
        assertTrue(dose instanceof Range);
        Range range = (Range) dose;
        assertEquals(range.getLow().getValue(), new BigDecimal(1));
        assertEquals(range.getHigh().getValue(), new BigDecimal(2));
    }

    @Test
    public void testMedicationConsumable() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationConsumable.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertNotNull(medicationRequest.getMedication());
        assertTrue(medicationRequest.getMedication() instanceof CodeableConcept);
        CodeableConcept medication = (CodeableConcept) medicationRequest.getMedication();
        assertNotNull(medication.getCodingFirstRep().getCode());
        assertEquals(medication.getCodingFirstRep().getCode(), "573621");
        assertEquals(medication.getCodingFirstRep().getSystem(), "http://www.nlm.nih.gov/research/umls/rxnorm");
    }


    @Test
    public void testRepeatNumber() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationRepeatNumber.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertFalse(medicationRequest.getDosageInstruction().isEmpty());
        Timing timing = medicationRequest.getDosageInstructionFirstRep().getTiming();
        assertNotNull(timing);
        assertEquals(timing.getRepeat().getCount(), 1);

    }

    @Test
    public void testApproachSiteCode() throws Exception {
        Resource resource = this.getMedicationRequest("Tests/Medication/MedicationApproachSiteCode.xml");
        assertNotNull(resource);
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        assertNotNull(medicationRequest.getDosageInstructionFirstRep().getSite().getCodingFirstRep().getCode());
        assertEquals(medicationRequest.getDosageInstructionFirstRep().getSite().getCodingFirstRep().getCode(), "10013000");
        assertEquals(medicationRequest.getDosageInstructionFirstRep().getSite().getCodingFirstRep().getSystem(), "http://snomed.info/sct");
    }

    @Test
    public void testMedicationAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Medication/MedicationAuthor.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 2);
        Resource mdResource = resources.values().stream().filter(resource -> resource instanceof MedicationRequest).findAny().orElse(null);
        Resource practitionerResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(mdResource);
        assertNotNull(practitionerResource);
        MedicationRequest medicationRequest = (MedicationRequest)mdResource;
        assertNotNull(medicationRequest.getRecorder().getReference());
        assertTrue(medicationRequest.getRecorder().getReference().contains("Practitioner/"));
        assertTrue(medicationRequest.getRecorder().getReference().contains(practitionerResource.getId()));

    }

    @Test
    public void testMedicationPerformer() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Medication/MedicationPerformer.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 4);
        Resource mdResource = resources.values().stream().filter(resource -> resource instanceof MedicationRequest).findAny().orElse(null);
        Resource practResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(mdResource);
        assertNotNull(practResource);
        MedicationRequest medicationRequest = (MedicationRequest)mdResource;
        assertNotNull(medicationRequest.getRequester().getReference());
        assertTrue(medicationRequest.getRequester().getReference().contains("Practitioner/"));
        assertTrue(medicationRequest.getRequester().getReference().contains(practResource.getId()));

    }

    private Resource getMedicationRequest(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(basicCDAElementsConverter);
            Map<String, Resource> resources = medicationsSectionConverter.convertMedications(((ContinuityOfCareDocument2) cda).getMedicationsSection2(), new HashMap<>());
            return resources.values().stream().filter(resource -> resource instanceof MedicationRequest).findAny().orElse(null);
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
            BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(basicCDAElementsConverter);
            return medicationsSectionConverter.convertMedications(((ContinuityOfCareDocument2) cda).getMedicationsSection2(), new HashMap<>());
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