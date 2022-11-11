package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.FamilyMemberHistory;
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

class CDAFamilyMemberHistorySectionConverterTest {


    @Test
    public void testFHSStatus() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHStatus.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasStatus());
        assertEquals("completed", fmh.getStatus().toCode());
    }

    @Test
    public void testFHSNoStatus() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHNoStatus.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasStatus());
        assertEquals("health-unknown", fmh.getStatus().toCode());
    }

    @Test
    public void testFHSSubjectCode() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHSubjectCode.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasRelationship() && fmh.getRelationship().hasCoding());
        assertEquals("FTH", fmh.getRelationship().getCodingFirstRep().getCode());
    }

    @Test
    public void testFHSSubjectNoCode() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHSubjectNoCode.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasRelationship() && fmh.getRelationship().hasCoding());
        assertEquals("FAMMEMB", fmh.getRelationship().getCodingFirstRep().getCode());
    }


    @Test
    public void testFHSSubjectGender() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHGender.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasSex() && fmh.getSex().hasCoding());
        assertEquals("male", fmh.getSex().getCodingFirstRep().getCode());
    }


    @Test
    public void testFHSBirthTime() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHBirthTime.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasBornDateType());
        assertEquals("1910", fmh.getBornDateType().getValueAsString());
    }


    @Test
    public void testFHSConditionCode() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHConditionCode.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasCondition() && fmh.getConditionFirstRep().hasCode() && fmh.getConditionFirstRep().getCode().hasCoding());
        assertEquals("22298006", fmh.getConditionFirstRep().getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void testFHSConditionTime() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHConditionTime.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasCondition() && fmh.getConditionFirstRep().hasOnset());
        assertEquals("1967", fmh.getConditionFirstRep().getOnset().primitiveValue());
    }

    @Test
    public void testFHSConditionAge() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHConditionAge.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasCondition() && fmh.getConditionFirstRep().hasOnsetAge());
        assertEquals(57, fmh.getConditionFirstRep().getOnsetAge().getValue().intValue());
    }

    @Test
    public void testFHSConditionDeath() throws Exception {
        Resource resource = this.getFamilyMeberHistory("Tests/FamilyMemberHistory/FMHConditionDeath.xml");
        assertNotNull(resource);
        assertTrue(resource instanceof FamilyMemberHistory);
        FamilyMemberHistory fmh = (FamilyMemberHistory) resource;
        assertTrue(fmh.hasDeceasedBooleanType());
        assertTrue(fmh.hasCondition() && fmh.getConditionFirstRep().hasContributedToDeath());
        assertEquals(true, fmh.getDeceasedBooleanType().getValue());
        assertEquals(true, fmh.getConditionFirstRep().getContributedToDeath());
    }


    private Resource getFamilyMeberHistory(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAFamilyHistorySectionConverter familyHistorySectionConverter = new CDAFamilyHistorySectionConverter(codeMappingProcessor);
            Map<String, Resource> resources = familyHistorySectionConverter.convertFamilyHistories(((ContinuityOfCareDocument2) cda).getFamilyHistorySection2(), new HashMap<>());
            return resources.values().stream().filter(resource -> resource instanceof FamilyMemberHistory).findAny().orElse(null);
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