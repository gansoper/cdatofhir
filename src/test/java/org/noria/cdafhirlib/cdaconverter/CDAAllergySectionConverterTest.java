package org.noria.cdafhirlib.cdaconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;
import org.openhealthtools.mdht.uml.cda.consol.ConsolPackage;
import org.openhealthtools.mdht.uml.cda.consol.ContinuityOfCareDocument2;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CDAAllergySectionConverterTest {

    @Test
    public void convertAllergiesSection() throws Exception {
        // this is needed for types casting
        ConsolPackage.eINSTANCE.eClass();
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Tests/Allergy/AllergySection.xml")).getFile());
        FileInputStream fis = new FileInputStream(file);
        ClinicalDocument cda = CDAUtil.load(fis);
        if (cda instanceof ContinuityOfCareDocument2) {
            BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(new SimpleCDATypesConverter(CodeMappingProcessor.getInstance(getTestCodes(), getSystems())));
            CDAAllergySectionConverter allergySectionConverter = new CDAAllergySectionConverter(basicCDAElementsConverter);
            Map<String, IBaseResource> resources = allergySectionConverter.convertAllergies(((ContinuityOfCareDocument2)cda).getAllergiesSection2(), new HashMap<>());
            assertTrue(!resources.isEmpty());
        }

    }

    private CDAtoFHIRCodes getTestCodes() {
        try {
            File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json")).getFile());
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, CDAtoFHIRCodes.class);
        } catch (Exception e) {
            return null;
        }
    }

    private SystemNamesMapping getSystems() {
        try {
            File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("OIDtoURL.json")).getFile());
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, SystemNamesMapping.class);
        } catch (Exception e) {
            return null;
        }
    }

}