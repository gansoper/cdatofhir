package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class CDAProceduresSectionConverter {

    private final CDABasicElementsConverter CDABasicElementsConverter;

    public CDAProceduresSectionConverter(CDABasicElementsConverter CDABasicElementsConverter) {
        this.CDABasicElementsConverter = CDABasicElementsConverter;
    }

    public Map<String, Resource> convertProcedures(ProceduresSection2 proceduresSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        proceduresSection.getConsolProcedureActivityProcedure2s().forEach(procedureActivityProcedure -> resources.putAll(CDACommonElementsConverter.getInstance(this.CDABasicElementsConverter).convertProcedureActivityProcedure(procedureActivityProcedure, headerResources)));
        return resources;
    }

}
