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

class CDAMedicationsSectionSupplyConverterTest {

    @Test
    public void testCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationSupply/MedicationCode.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationRequest).filter(e->((MedicationRequest) e).getStatus() != null).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationRequest md  = (MedicationRequest)resource;
        assertEquals(md.getStatus(), MedicationRequest.MedicationRequestStatus.COMPLETED);

    }

    @Test
    public void testDate() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationSupply/MedicationDate.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationRequest).filter(e->((MedicationRequest) e).getAuthoredOnElement() != null).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationRequest md  = (MedicationRequest)resource;
        assertEquals(md.getAuthoredOnElement().getValueAsString(), "2007-01-03");

    }

    @Test
    public void testDosageTiming() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationSupply/MedicationDosageTiming.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationRequest).filter(e->!((MedicationRequest) e).getDosageInstruction().isEmpty()).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationRequest md  = (MedicationRequest)resource;
        assertFalse(md.getDosageInstruction().isEmpty());
        Dosage dosage = md.getDosageInstructionFirstRep();
        assertEquals(1, dosage.getTiming().getRepeat().getCount());
        assertTrue(dosage.getDoseAndRate().isEmpty());

    }


    @Test
    public void testDosageQuantity() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationSupply/MedicationDosageQuantity.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationRequest).filter(e->!((MedicationRequest) e).getDosageInstruction().isEmpty()).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationRequest md  = (MedicationRequest)resource;
        assertFalse(md.getDosageInstruction().isEmpty());
        Dosage dosage = md.getDosageInstructionFirstRep();
        assertNotNull(dosage.getDoseAndRateFirstRep().getRate());
        assertTrue(dosage.getDoseAndRateFirstRep().getRate() instanceof SimpleQuantity);
        assertNotNull(((SimpleQuantity)dosage.getDoseAndRateFirstRep().getRate()).getValue());
        assertEquals(((SimpleQuantity)dosage.getDoseAndRateFirstRep().getRate()).getValue().intValue(), 75);

    }


    @Test
    public void testMedicationCodeableConcept() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationSupply/MedicationManufacturedProduct.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationRequest).filter(e->!((MedicationRequest) e).getMedicationCodeableConcept().isEmpty()).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationRequest md  = (MedicationRequest)resource;
        assertFalse(md.getMedicationCodeableConcept().isEmpty());
        assertTrue(md.getMedicationCodeableConcept().getCodingFirstRep().getCode().equals("573621"));
    }


    @Test
    public void testMedicationAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationSupply/MedicationAuthor.xml");
        assertNotNull(resources);
        assertEquals(3, resources.size());
        Resource mdResource = resources.values().stream().filter(resource -> resource instanceof MedicationRequest).findAny().orElse(null);
        Resource practitionerResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(mdResource);
        assertNotNull(practitionerResource);
        MedicationRequest medicationRequest = (MedicationRequest)mdResource;
        assertNotNull(medicationRequest.getRecorder().getReference());
        assertTrue(medicationRequest.getRecorder().getReference().contains("Practitioner/"));
        assertTrue(medicationRequest.getRecorder().getReference().contains(practitionerResource.getId()));

    }



    private Resource getMedicationRequest(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(codeMappingProcessor);
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
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(codeMappingProcessor);
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
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(codeMappingProcessor);
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