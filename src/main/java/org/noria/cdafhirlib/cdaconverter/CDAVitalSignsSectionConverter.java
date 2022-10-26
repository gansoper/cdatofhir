package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class CDAVitalSignsSectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAVitalSignsSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertVitalSigns(VitalSignsSection2 vitalSignsSection2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        vitalSignsSection2.getConsolVitalSignsOrganizer2s().forEach(vsOrganizer -> resources.putAll(this.convertVitalSignOrganizer(vsOrganizer, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertVitalSignOrganizer(VitalSignsOrganizer2 vitalSignsOrganizer2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();


        if (!vitalSignsOrganizer2.getAuthors().isEmpty()) {
            resources.putAll(this.basicCDAElementsConverter.convertAuthors(null, vitalSignsOrganizer2.getAuthors(), headerResources));
        }

        if (!vitalSignsOrganizer2.getConsolVitalSignObservation2s().isEmpty()){
            for(VitalSignObservation2 vso: vitalSignsOrganizer2.getConsolVitalSignObservation2s()){
                resources.putAll(this.basicCDAElementsConverter.createFHIRObservation(vso, ObservationCategory.VITALSIGNS, resources, headerResources));
            }
        }

        return resources;
    }
}
