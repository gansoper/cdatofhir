package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class CDAPlanOfTreatmentSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;


    public CDAPlanOfTreatmentSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertPlanOfTreatment(PlanOfTreatmentSection2 planOfTreatmentSection2, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDACommonElementsConverter cdaCommonElementsConverter = CDACommonElementsConverter.getInstance(this.codeMappingProcessor);

        List<CarePlan.CarePlanActivityComponent> carePlanActivityComponents = new ArrayList<>();
        for (PlannedObservation2 plannedObservation : planOfTreatmentSection2.getConsolPlannedObservation2s()) {
            Map<String, Resource> observationResources = cdaCommonElementsConverter.convertObservation(plannedObservation, ObservationCategory.ACTIVITY, new HashMap<>(), headerResources);
            carePlanActivityComponents.addAll(this.createCarePlanActivityComponents(observationResources));
            resources.putAll(observationResources);
        }

        for (PlannedProcedure2 plannedProcedure2 : planOfTreatmentSection2.getConsolPlannedProcedure2s()) {
            Map<String, Resource> procedureResources = cdaCommonElementsConverter.convertProcedure(plannedProcedure2, headerResources);
            carePlanActivityComponents.addAll(this.createCarePlanActivityComponents(procedureResources));
            resources.putAll(procedureResources);
        }

        for (PlannedMedicationActivity2 plannedMedicationActivity : planOfTreatmentSection2.getConsolPlannedMedicationActivity2s()) {
            Map<String, Resource> medicationResources = cdaCommonElementsConverter.convertToMedicationStatement(plannedMedicationActivity, headerResources);
            carePlanActivityComponents.addAll(this.createCarePlanActivityComponents(medicationResources));
            resources.putAll(medicationResources);
        }

        for (PlannedAct2 plannedAct : planOfTreatmentSection2.getConsolPlannedAct2s()) {
            resources.putAll(this.convertPlanedAct(plannedAct, headerResources, carePlanActivityComponents));
        }


        return resources;
    }

    private Map<String, Resource> convertPlanedAct(PlannedAct2 plannedAct, Map<String, Resource> headerResources, List<CarePlan.CarePlanActivityComponent> carePlanActivityComponents) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        CarePlan carePlan = new CarePlan();
        if (!plannedAct.getIds().isEmpty()) {
            plannedAct.getIds().forEach(id -> carePlan.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }
        carePlan.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.CAREPLAN, carePlan.getIdentifier()));

        if (!plannedAct.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthors(carePlan, plannedAct.getAuthors(), headerResources));
        }

        if (plannedAct.getStatusCode() != null && !plannedAct.getStatusCode().isSetNullFlavor()) {
            try {
                carePlan.setStatus(CarePlan.CarePlanStatus.fromCode(plannedAct.getStatusCode().getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (!carePlan.hasStatus()) {
            carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        }

        if (plannedAct.getCode() != null && !plannedAct.getCode().isSetNullFlavor()) {
            carePlan.setCategory(Collections.singletonList(cdaBasicElementsConverter.createFHIRCodeableConcept(plannedAct.getCode(), null)));
        }

        if (plannedAct.getEffectiveTime() != null && !plannedAct.getEffectiveTime().isSetNullFlavor()) {
            Type dateTime = cdaBasicElementsConverter.convertIVLTSDate(plannedAct.getEffectiveTime());
            if (dateTime instanceof Period) {
                carePlan.setPeriod((Period) dateTime);
            } else if (dateTime instanceof DateTimeType) {
                Period period = new Period();
                period.setStartElement((DateTimeType) dateTime);
                carePlan.setPeriod(period);
            }
        }

        carePlan.setActivity(carePlanActivityComponents);
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
        resources.put(carePlan.getId(), carePlan);
        return resources;
    }

    private List<CarePlan.CarePlanActivityComponent> createCarePlanActivityComponents(Map<String, Resource> resources) {
        List<CarePlan.CarePlanActivityComponent> carePlanActivityComponents = resources.values().stream().filter(r -> !(r instanceof Practitioner)).map(r -> {
            CarePlan.CarePlanActivityComponent carePlanActivityComponent = new CarePlan.CarePlanActivityComponent();
            if (r instanceof Procedure) {
                carePlanActivityComponent.setReference(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PROCEDURE, r.getId()));
            } else if (r instanceof Observation) {
                carePlanActivityComponent.setReference(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.OBSERVATION, r.getId()));
            } else if (r instanceof MedicationStatement) {
                carePlanActivityComponent.setReference(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.MEDICATIONSTATEMENT, r.getId()));
            }
            return carePlanActivityComponent;
        }).collect(Collectors.toList());

        return carePlanActivityComponents;
    }
}
