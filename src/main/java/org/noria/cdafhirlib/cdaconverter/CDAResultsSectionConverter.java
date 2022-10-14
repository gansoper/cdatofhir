package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.hl7.datatypes.CD;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_PQ;
import org.eclipse.mdht.uml.hl7.datatypes.PQ;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class CDAResultsSectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAResultsSectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, Resource> convertResult(ResultsSection2 resultsSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        resultsSection.getConsolResultOrganizer2s().forEach(resultOrganizer -> resources.putAll(this.convertResultOrganizer(resultOrganizer, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertResultOrganizer(ResultOrganizer2 resultOrganizer, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        if (CollectionUtils.isNotEmpty(resultOrganizer.getIds())) {
            resultOrganizer.getIds().forEach(id -> diagnosticReport.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        Coding coding = basicCDAElementsConverter.createFHIRCoding(resultOrganizer.getStatusCode(), CDAtoFHIRCodeConversionType.RESULT_STATUS.toValue());
        if (coding != null) {
            try {
                diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (resultOrganizer.getCode() != null){
            diagnosticReport.setCode(this.basicCDAElementsConverter.createFHIRCodeableConcept(resultOrganizer.getCode(), null));
        }

        Reference reference = ConvertedElementsHelper.getPateintReference(headerResources);
        if (reference != null) {
            diagnosticReport.setSubject(reference);
        }

        if (!resultOrganizer.getAuthors().isEmpty()) {
            resources.putAll(this.convertResultAuthors(diagnosticReport, resultOrganizer.getAuthors(), resources, headerResources));
        }

        diagnosticReport.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.DOCUMENTREFERENCE, diagnosticReport.getIdentifier()));

        if (!resultOrganizer.getResultObservations().isEmpty()){
            List<Observation> observations = resultOrganizer.getConsolResultObservation2s().stream().map(ro->createResultObservation(ro, resources, headerResources)).collect(Collectors.toList());
            observations.forEach(o->{
                diagnosticReport.getResult().add(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.OBSERVATION, o.getId()));
                resources.put(o.getId(), o);
            });
        }

        resources.put(diagnosticReport.getId(), diagnosticReport);
        return resources;
    }


    private Observation createResultObservation(ResultObservation2 resultObservation, Map<String, Resource> resources, Map<String, Resource> headerResources) {
        Observation observation = new Observation();
        if (CollectionUtils.isNotEmpty(resultObservation.getIds())) {
            resultObservation.getIds().forEach(id -> observation.addIdentifier(this.basicCDAElementsConverter.createFHIRIdentifier(id)));
        }

        if (resultObservation.getEffectiveTime() != null) {
            Type recordedDate = this.basicCDAElementsConverter.convertIVLTSDate(resultObservation.getEffectiveTime());
            observation.setEffective(recordedDate);
        }

        Reference reference = ConvertedElementsHelper.getPateintReference(headerResources);
        if (reference != null) {
            observation.setSubject(reference);
        }

        Coding coding = basicCDAElementsConverter.createFHIRCoding(resultObservation.getStatusCode(), CDAtoFHIRCodeConversionType.RESULT_STATUS.toValue());
        if (coding != null) {
            try {
                observation.setStatus(Observation.ObservationStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (!resultObservation.getValues().isEmpty()) {
            if (resultObservation.getValues().get(0) instanceof CD) {
                observation.setValue(basicCDAElementsConverter.createFHIRCodeableConcept((CD) resultObservation.getValues().get(0), null));
            }

            if (resultObservation.getValues().get(0) instanceof PQ){
                observation.setValue(basicCDAElementsConverter.createSimpleQuantity((PQ)resultObservation.getValues().get(0)));
            }
        }

        if(!resultObservation.getInterpretationCodes().isEmpty()){
            resultObservation.getInterpretationCodes().forEach(ic -> observation.getInterpretation().add(basicCDAElementsConverter.createFHIRCodeableConcept(ic,null)));
        }

        if(!resultObservation.getMethodCodes().isEmpty()){
            observation.setMethod(basicCDAElementsConverter.createFHIRCodeableConcept(resultObservation.getMethodCodes().get(0),null));
        }

        if(!resultObservation.getTargetSiteCodes().isEmpty()){
            observation.setBodySite(basicCDAElementsConverter.createFHIRCodeableConcept(resultObservation.getTargetSiteCodes().get(0),null));
        }

        if (!resultObservation.getAuthors().isEmpty()) {
            resources.putAll(this.convertResultAuthors(observation, resultObservation.getAuthors(), resources, headerResources));
        }

        if (!resultObservation.getReferenceRanges().isEmpty()){
            resultObservation.getReferenceRanges().forEach(rr -> {
                if (rr.getObservationRange() != null && rr.getObservationRange().getValue() instanceof IVL_PQ) {
                    IVL_PQ refRangeValue = (IVL_PQ) rr.getObservationRange().getValue();
                    Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent = new Observation.ObservationReferenceRangeComponent();
                    observationReferenceRangeComponent.setLow(this.basicCDAElementsConverter.createSimpleQuantity(refRangeValue.getLow()));
                    observationReferenceRangeComponent.setHigh(this.basicCDAElementsConverter.createSimpleQuantity(refRangeValue.getHigh()));
                    observation.getReferenceRange().add(observationReferenceRangeComponent);
                }
            });
        }

        observation.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.OBSERVATION, observation.getIdentifier()));

        return observation;
    }

    private Map<String, Resource> convertResultAuthors(Resource fhirResource, List<Author> authors, Map<String, Resource> headerResources,  Map<String, Resource> resources) {
        Map<String, Resource> resultAuthors = this.basicCDAElementsConverter.convertSectionAuthors(authors, headerResources);
        if (!resultAuthors.isEmpty()) {
            if (fhirResource instanceof DiagnosticReport) {
                ((DiagnosticReport) fhirResource).setPerformer(resultAuthors.values().stream().filter(v -> v instanceof Practitioner).map(
                        ra ->
                                FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, ra.getId())
                ).collect(Collectors.toList()));
            } else if (fhirResource instanceof Observation) {
                ((Observation) fhirResource).setPerformer(resultAuthors.values().stream().filter(v -> v instanceof Practitioner).map(
                        ra ->
                                FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, ra.getId())
                ).collect(Collectors.toList()));

                resources.putAll(resultAuthors);
            }

        }

        return  resources;
    }
}
