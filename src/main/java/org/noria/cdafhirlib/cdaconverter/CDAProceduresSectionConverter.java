package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.eclipse.mdht.uml.hl7.vocab.SetOperator;
import org.eclipse.mdht.uml.hl7.vocab.x_DocumentSubstanceMood;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class CDAProceduresSectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAProceduresSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertProcedures(ProceduresSection2 proceduresSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        proceduresSection.getConsolProcedureActivityProcedure2s().forEach(procedureActivityProcedure -> resources.putAll(this.convertProcedureActivityProcedure(procedureActivityProcedure, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertProcedureActivityProcedure(ProcedureActivityProcedure2 procedureActivityProcedure, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        Procedure procedure = new Procedure();
        if (CollectionUtils.isNotEmpty(procedureActivityProcedure.getIds())) {
            procedureActivityProcedure.getIds().forEach(id -> procedure.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        if (procedureActivityProcedure.getEffectiveTime() !=null && !procedureActivityProcedure.getEffectiveTime().isSetNullFlavor()){
            Type performedDate = this.basicCDAElementsConverter.convertIVLTSDate(procedureActivityProcedure.getEffectiveTime());
            procedure.setPerformed(performedDate);
        }

        if (procedureActivityProcedure.getCode() != null && !procedureActivityProcedure.getCode().isSetNullFlavor()){
            procedure.setCode(this.basicCDAElementsConverter.createFHIRCodeableConcept(procedureActivityProcedure.getCode(), null));
        }

        if (procedureActivityProcedure.getStatusCode() != null && !procedureActivityProcedure.getStatusCode().isSetNullFlavor()){
            try{
                Coding procedureStatusCoding = this.basicCDAElementsConverter.createFHIRCoding(procedureActivityProcedure.getStatusCode(), CDAtoFHIRCodeConversionType.PROCEDURE_STATUS.toValue());
                procedure.setStatus(Procedure.ProcedureStatus.fromCode(procedureStatusCoding.getCode()));
            }
            catch (FHIRException e){
                log.error(e.getMessage(), e);
            }
        }

        if (!procedureActivityProcedure.getTargetSiteCodes().isEmpty()){
            List<CodeableConcept> bodySite =procedureActivityProcedure.getTargetSiteCodes().stream().map(cd-> this.basicCDAElementsConverter.createFHIRCodeableConcept(cd, null)).collect(Collectors.toList());
            procedure.setBodySite(bodySite);
        }

        if (!procedureActivityProcedure.getSpecimens().isEmpty()){
            List<Coding> outcomeCodingList = procedureActivityProcedure.getSpecimens().stream()
                    .filter(specimen -> !specimen.isSetNullFlavor() && specimen.getSpecimenRole() !=null && !specimen.getSpecimenRole().isSetNullFlavor() )
                    .filter(specimen -> specimen.getSpecimenRole().getSpecimenPlayingEntity() !=null && !specimen.getSpecimenRole().getSpecimenPlayingEntity().isSetNullFlavor())
                    .map(specimen -> this.basicCDAElementsConverter.createFHIRCoding(specimen.getSpecimenRole().getSpecimenPlayingEntity().getCode(), null))
                    .collect(Collectors.toList());
            CodeableConcept outcome = new CodeableConcept();
            outcome.setCoding(outcomeCodingList);
            procedure.setOutcome(outcome);
        }

        if (CollectionUtils.isNotEmpty(procedureActivityProcedure.getPerformers())){
            Map<String, Resource> procedurePerformers = new HashMap<>();
            procedureActivityProcedure.getPerformers().forEach(performer -> procedurePerformers.putAll(this.basicCDAElementsConverter.convertPerformer(performer, headerResources)));
            if (!procedurePerformers.isEmpty()) {
                List<Resource> practitioners = procedurePerformers.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations = procedurePerformers.values().stream().filter(r -> r instanceof Organization).collect(Collectors.toList());

                List<Procedure.ProcedurePerformerComponent> procedurePerformerComponents = new ArrayList<>();
                for(Resource practitioner: practitioners){
                    Procedure.ProcedurePerformerComponent procedurePerformerComponent = new Procedure.ProcedurePerformerComponent();
                    procedurePerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));
                    procedurePerformerComponents.add(procedurePerformerComponent);
                }

                for(Resource organization: organizations){
                    Procedure.ProcedurePerformerComponent procedurePerformerComponent = new Procedure.ProcedurePerformerComponent();
                    procedurePerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getId()));
                    procedurePerformerComponents.add(procedurePerformerComponent);
                }

                if (!procedurePerformerComponents.isEmpty()){
                    procedure.setPerformer(procedurePerformerComponents);
                    resources.putAll(procedurePerformers);
                }

            }
        }

        procedure.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PROCEDURE, procedure.getIdentifier()));
        resources.put(procedure.getId(), procedure);
        return resources;
    }


}
