package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.ResultObservation2;
import org.openhealthtools.mdht.uml.cda.consol.ResultOrganizer2;
import org.openhealthtools.mdht.uml.cda.consol.ResultsSection2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class CDAResultsSectionConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    public CDAResultsSectionConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public Map<String, Resource> convertResult(ResultsSection2 resultsSection, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        resultsSection.getConsolResultOrganizer2s().forEach(resultOrganizer -> resources.putAll(this.convertResultOrganizer(resultOrganizer, headerResources)));
        return resources;
    }

    private Map<String, Resource> convertResultOrganizer(ResultOrganizer2 resultOrganizer, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        if (CollectionUtils.isNotEmpty(resultOrganizer.getIds())) {
            resultOrganizer.getIds().forEach(id -> diagnosticReport.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        Coding coding = cdaBasicElementsConverter.createFHIRCoding(resultOrganizer.getStatusCode(), CDAtoFHIRCodeConversionType.RESULT_STATUS.toValue());
        if (coding != null) {
            try {
                diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (resultOrganizer.getCode() != null) {
            diagnosticReport.setCode(cdaBasicElementsConverter.createFHIRCodeableConcept(resultOrganizer.getCode(), null));
        }

        if (resultOrganizer.getEffectiveTime() != null) {
            diagnosticReport.setEffective(cdaBasicElementsConverter.convertIVLTSDate(resultOrganizer.getEffectiveTime()));
        }

        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            diagnosticReport.setSubject(reference);
        }

        if (!resultOrganizer.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthors(diagnosticReport, resultOrganizer.getAuthors(), headerResources));
        }

        diagnosticReport.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.DIAGNOSTICREPORT, diagnosticReport.getIdentifier()));

        if (!resultOrganizer.getResultObservations().isEmpty()) {
            Map<String, Resource> observationResources = new HashMap<>();
            for (ResultObservation2 ro : resultOrganizer.getConsolResultObservation2s()) {
                observationResources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).createFHIRObservation(ro, ObservationCategory.EXAM, resources, headerResources));
            }
            List<Resource> observations = observationResources.values().stream().filter(r -> r instanceof Observation).collect(Collectors.toList());
            observations.forEach(o -> {
                diagnosticReport.getResult().add(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.OBSERVATION, o.getId()));
            });
            resources.putAll(observationResources);
        }

        resources.put(diagnosticReport.getId(), diagnosticReport);
        return resources;
    }

}
