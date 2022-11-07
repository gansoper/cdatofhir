package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.vocab.ActClass;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.openhealthtools.mdht.uml.cda.consol.EncounterActivity2;
import org.openhealthtools.mdht.uml.cda.consol.EncountersSection2;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CDAEncountersSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAEncountersSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertEncounters(EncountersSection2 encountersSection2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        encountersSection2.getConsolEncounterActivity2s().stream().forEach(e -> resources.putAll(this.convertEncounterActivity(e, headerResources)));
        return resources;
    }


    private Map<String, Resource> convertEncounterActivity(EncounterActivity2 encounterActivity, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        Encounter encounter = new Encounter();
        if (CollectionUtils.isNotEmpty(encounterActivity.getIds())) {
            encounterActivity.getIds().forEach(id -> encounter.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        encounter.setStatus(Encounter.EncounterStatus.ARRIVED); // there is no status in CDA for EncounterActivity

        if (encounterActivity.getCode() != null && !encounterActivity.getCode().isSetNullFlavor()){
            encounter.setClass_(cdaBasicElementsConverter.createFHIRCoding(encounterActivity.getCode(), null));
        }

        if (encounterActivity.getEffectiveTime() != null && !encounterActivity.getEffectiveTime().isSetNullFlavor()){
            Type encounterTime = cdaBasicElementsConverter.convertIVLTSDate(encounterActivity.getEffectiveTime());
            if (encounterTime instanceof Period){
                encounter.setPeriod((Period) encounterTime);
            }
            else{
                Period period  = new Period();
                period.setStartElement((DateTimeType) encounterTime);
                encounter.setPeriod(period);
            }

        }

        return resources;
    }

}
