package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;
import org.openhealthtools.mdht.uml.cda.consol.ConsolPackage;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CDAHeaderConverterTest {

    @Test
    void convertHeaderResources() throws Exception {
        ConsolPackage.eINSTANCE.eClass();
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Header/Header1.xml")).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        FileInputStream fis = new FileInputStream(decodedPath);
        ClinicalDocument cda = CDAUtil.load(fis);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems()));
        CDAHeaderConverter cdaHeaderConverter = new CDAHeaderConverter(CDABasicElementsConverter);
        Map<String, Resource> resources = cdaHeaderConverter.convertHeaderResources(cda);
        assertTrue(resources.size() != 0);
        assertEquals(resources.size(), 6);
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