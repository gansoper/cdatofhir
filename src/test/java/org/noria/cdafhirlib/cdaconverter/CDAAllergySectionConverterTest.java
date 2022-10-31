package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
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

class CDAAllergySectionConverterTest {

    @Test
    public void convertAllergiesSection() throws Exception {
        // this is needed for types casting
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Allergy/AllergySection.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAAllergySectionConverter allergySectionConverter = new CDAAllergySectionConverter(CDABasicElementsConverter);
            HashMap<String, Resource> headerResources = new HashMap<>();
            Patient patient = new Patient();
            patient.setId("test");
            headerResources.put("test", patient);
            Map<String, Resource> resources = allergySectionConverter.convertAllergies(((ContinuityOfCareDocument2) cda).getAllergiesSection2(), headerResources);
            assertFalse(resources.isEmpty());
            assertEquals(resources.size(), 3);
            Resource Resource = resources.values().stream().filter(resource -> resource instanceof AllergyIntolerance).findAny().orElse(null);
            assertNotNull(Resource);
            AllergyIntolerance allergyIntolerance = (AllergyIntolerance) Resource;
            assertEquals(allergyIntolerance.getRecordedDateElement().getValueAsString(), "1998-05-01T11:45:00-08:00");

            assertNull(allergyIntolerance.getIdentifier().get(0).getSystem());

            assertTrue(allergyIntolerance.getOnset() instanceof Period);
            assertEquals(((Period) allergyIntolerance.getOnset()).getStartElement().getValueAsString(), "1998-05-01");

            assertEquals(allergyIntolerance.getCode().getCodingFirstRep().getCode(), "70618");
            assertEquals(allergyIntolerance.getCode().getCodingFirstRep().getSystem(), "http://www.nlm.nih.gov/research/umls/rxnorm");

            assertEquals(allergyIntolerance.getClinicalStatus().getCodingFirstRep().getCode(), "active");


            assertEquals(allergyIntolerance.getReactionFirstRep().getManifestationFirstRep().getCodingFirstRep().getCode(), "422587007");
            assertEquals(allergyIntolerance.getReactionFirstRep().getManifestationFirstRep().getCodingFirstRep().getSystem(), "http://snomed.info/sct");

            assertEquals(allergyIntolerance.getReactionFirstRep().getSeverity().toCode(), "mild");

            assertEquals(allergyIntolerance.getCriticality().toCode(), "high");

            assertEquals(allergyIntolerance.getVerificationStatus().getCodingFirstRep().getCode(), "confirmed");
            assertEquals(allergyIntolerance.getPatient().getReference(), "Patient/test");
            assertFalse(allergyIntolerance.getRecorder().isEmpty());
        }

    }

    @Test
    public void convertAllergiesSectionNoAuthor() throws Exception {
        // this is needed for types casting
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Allergy/AllergySectionNoAuthor.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
            CDAAllergySectionConverter allergySectionConverter = new CDAAllergySectionConverter(CDABasicElementsConverter);
            HashMap<String, Resource> headerResources = new HashMap<>();
            Patient patient = new Patient();
            patient.setId("test");
            headerResources.put("test", patient);
            Map<String, Resource> resources = allergySectionConverter.convertAllergies(((ContinuityOfCareDocument2) cda).getAllergiesSection2(), headerResources);
            assertFalse(resources.isEmpty());
            assertEquals(resources.size(), 2);
            Resource Resource = resources.values().stream().filter(resource -> resource instanceof AllergyIntolerance).findAny().orElse(null);
            assertNotNull(Resource);
            AllergyIntolerance allergyIntolerance = (AllergyIntolerance) Resource;
            assertEquals(allergyIntolerance.getRecordedDateElement().getValueAsString(), "1998-05-01T11:45:00-08:00");

            assertNull(allergyIntolerance.getIdentifier().get(0).getSystem());

            assertTrue(allergyIntolerance.getOnset() instanceof Period);
            assertEquals(((Period) allergyIntolerance.getOnset()).getStartElement().getValueAsString(), "1998-05-01");

            assertEquals(allergyIntolerance.getCode().getCodingFirstRep().getCode(), "70618");
            assertEquals(allergyIntolerance.getCode().getCodingFirstRep().getSystem(), "http://www.nlm.nih.gov/research/umls/rxnorm");

            assertEquals(allergyIntolerance.getClinicalStatus().getCodingFirstRep().getCode(), "active");


            assertEquals(allergyIntolerance.getReactionFirstRep().getManifestationFirstRep().getCodingFirstRep().getCode(), "422587007");
            assertEquals(allergyIntolerance.getReactionFirstRep().getManifestationFirstRep().getCodingFirstRep().getSystem(), "http://snomed.info/sct");

            assertEquals(allergyIntolerance.getReactionFirstRep().getSeverity().toCode(), "mild");

            assertEquals(allergyIntolerance.getCriticality().toCode(), "high");

            assertEquals(allergyIntolerance.getVerificationStatus().getCodingFirstRep().getCode(), "confirmed");
            assertEquals(allergyIntolerance.getPatient().getReference(), "Patient/test");
            assertFalse(allergyIntolerance.getRecorder().isEmpty());
        }

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