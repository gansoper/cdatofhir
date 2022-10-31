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

class CDAMedicationsSectionStatementConverterTest {



    @Test
    public void testEIVL_TSMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationEIVL.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertFalse(medicationStatement.getDosage().isEmpty());
        Timing timing = medicationStatement.getDosageFirstRep().getTiming();
        assertNotNull(timing.getCode().getCodingFirstRep().getCode());
        assertEquals(timing.getCode().getCodingFirstRep().getCode(), "AC");
        assertEquals(timing.getRepeat().getOffset(), 10);
    }

    @Test
    public void testPIVL_TSMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationPIVL.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertFalse(medicationStatement.getDosage().isEmpty());
        Timing timing = medicationStatement.getDosageFirstRep().getTiming();
        assertNotNull(timing);
        assertEquals(timing.getRepeat().getPeriod(), new BigDecimal(6));
        assertEquals(timing.getRepeat().getPeriodUnit(), Timing.UnitsOfTime.H);
    }

    @Test
    public void testRouteCodeMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationRouteCode.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertFalse(medicationStatement.getDosage().isEmpty());
        CodeableConcept route = medicationStatement.getDosageFirstRep().getRoute();
        assertNotNull(route.getCodingFirstRep().getCode());
        assertEquals(route.getCodingFirstRep().getCode(), "C38216");
        assertEquals(route.getCodingFirstRep().getSystem(), "urn:oid:2.16.840.1.113883.3.26.1.1");
    }

    @Test
    public void testDoseQuantityMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationDoseSimpleQuantity.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertFalse(medicationStatement.getDosage().isEmpty());
        Type dose = medicationStatement.getDosageFirstRep().getDoseAndRateFirstRep().getDose();
        assertNotNull(dose);
        assertTrue(dose instanceof SimpleQuantity);
        SimpleQuantity sq = (SimpleQuantity) dose;
        assertEquals(sq.getUnit(), "mg");
        assertEquals(sq.getValue(), new BigDecimal(2));
    }

    @Test
    public void testDoseRangeQuantityMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationDoseRangeQuantity.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertFalse(medicationStatement.getDosage().isEmpty());
        Type dose = medicationStatement.getDosageFirstRep().getDoseAndRateFirstRep().getDose();
        assertNotNull(dose);
        assertTrue(dose instanceof Range);
        Range range = (Range) dose;
        assertEquals(range.getLow().getValue(), new BigDecimal(1));
        assertEquals(range.getHigh().getValue(), new BigDecimal(2));
    }

    @Test
    public void testMedicationConsumableMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationConsumable.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertNotNull(medicationStatement.getMedication());
        assertTrue(medicationStatement.getMedication() instanceof CodeableConcept);
        CodeableConcept medication = (CodeableConcept) medicationStatement.getMedication();
        assertNotNull(medication.getCodingFirstRep().getCode());
        assertEquals(medication.getCodingFirstRep().getCode(), "573621");
        assertEquals(medication.getCodingFirstRep().getSystem(), "http://www.nlm.nih.gov/research/umls/rxnorm");
    }


    @Test
    public void testRepeatNumberMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationRepeatNumber.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertFalse(medicationStatement.getDosage().isEmpty());
        Timing timing = medicationStatement.getDosageFirstRep().getTiming();
        assertNotNull(timing);
        assertEquals(timing.getRepeat().getCount(), 1);

    }

    @Test
    public void testApproachSiteCodeMedicationStatement() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationApproachSiteCode.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertNotNull(medicationStatement.getDosageFirstRep().getSite().getCodingFirstRep().getCode());
        assertEquals(medicationStatement.getDosageFirstRep().getSite().getCodingFirstRep().getCode(), "10013000");
        assertEquals(medicationStatement.getDosageFirstRep().getSite().getCodingFirstRep().getSystem(), "http://snomed.info/sct");
    }

    @Test
    public void testMedicationAuthorMedicationStatement() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationEVN/MedicationAuthor.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 2);
        Resource mdResource = resources.values().stream().filter(resource -> resource instanceof MedicationStatement).findAny().orElse(null);
        Resource practitionerResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(mdResource);
        assertNotNull(practitionerResource);
        MedicationStatement medicationStatement = (MedicationStatement) mdResource;
        assertNotNull(medicationStatement.getInformationSource().getReference());
        assertTrue(medicationStatement.getInformationSource().getReference().contains("Practitioner/"));
        assertTrue(medicationStatement.getInformationSource().getReference().contains(practitionerResource.getId()));

    }

    @Test
    public void testMedicationPerformerMedicationStatement() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationEVN/MedicationPerformer.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 4);
        Resource mdResource = resources.values().stream().filter(resource -> resource instanceof MedicationStatement).findAny().orElse(null);
        Resource practResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(mdResource);
        assertNotNull(practResource);
        MedicationStatement medicationStatement = (MedicationStatement) mdResource;
        assertNotNull(medicationStatement.getInformationSource().getReference());
        assertTrue(medicationStatement.getInformationSource().getReference().contains("Practitioner/"));
        assertTrue(medicationStatement.getInformationSource().getReference().contains(practResource.getId()));

    }


    @Test
    public void testMedicationPatientMedicationStatement() throws Exception {
        Map<String, Resource> resources = this.getAllResourcesWithPatient("Tests/MedicationEVN/MedicationAuthor.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 2);
        Resource mdResource = resources.values().stream().filter(resource -> resource instanceof MedicationStatement).findAny().orElse(null);
        assertNotNull(mdResource);
        MedicationStatement medicationStatement = (MedicationStatement) mdResource;
        assertNotNull(medicationStatement.getSubject().getReference());
        assertEquals(medicationStatement.getSubject().getReference(), "Patient/test");
    }


    @Test
    public void testIncorrectStatusCode() throws Exception {
        Resource resource = this.getMedicationStatement("Tests/MedicationEVN/MedicationIncorrectStatusCode.xml");
        assertNotNull(resource);
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        assertNull(medicationStatement.getStatus());
    }


    private Resource getMedicationStatement(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(CDABasicElementsConverter);
            Map<String, Resource> resources = medicationsSectionConverter.convertMedications(((ContinuityOfCareDocument2) cda).getMedicationsSection2(), new HashMap<>());
            return resources.values().stream().filter(resource -> resource instanceof MedicationStatement).findAny().orElse(null);
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
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(CDABasicElementsConverter);
            return medicationsSectionConverter.convertMedications(((ContinuityOfCareDocument2) cda).getMedicationsSection2(), new HashMap<>());
        }

        return null;
    }

    private Map<String, Resource> getAllResourcesWithPatient(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(CDABasicElementsConverter);
            HashMap<String, Resource> headerResources = new HashMap<>();
            Patient patient = new Patient();
            patient.setId("test");
            headerResources.put("test", patient);
            return medicationsSectionConverter.convertMedications(((ContinuityOfCareDocument2) cda).getMedicationsSection2(), headerResources);
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