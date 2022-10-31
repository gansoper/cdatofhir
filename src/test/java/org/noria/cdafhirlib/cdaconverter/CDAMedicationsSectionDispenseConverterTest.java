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

class CDAMedicationsSectionDispenseConverterTest {

    @Test
    public void testCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationDispense/MedicationCode.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationDispense).filter(e -> ((MedicationDispense) e).getStatus() != null).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationDispense md = (MedicationDispense) resource;
        assertEquals(md.getStatus(), MedicationDispense.MedicationDispenseStatus.COMPLETED);

    }

    @Test
    public void testDate() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationDispense/MedicationDate.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationDispense).filter(e -> ((MedicationDispense) e).getWhenPrepared() != null).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationDispense md = (MedicationDispense) resource;
        assertEquals(md.getWhenPreparedElement().getValueAsString(), "2012-08-15T14:50:00-08:00");

    }

    @Test
    public void testDosageTiming() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationDispense/MedicationDosageTiming.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationDispense).filter(e -> !((MedicationDispense) e).getDosageInstruction().isEmpty()).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationDispense md = (MedicationDispense) resource;
        assertFalse(md.getDosageInstruction().isEmpty());
        Dosage dosage = md.getDosageInstructionFirstRep();
        assertEquals(1, dosage.getTiming().getRepeat().getCount());
        assertTrue(dosage.getDoseAndRate().isEmpty());

    }


    @Test
    public void testDosageQuantity() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationDispense/MedicationDosageQuantity.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationDispense).filter(e -> !((MedicationDispense) e).getDosageInstruction().isEmpty()).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationDispense md = (MedicationDispense) resource;
        assertFalse(md.getDosageInstruction().isEmpty());
        Dosage dosage = md.getDosageInstructionFirstRep();
        assertNotNull(dosage.getDoseAndRateFirstRep().getRate());
        assertTrue(dosage.getDoseAndRateFirstRep().getRate() instanceof SimpleQuantity);
        assertNotNull(((SimpleQuantity) dosage.getDoseAndRateFirstRep().getRate()).getValue());
        assertEquals(((SimpleQuantity) dosage.getDoseAndRateFirstRep().getRate()).getValue().intValue(), 75);

    }


    @Test
    public void testMedicationCodeableConcept() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationDispense/MedicationManufacturedProduct.xml");
        assertEquals(2, resources.size());
        Resource resource = resources.values().stream().filter(e -> e instanceof MedicationDispense).filter(e -> !((MedicationDispense) e).getMedicationCodeableConcept().isEmpty()).findFirst().orElse(null);
        assertNotNull(resource);
        MedicationDispense md = (MedicationDispense) resource;
        assertFalse(md.getMedicationCodeableConcept().isEmpty());
        assertTrue(md.getMedicationCodeableConcept().getCodingFirstRep().getCode().equals("573621"));
    }


    @Test
    public void testMedicationAuthor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/MedicationDispense/MedicationPerformer.xml");
        assertNotNull(resources);
        assertEquals(6, resources.size());
        Resource mdResource = resources.values().stream().filter(resource -> resource instanceof MedicationDispense).findAny().orElse(null);
        Resource practitionerResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        assertNotNull(mdResource);
        assertNotNull(practitionerResource);
        MedicationDispense md = (MedicationDispense) mdResource;
        assertFalse(md.getPerformerFirstRep().isEmpty());
        assertTrue(md.getPerformerFirstRep().getActor().getReference().contains("Practitioner/"));
        assertTrue(md.getPerformerFirstRep().getActor().getReference().contains(practitionerResource.getId()));

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