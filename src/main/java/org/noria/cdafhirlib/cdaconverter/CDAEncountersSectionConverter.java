package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class CDAEncountersSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAEncountersSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertEncounters(EncountersSectionEntriesOptional2 encountersSection2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        encountersSection2.getConsolEncounterActivity2s().stream().forEach(e -> resources.putAll(this.convertEncounterActivity(e, headerResources)));
        return resources;
    }


    private Map<String, Resource> convertEncounterActivity(EncounterActivity2 encounterActivity, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);
        Encounter encounter = new Encounter();
        if (CollectionUtils.isNotEmpty(encounterActivity.getIds())) {
            encounterActivity.getIds().forEach(id -> encounter.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        encounter.setStatus(Encounter.EncounterStatus.ARRIVED); // there is no status in CDA for EncounterActivity

        if (encounterActivity.getCode() != null && !encounterActivity.getCode().isSetNullFlavor()) {
            encounter.setClass_(cdaBasicElementsConverter.createFHIRCoding(encounterActivity.getCode(), null));
        }

        if (encounterActivity.getEffectiveTime() != null && !encounterActivity.getEffectiveTime().isSetNullFlavor()) {
            Type encounterTime = cdaBasicElementsConverter.convertIVLTSDate(encounterActivity.getEffectiveTime());
            if (encounterTime instanceof Period) {
                encounter.setPeriod((Period) encounterTime);
            } else {
                Period period = new Period();
                period.setStartElement((DateTimeType) encounterTime);
                encounter.setPeriod(period);
            }
        }

        if (!encounterActivity.getPerformers().isEmpty()) {
            Map<String, Resource> encounterParticipants = new HashMap<>();
            encounterActivity.getPerformers().forEach(performer -> encounterParticipants.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertPerformer(performer, headerResources)));
            if (!encounterParticipants.isEmpty()) {
                List<Resource> practitioners = encounterParticipants.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                if (!practitioners.isEmpty()) {
                    Encounter.EncounterParticipantComponent encounterParticipantComponent = new Encounter.EncounterParticipantComponent();
                    encounterParticipantComponent.setIndividual(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioners.get(0).getId()));
                    encounter.getParticipant().add(encounterParticipantComponent);
                }

                resources.putAll(encounterParticipants);
            }
        }

        List<Location> locations = null;

        if (!encounterActivity.getConsolServiceDeliveryLocations().isEmpty()) {
            locations = encounterActivity.getConsolServiceDeliveryLocations()
                    .stream()
                    .map(this::convertLocation)
                    .collect(Collectors.toList());
        }
        else if (!encounterActivity.getParticipants().isEmpty()){
            locations = encounterActivity.getParticipants()
                    .stream()
                    .filter(pl-> pl.getParticipantRole() instanceof ServiceDeliveryLocation)
                    .map(pl-> this.convertLocation((ServiceDeliveryLocation) pl.getParticipantRole()))
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isNotEmpty(locations)){

            locations.forEach(l -> {
                Encounter.EncounterLocationComponent encounterLocationComponent = new Encounter.EncounterLocationComponent();
                encounterLocationComponent.setLocation(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.LOCATION, l.getId()));
                encounter.getLocation().add(encounterLocationComponent);
                resources.put(l.getId(), l);
            });
        }


        if (CollectionUtils.isNotEmpty(encounterActivity.getConsolEncounterDiagnosis2s())) {
            for (EncounterDiagnosis2 ed : encounterActivity.getConsolEncounterDiagnosis2s()) {
                if (CollectionUtils.isNotEmpty(ed.getConsolProblemObservation2s())) {
                    for (ProblemObservation2 po : ed.getConsolProblemObservation2s()) {
                        Map<String, Resource> conditions = cdaCommonElementsConverter.convertObservationToCondition(po, null, null, headerResources);
                        conditions.values().stream().filter(resource -> resource instanceof Condition).forEach(o -> {
                            Encounter.DiagnosisComponent diagnosisComponent = new Encounter.DiagnosisComponent();
                            diagnosisComponent.setCondition(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.CONDITION, o.getId()));
                            encounter.getDiagnosis().add(diagnosisComponent);
                        });
                        resources.putAll(conditions);
                    }
                }
            }

        }

        encounter.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ENCOUNTER, encounter.getIdentifier()));
        resources.put(encounter.getId(), encounter);
        return resources;
    }

    private Location convertLocation(ServiceDeliveryLocation sdLoc) {
        Location location = new Location();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        if (CollectionUtils.isNotEmpty(sdLoc.getIds())) {
            sdLoc.getIds().forEach(id -> location.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        if (CollectionUtils.isNotEmpty(sdLoc.getAddrs())) {
            location.setAddress(cdaBasicElementsConverter.createFHIRAddress(sdLoc.getAddrs().get(0)));
        }

        if (CollectionUtils.isNotEmpty(sdLoc.getTelecoms())) {
            location.setTelecom(sdLoc.getTelecoms().stream().map(cdaBasicElementsConverter::createContactPoint).collect(Collectors.toList()));
        }

        if (sdLoc.getPlayingEntity() != null && !sdLoc.getPlayingEntity().isSetNullFlavor()) {
            if (CollectionUtils.isNotEmpty(sdLoc.getPlayingEntity().getNames())) {
                sdLoc.getPlayingEntity().getNames()
                        .stream()
                        .filter(n -> !n.isSetNullFlavor())
                        .findFirst()
                        .ifPresent(name -> location.setName(name.getText()));
            }
        }

        location.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.LOCATION, location.getIdentifier()));

        return location;
    }


}
