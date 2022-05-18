package org.noria.cdafhirlib.fhirconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.vocab.NullFlavor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;
import org.noria.cdafhirlib.enumerations.FHIRtoCDACodeConversionType;
import org.noria.cdafhirlib.model.FHIRtoCDACodes;


import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SimpleFHIRTypesConverterTest {

    @Test
    void NullableCodingAndMapping() {
        SimpleFHIRTypesConverter simpleFHIRTypesConverter = new SimpleFHIRTypesConverter(null);
        CD cd = simpleFHIRTypesConverter.createCD(null, null);
        assertNotNull(cd);
        assertNull(cd.getCode());
        assertEquals(cd.getNullFlavor(), NullFlavor.UNK);
        assertEquals(cd.getTranslations().size(), 0);
    }

    @Test
    void EmptyCodingAndNullableMapping() {
        SimpleFHIRTypesConverter simpleFHIRTypesConverter = new SimpleFHIRTypesConverter(null);
        CD cd = simpleFHIRTypesConverter.createCD(new Coding(), null);
        assertNotNull(cd);
        assertNull(cd.getCode());
        assertEquals(cd.getNullFlavor(), NullFlavor.UNK);
        assertEquals(cd.getTranslations().size(), 0);
    }

    @Test
    void EmptyCodingAndNonExistingMapping() {
        SimpleFHIRTypesConverter simpleFHIRTypesConverter = new SimpleFHIRTypesConverter(null);
        CD cd = simpleFHIRTypesConverter.createCD(new Coding(), "test");
        assertNotNull(cd);
        assertNull(cd.getCode());
        assertEquals(cd.getTranslations().size(), 0);
    }

    @Test
    void CodingAndNonExistingMapping() {
        SimpleFHIRTypesConverter simpleFHIRTypesConverter = new SimpleFHIRTypesConverter(null);
        Coding coding = new Coding();
        coding.setCode("test");
        CD cd = simpleFHIRTypesConverter.createCD(coding, "test");
        assertNotNull(cd);
        assertEquals(cd.getCode(), "test");
        assertEquals(cd.getTranslations().size(), 0);
    }


    @Test
    void EmptyCodeableConceptAndNonExistingMapping() {
        SimpleFHIRTypesConverter simpleFHIRTypesConverter = new SimpleFHIRTypesConverter(null);
        CodeableConcept codeableConcept = new CodeableConcept();
        CD cd = simpleFHIRTypesConverter.createCDWithTranslation(codeableConcept, "test");
        assertNotNull(cd);
        assertNull(cd.getCode());
        assertEquals(cd.getNullFlavor(), NullFlavor.UNK);
        assertEquals(cd.getTranslations().size(), 0);
    }

    @Test
    void CodeableConceptAndNonExistingMapping() {
        SimpleFHIRTypesConverter simpleFHIRTypesConverter = new SimpleFHIRTypesConverter(null);
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode("test");
        codeableConcept.getCoding().add(coding);
        CD cd = simpleFHIRTypesConverter.createCDWithTranslation(codeableConcept, "test");
        assertNotNull(cd);
        assertEquals(cd.getCode(), "test");
        assertEquals(cd.getTranslations().size(), 0);

        Coding secondCoding = new Coding();
        secondCoding.setCode("test2");
        codeableConcept.getCoding().add(secondCoding);
        cd = simpleFHIRTypesConverter.createCDWithTranslation(codeableConcept, "test");
        assertNotNull(cd);
        assertEquals(cd.getCode(), "test");
        assertEquals(cd.getTranslations().size(), 1);
        assertEquals(cd.getTranslations().get(0).getCode(), "test2");
    }


    @Test
    void CodeableConceptAndExistingMapping() {
        SimpleFHIRTypesConverter simpleFHIRTypesConverter = new SimpleFHIRTypesConverter(this.getTestCodes());
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode("confirmed");
        codeableConcept.getCoding().add(coding);
        CD cd = simpleFHIRTypesConverter.createCDWithTranslation(codeableConcept, FHIRtoCDACodeConversionType.REACTION_STATUS_CODING_VERIFICATION.toValue());
        assertNotNull(cd);
        assertEquals(cd.getCode(), "completed");
        assertEquals(cd.getTranslations().size(), 0);

        Coding secondCoding = new Coding();
        secondCoding.setCode("unconfirmed");
        codeableConcept.getCoding().add(secondCoding);
        cd = simpleFHIRTypesConverter.createCDWithTranslation(codeableConcept, FHIRtoCDACodeConversionType.REACTION_STATUS_CODING_VERIFICATION.toValue());
        assertNotNull(cd);
        assertEquals(cd.getCode(), "completed");
        assertEquals(cd.getTranslations().size(), 1);
        assertEquals(cd.getTranslations().get(0).getCode(), "completed");
    }


    private FHIRtoCDACodes getTestCodes() {
        try {
            File file = new File(this.getClass().getClassLoader().getResource("FHIRtoCDACodes.json").getFile());
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, FHIRtoCDACodes.class);
        } catch (Exception e) {
            return null;
        }
    }

}