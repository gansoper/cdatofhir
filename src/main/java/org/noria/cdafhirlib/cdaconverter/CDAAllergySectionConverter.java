package org.noria.cdafhirlib.cdaconverter;

import ca.uhn.fhir.model.api.IElement;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.AllergiesSection;
import org.openhealthtools.mdht.uml.cda.consol.AllergyObservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CDAAllergySectionConverter {

    private final BasicCDAElementsConverter basicCDAElementsConverter;

    public CDAAllergySectionConverter(BasicCDAElementsConverter basicCDAElementsConverter) {
        this.basicCDAElementsConverter = basicCDAElementsConverter;
    }

    public Map<String, IBaseResource> convertAllergies(AllergiesSection allergiesSection, Map<String, IBaseResource> headerResources) {
        Map<String, IBaseResource> resources = new HashMap<>();
        List<AllergyObservation> allergyObservationList = new ArrayList<>();
        allergiesSection.getAllergyProblemActs().forEach(act -> {
            allergyObservationList.addAll(act.getAllergyObservations());
            act.getAuthors().forEach(author -> resources.putAll(this.basicCDAElementsConverter.convertAuthor(author)));
            act.getAllergyObservations().forEach(allergyObservation -> resources.putAll(this.convertCDAAllergyObservation(allergyObservation, act.getEffectiveTime())));
        });

        return resources;
    }

    private Map<String, IBaseResource> convertCDAAllergyObservation(AllergyObservation allergyObservation, IVL_TS recordedTime) {
        Map<String, IBaseResource> resources = new HashMap<>();
        AllergyIntolerance allergy = new AllergyIntolerance();
        Type recordedDate = this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertIVLTSDate(recordedTime);
        if (recordedDate instanceof DateTimeType){
            allergy.setRecordedDateElement((DateTimeType)recordedDate);
        }
        else if (recordedDate instanceof Period){
            allergy.setRecordedDateElement(((Period) recordedDate).getStartElement());
        }

        if (CollectionUtils.isNotEmpty(allergyObservation.getIds())){
            allergyObservation.getIds().forEach(id -> allergy.addIdentifier(this.basicCDAElementsConverter.getSimpleCDATypesConverter().createFHIRIdentifier(id)));
        }

        allergy.setOnset(this.basicCDAElementsConverter.getSimpleCDATypesConverter().convertIVLTSDate(allergyObservation.getEffectiveTime()));

        return resources;
    }

    private Map<String, IBaseResource> convertAllergyAuthor(Author cdaAuthor, Map<String, IBaseResource> headerResources){
        if (cdaAuthor.getAssignedAuthor()!= null && !cdaAuthor.getAssignedAuthor().isSetNullFlavor() && CollectionUtils.isNotEmpty(cdaAuthor.getAssignedAuthor().getIds())){
            List<Identifier> identifiers = cdaAuthor.getAssignedAuthor().getIds().stream().map(this.basicCDAElementsConverter.getSimpleCDATypesConverter()::createFHIRIdentifier).collect(Collectors.toList());
            Practitioner existingPractitoner = ConvertedElementsHelper.findPractitionerByAuthor(identifiers, headerResources);
            if (existingPractitoner != null){
                Map<String, IBaseResource> resources = new HashMap<>();
                resources.put(existingPractitoner.getId(), existingPractitoner);
                return resources;
            }
        }

        return this.basicCDAElementsConverter.convertAuthor(cdaAuthor);

    }
}
