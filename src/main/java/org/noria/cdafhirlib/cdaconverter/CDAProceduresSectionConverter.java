package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.r4.model.Resource;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.openhealthtools.mdht.uml.cda.consol.ProceduresSection2;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAProceduresSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAProceduresSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertProcedures(ProceduresSection2 proceduresSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        proceduresSection.getConsolProcedureActivityProcedure2s().forEach(procedureActivityProcedure -> resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertProcedureActivityProcedure(procedureActivityProcedure, headerResources)));
        return resources;
    }

}
