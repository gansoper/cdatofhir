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

class CDAProcedureSectionConverterTest {


    @Test
    public void testProcedureCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Procedure/ProcedureCode.xml");
        assertNotNull(resources);
        Resource pResource = resources.values().stream().filter(r -> r instanceof Procedure).findFirst().orElse(null);
        assertNotNull(pResource);
        Procedure procedure = (Procedure) pResource;
        assertTrue(procedure.hasCode());
        assertTrue(procedure.getCode().hasCoding());
        assertEquals("73761001", procedure.getCode().getCodingFirstRep().getCode());
    }

    @Test
    public void testProcedureStatus() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Procedure/ProcedureStatus.xml");
        assertNotNull(resources);
        Resource pResource = resources.values().stream().filter(r -> r instanceof Procedure).findFirst().orElse(null);
        assertNotNull(pResource);
        Procedure procedure = (Procedure) pResource;
        assertTrue(procedure.hasStatus());
        assertEquals("completed", procedure.getStatus().toCode());
    }

    @Test
    public void testProcedureEffecitveTime() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Procedure/ProcedureEffectiveTime.xml");
        assertNotNull(resources);
        Resource pResource = resources.values().stream().filter(r -> r instanceof Procedure).findFirst().orElse(null);
        assertNotNull(pResource);
        Procedure procedure = (Procedure) pResource;
        assertTrue(procedure.hasPerformedDateTimeType());
        assertEquals("2012-05-12", procedure.getPerformedDateTimeType().getValueAsString());
    }


    @Test
    public void testProcedureTargetSite() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Procedure/ProcedureTargetSite.xml");
        assertNotNull(resources);
        Resource pResource = resources.values().stream().filter(r -> r instanceof Procedure).findFirst().orElse(null);
        assertNotNull(pResource);
        Procedure procedure = (Procedure) pResource;
        assertTrue(procedure.hasBodySite());
        assertEquals("appropriate_code", procedure.getBodySiteFirstRep().getCodingFirstRep().getCode());
    }

    @Test
    public void testProcedureSpecimenCode() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Procedure/ProcedureSpecimen.xml");
        assertNotNull(resources);
        Resource pResource = resources.values().stream().filter(r -> r instanceof Procedure).findFirst().orElse(null);
        assertNotNull(pResource);
        Procedure procedure = (Procedure) pResource;
        assertTrue(procedure.hasOutcome());
        assertEquals("309226005", procedure.getOutcome().getCodingFirstRep().getCode());
    }

    @Test
    public void testProcedureSpecimenNullFlavor() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Procedure/ProcedureSpecimenNullFlavor.xml");
        assertNotNull(resources);
        Resource pResource = resources.values().stream().filter(r -> r instanceof Procedure).findFirst().orElse(null);
        assertNotNull(pResource);
        Procedure procedure = (Procedure) pResource;
        assertFalse(procedure.hasOutcome());
    }

    //fixme: add ID check
    @Test
    public void testProcedurePerformer() throws Exception {
        Map<String, Resource> resources = this.getAllResources("Tests/Procedure/ProcedurePerformer.xml");
        assertNotNull(resources);
        assertEquals(resources.size(), 5);
        Resource pResource = resources.values().stream().filter(resource -> resource instanceof Procedure).findAny().orElse(null);
        Resource practResource = resources.values().stream().filter(resource -> resource instanceof Practitioner).findAny().orElse(null);
        Resource orgResource = resources.values().stream().filter(resource -> resource instanceof Organization).findAny().orElse(null);
        assertNotNull(pResource);
        assertNotNull(practResource);
        assertNotNull(orgResource);
        Procedure procedure = (Procedure) pResource;
        assertFalse(procedure.getPerformer().isEmpty());
        assertNotNull(procedure.getPerformer().stream().filter(p-> p.getActor().getReference().contains("Practitioner/")).findAny().orElse(null));
        assertNotNull(procedure.getPerformer().stream().filter(p-> p.getActor().getReference().contains("Organization/")).findAny().orElse(null));
    }

    private Map<String, Resource> getAllResources(String testFileName) throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(testFileName)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            CodeMappingProcessor codeMappingProcessor = new CodeMappingProcessor(this.getTestCodes(), getSystems());
            CDAProceduresSectionConverter cdaProceduresSectionConverter = new CDAProceduresSectionConverter(codeMappingProcessor);
            return cdaProceduresSectionConverter.convertProcedures(((ContinuityOfCareDocument2) cda).getProceduresSection2(), new HashMap<>());
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