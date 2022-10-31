package org.noria.cdafhirlib.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.noria.cdafhirlib.cdaconverter.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.constants.BaseConstants;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;
import org.noria.cdafhirlib.model.SystemNamesMapping;
import org.openhealthtools.mdht.uml.cda.consol.ContinuityOfCareDocument2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class FHIRProducer {

    private static FHIRProducer fhirProducer;
    private final CDAtoFHIRCodes cdAtoFHIRCodes;
    private final SystemNamesMapping systemNamesMapping;

    private FHIRProducer() throws IOException {
        this.cdAtoFHIRCodes = this.getTestCodes();
        this.systemNamesMapping = this.getSystems();
    }

    public static FHIRProducer getFHIRProducer() {
        if (fhirProducer == null) {
            try {
                fhirProducer = new FHIRProducer();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return fhirProducer;
    }

    public Bundle produceFHIRBundle(InputStream cdaInputStream) throws Exception {
        ClinicalDocument cda = CDAUtil.load(cdaInputStream);
        CDABasicElementsConverter CDABasicElementsConverter = new CDABasicElementsConverter(CodeMappingProcessor.getInstance(this.cdAtoFHIRCodes, this.systemNamesMapping));
        CDAHeaderConverter headerConverter = new CDAHeaderConverter(CDABasicElementsConverter);
        CDAAllergySectionConverter allergySectionConverter = new CDAAllergySectionConverter(CDABasicElementsConverter);
        CDAMedicationsSectionConverter medicationsSectionConverter = new CDAMedicationsSectionConverter(CDABasicElementsConverter);

        Map<String, Resource> resources = new HashMap<>();
        Map<String, Resource> headerResources = headerConverter.convertHeaderResources(cda);
        resources.putAll(headerResources);
        if (cda instanceof ContinuityOfCareDocument2){
            ContinuityOfCareDocument2 ccd = (ContinuityOfCareDocument2) cda;
            resources.putAll(allergySectionConverter.convertAllergies(ccd.getAllergiesSection2(), headerResources));
            resources.putAll(medicationsSectionConverter.convertMedications(ccd.getMedicationsSection2(), headerResources));
        }

        Bundle bundle = new Bundle();
        resources.forEach((k, v) -> {
            Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
            bundleEntryComponent.setResource(v);
            bundleEntryComponent.setFullUrl(v.getResourceType().toString() + "/" + k);
            bundle.getEntry().add(bundleEntryComponent);
        });
        return bundle;
    }

    private CDAtoFHIRCodes getTestCodes() throws IOException {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(BaseConstants.CDA_TO_FHIR_CODES_FILE)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        File file = new File(decodedPath);
        ObjectMapper om = new ObjectMapper();
        return om.readValue(file, CDAtoFHIRCodes.class);
    }

    private SystemNamesMapping getSystems() throws IOException {
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(BaseConstants.OID_TO_URL_FILE)).getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        File file = new File(decodedPath);
        ObjectMapper om = new ObjectMapper();
        return om.readValue(file, SystemNamesMapping.class);
    }
}
