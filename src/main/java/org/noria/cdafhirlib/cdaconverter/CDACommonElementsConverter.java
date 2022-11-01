package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.cda.Person;
import org.eclipse.mdht.uml.cda.*;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.eclipse.mdht.uml.hl7.rim.Participation;
import org.eclipse.mdht.uml.hl7.vocab.SetOperator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.constants.BaseConstants;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.MedicationActivity;
import org.openhealthtools.mdht.uml.cda.consol.MedicationActivity2;
import org.openhealthtools.mdht.uml.cda.consol.ResultObservation2;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class CDACommonElementsConverter {

    private static CDACommonElementsConverter instance;

    //private CDABasicElementsConverter cdaBasicElementsConverter;

    private final CodeMappingProcessor codeMappingProcessor;

    private CDACommonElementsConverter(CodeMappingProcessor codeMappingProcessor) {
        //cdaBasicElementsConverter = cdaBasicElementsConverter;
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public static CDACommonElementsConverter getInstance(CodeMappingProcessor codeMappingProcessor) {
        if (instance == null) {
            instance = new CDACommonElementsConverter(codeMappingProcessor);
        }

        return instance;
    }


    public Map<String, Resource> convertAuthor(Author author) {
        Map<String, Resource> resources = new HashMap<>();
        Practitioner practitioner = new Practitioner();
        PractitionerRole practitionerRole = new PractitionerRole();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);

        if (author.getAssignedAuthor() != null) {

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getIds())) {
                author.getAssignedAuthor().getIds().forEach(e -> practitioner.getIdentifier().add(cdaBasicElementsConverter.createFHIRIdentifier(e)));
                practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
                practitionerRole.getIdentifier().addAll(practitioner.getIdentifier());
            }

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getAddrs())) {
                practitioner.setAddress(author.getAssignedAuthor().getAddrs().stream().map(cdaBasicElementsConverter::createFHIRAddress).collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getTelecoms())) {
                practitioner.setTelecom(author.getAssignedAuthor().getTelecoms().stream().map(cdaBasicElementsConverter::createContactPoint).collect(Collectors.toList()));
            }


            if (author.getAssignedAuthor().getAssignedPerson() != null) {

                if (author.getAssignedAuthor().getAssignedPerson() != null && author.getAssignedAuthor().getAssignedPerson().getNames() != null) {
                    practitioner.setName(author.getAssignedAuthor().getAssignedPerson().getNames().stream().map(cdaBasicElementsConverter::createFHIRHumanName).collect(Collectors.toList()));
                }

                if (author.getAssignedAuthor().getRepresentedOrganization() != null) {
                    Organization organization = this.createFHIROrganization(author.getAssignedAuthor().getRepresentedOrganization());
                    resources.put(organization.getId(), organization);
                    log.info("FHIR Organization created from CDA Author");
                    if (StringUtils.isNoneBlank(practitioner.getId())) {
                        practitionerRole.setTelecom(practitioner.getTelecom());
                        practitionerRole.getTelecom().addAll(organization.getTelecom());
                        practitionerRole.setOrganization(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getId()));
                        practitionerRole.setPractitioner(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));
                        practitionerRole.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONERROLE, null));
                        resources.put(practitionerRole.getId(), practitionerRole);
                        log.info("FHIR PractitionerRole created from CDA Author");
                    }
                }

            }

            if (StringUtils.isNoneBlank(practitioner.getId())) {
                resources.put(practitioner.getId(), practitioner);
                log.info("FHIR Practitioner created from CDA Author");
            }
        }

        return resources;
    }

    public Map<String, Resource> convertPerformer(Participation performer, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        Practitioner practitioner = new Practitioner();
        List<PractitionerRole> practitionerRoles = new ArrayList<>();
        List<Location> locations = new ArrayList<>();
        List<Organization> organizations = new ArrayList<>();
        AssignedEntity assignedEntity;
        if (performer instanceof Performer1) {
            assignedEntity = ((Performer1) performer).getAssignedEntity();
        } else {
            assignedEntity = ((Performer2) performer).getAssignedEntity();
        }


        if (assignedEntity != null) {
            if (CollectionUtils.isNotEmpty(assignedEntity.getIds())) {
                List<Identifier> identifiers = assignedEntity.getIds().stream().map(cdaBasicElementsConverter::createFHIRIdentifier).collect(Collectors.toList());
                Practitioner existingPractitoner = ConvertedElementsHelper.findPractitionerByIdentifier(identifiers, headerResources);
                if (existingPractitoner != null) {
                    practitioner = existingPractitoner;
                    resources.put(existingPractitoner.getId(), existingPractitoner);
                    log.info("FHIR Practitioner found in Header");

                } else {
                    practitioner.getIdentifier().addAll(assignedEntity.getIds().stream().map(cdaBasicElementsConverter::createFHIRIdentifier).filter(Objects::nonNull).collect(Collectors.toList()));
                }
            }

            if (resources.isEmpty() && CollectionUtils.isNotEmpty(assignedEntity.getAddrs())) {
                practitioner.setAddress(assignedEntity.getAddrs().stream().map(cdaBasicElementsConverter::createFHIRAddress).filter(Objects::nonNull).collect(Collectors.toList()));
            }

            if (resources.isEmpty() && CollectionUtils.isNotEmpty(assignedEntity.getTelecoms())) {
                practitioner.setTelecom(assignedEntity.getTelecoms().stream().map(cdaBasicElementsConverter::createContactPoint).filter(Objects::nonNull).collect(Collectors.toList()));
            }

            if (resources.isEmpty() && assignedEntity.getAssignedPerson() != null) {
                if (CollectionUtils.isNotEmpty(assignedEntity.getAssignedPerson().getNames())) {
                    practitioner.setName(assignedEntity.getAssignedPerson().getNames().stream().map(cdaBasicElementsConverter::createFHIRHumanName).filter(Objects::nonNull).collect(Collectors.toList()));
                }
            }

            if (CollectionUtils.isNotEmpty(assignedEntity.getRepresentedOrganizations())) {
                organizations = assignedEntity.getRepresentedOrganizations().stream().map(this::createFHIROrganization).filter(Objects::nonNull).collect(Collectors.toList());
                log.info("FHIR Organization created from CDA Performer");
            }

        }

        if (resources.isEmpty()) {
            practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
            resources.put(practitioner.getId(), practitioner);
            log.info("FHIR Practitioner created from CDA Performer");
        }

        if (CollectionUtils.isNotEmpty(practitioner.getAddress())) {
            practitioner.getAddress().forEach(address -> {
                Location location = new Location();
                location.setAddress(address);
                location.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.LOCATION, location.getIdentifier()));
                locations.add(location);
                resources.put(location.getId(), location);
                log.info("FHIR Location created from CDA Performer");
            });

        }

        CodeableConcept codeableConcept = null;
        if (performer instanceof Performer1) {
            Performer1 performerWithFC = (Performer1) performer;
            codeableConcept = cdaBasicElementsConverter.createFHIRCodeableConcept(performerWithFC.getFunctionCode(), null);
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            for (Organization organization : organizations) {
                practitionerRoles.add(this.createPractitionerRole(codeableConcept, practitioner, locations, organization));
            }
        } else {
            practitionerRoles.add(this.createPractitionerRole(codeableConcept, practitioner, locations, null));
        }
        log.info("FHIR PractitionerRole created from CDA Performer");


        practitionerRoles.forEach(pr -> resources.put(pr.getId(), pr));
        organizations.forEach(org -> resources.put(org.getId(), org));

        return resources;
    }


    Map<String, Resource> convertPatient(PatientRole cdaPatientRole) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        Patient patient = new Patient();
        if (CollectionUtils.isNotEmpty(cdaPatientRole.getIds())) {
            cdaPatientRole.getIds().forEach(id -> patient.getIdentifier().add(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        patient.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PATIENT, patient.getIdentifier()));

        if (CollectionUtils.isNotEmpty(cdaPatientRole.getAddrs())) {
            cdaPatientRole.getAddrs().forEach(address -> patient.getAddress().add(cdaBasicElementsConverter.createFHIRAddress(address)));
        }

        if (CollectionUtils.isNotEmpty(cdaPatientRole.getTelecoms())) {
            cdaPatientRole.getTelecoms().forEach(tel -> patient.getTelecom().add(cdaBasicElementsConverter.createContactPoint(tel)));
        }

        if (cdaPatientRole.getPatient() != null) {
            org.eclipse.mdht.uml.cda.Patient cdaPatient = cdaPatientRole.getPatient();
            if (CollectionUtils.isNotEmpty(cdaPatient.getNames())) {
                cdaPatient.getNames().forEach(name -> patient.getName().add(cdaBasicElementsConverter.createFHIRHumanName(name)));
            }

            try {
                patient.setGender(cdaBasicElementsConverter.createGender(cdaPatient.getAdministrativeGenderCode()));
            } catch (FHIRException e) {
                log.error("Unknown Gender Code", e);
            }

            patient.setMaritalStatus(cdaBasicElementsConverter.createFHIRCodeableConcept(cdaPatient.getMaritalStatusCode(), null));

            if (cdaPatient.getReligiousAffiliationCode() != null) {
                patient.getExtension().add(cdaBasicElementsConverter.createExtension(cdaPatient.getReligiousAffiliationCode(), BaseConstants.USCORE_EXTENSION_URL));
            }

            if (cdaPatient.getRaceCode() != null) {
                patient.getExtension().add(cdaBasicElementsConverter.createExtension(cdaPatient.getRaceCode(), BaseConstants.USCORE_EXTENSION_URL));
            }

            if (cdaPatient.getBirthplace() != null && cdaPatient.getBirthplace().getPlace() != null) {
                if (CollectionUtils.isNotEmpty(cdaPatient.getBirthplace().getPlace().getAddrs())) {
                    cdaPatient.getBirthplace().getPlace().getAddrs().forEach(ad -> patient.getExtension().add(cdaBasicElementsConverter.createExtension(ad, BaseConstants.BIRTHPLACE_EXTENSION_URL)));
                }
            }

            if (CollectionUtils.isNotEmpty(cdaPatient.getLanguageCommunications())) {
                List<Patient.PatientCommunicationComponent> patientCommunicationComponents = cdaPatient.getLanguageCommunications().stream()
                        .map(languageCommunication -> new Patient.PatientCommunicationComponent(cdaBasicElementsConverter.createFHIRCodeableConcept(languageCommunication.getLanguageCode(), null)))
                        .collect(Collectors.toList());
                patient.setCommunication(patientCommunicationComponents);
            }

        }

        resources.put(patient.getId(), patient);

        if (cdaPatientRole.getProviderOrganization() != null) {
            Organization organization = this.createFHIROrganization(cdaPatientRole.getProviderOrganization());
            resources.put(organization.getId(), organization);
        }

        return resources;
    }

    Map<String, Resource> convertCustodian(Custodian custodian) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        if (custodian.getAssignedCustodian() != null && custodian.getAssignedCustodian().getRepresentedCustodianOrganization() != null) {
            CustodianOrganization custodianOrganization = custodian.getAssignedCustodian().getRepresentedCustodianOrganization();

            Organization organization = new Organization();
            if (CollectionUtils.isNotEmpty(custodianOrganization.getIds())) {
                custodianOrganization.getIds().forEach(e -> organization.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(e)));
            }

            if (custodianOrganization.getNames() != null) {
                organization.setName(custodianOrganization.getNames().stream().map(EN::getText).collect(Collectors.joining(",")));
            }

            if (CollectionUtils.isNotEmpty(custodianOrganization.getAddrs())) {
                organization.setAddress(custodianOrganization.getAddrs().stream().map(cdaBasicElementsConverter::createFHIRAddress).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(custodianOrganization.getTelecoms())) {
                organization.setTelecom(custodianOrganization.getTelecoms().stream().map(cdaBasicElementsConverter::createContactPoint).collect(Collectors.toList()));
            }
            organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
            resources.put(organization.getId(), organization);
        }

        return resources;
    }

    Map<String, Resource> convertParticipant(Participant1 participant) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        if (participant.getAssociatedEntity() != null && !participant.getAssociatedEntity().isSetNullFlavor()) {
            Practitioner practitioner = new Practitioner();
            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getIds())) {
                participant.getAssociatedEntity().getIds().forEach(id -> practitioner.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
            }

            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getAddrs())) {
                practitioner.setAddress(participant.getAssociatedEntity().getAddrs().stream().map(cdaBasicElementsConverter::createFHIRAddress).collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getTelecoms())) {
                practitioner.setTelecom(participant.getAssociatedEntity().getTelecoms().stream().map(cdaBasicElementsConverter::createContactPoint).collect(Collectors.toList()));
            }

            if (participant.getAssociatedEntity().getAssociatedPerson() != null && !participant.getAssociatedEntity().getAssociatedPerson().isSetNullFlavor()) {
                Person person = participant.getAssociatedEntity().getAssociatedPerson();
                if (CollectionUtils.isNotEmpty(person.getNames())) {
                    person.getNames().forEach(name -> practitioner.getName().add(cdaBasicElementsConverter.createFHIRHumanName(name)));
                }
            }

            practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
            resources.put(practitioner.getId(), practitioner);
        }

        return resources;
    }

    public Map<String, Resource> convertSectionAuthor(Author cdaAuthor, Map<String, Resource> headerResources) {
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        if (cdaAuthor.getAssignedAuthor() != null && !cdaAuthor.getAssignedAuthor().isSetNullFlavor() && CollectionUtils.isNotEmpty(cdaAuthor.getAssignedAuthor().getIds())) {
            List<Identifier> identifiers = cdaAuthor.getAssignedAuthor().getIds().stream().map(cdaBasicElementsConverter::createFHIRIdentifier).collect(Collectors.toList());
            Practitioner existingPractitoner = ConvertedElementsHelper.findPractitionerByIdentifier(identifiers, headerResources);
            if (existingPractitoner != null) {
                Map<String, Resource> resources = new HashMap<>();
                resources.put(existingPractitoner.getId(), existingPractitoner);
                return resources;
            }
        }

        return this.convertAuthor(cdaAuthor);

    }


    public Organization createFHIROrganization(org.eclipse.mdht.uml.cda.Organization cdaOrganization) {
        Organization organization = new Organization();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        if (CollectionUtils.isNotEmpty(cdaOrganization.getIds())) {
            cdaOrganization.getIds().forEach(e -> organization.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(e)));
        }

        if (cdaOrganization.getNames() != null) {
            organization.setName(cdaOrganization.getNames().stream().map(ON::getText).collect(Collectors.joining(",")));
        }

        if (CollectionUtils.isNotEmpty(cdaOrganization.getAddrs())) {
            organization.setAddress(cdaOrganization.getAddrs().stream().map(cdaBasicElementsConverter::createFHIRAddress).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(cdaOrganization.getTelecoms())) {
            organization.setTelecom(cdaOrganization.getTelecoms().stream().map(cdaBasicElementsConverter::createContactPoint).collect(Collectors.toList()));
        }

        organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
        return organization;
    }

    public Map<String, Resource> convertObservation(org.eclipse.mdht.uml.cda.Observation cdaObservation, ObservationCategory observationCategory, Map<String, Resource> outerResources, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        Observation observation = new Observation();
        if (CollectionUtils.isNotEmpty(cdaObservation.getIds())) {
            cdaObservation.getIds().forEach(id -> observation.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        if (observationCategory != null) {
            CodeableConcept codeableConcept = new CodeableConcept();
            Coding coding = new Coding();
            coding.setSystem(observationCategory.getSystem());
            coding.setCode(observationCategory.toCode());
            coding.setDisplay(observationCategory.getDisplay());
            codeableConcept.setCoding(Collections.singletonList(coding));
            observation.setCategory(Collections.singletonList(codeableConcept));
        }

        if (cdaObservation.getEffectiveTime() != null) {
            Type recordedDate = cdaBasicElementsConverter.convertIVLTSDate(cdaObservation.getEffectiveTime());
            observation.setEffective(recordedDate);
        }

        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            observation.setSubject(reference);
        }

        if (cdaObservation.getCode() != null) {
            observation.setCode(cdaBasicElementsConverter.createFHIRCodeableConcept(cdaObservation.getCode(), null));
        }

        Coding coding = this.createObservationStatusCoding(cdaObservation);
        if (coding != null) {
            try {
                observation.setStatus(Observation.ObservationStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (!cdaObservation.getValues().isEmpty()) {
            if (cdaObservation.getValues().get(0) instanceof CD) {
                observation.setValue(cdaBasicElementsConverter.createFHIRCodeableConcept((CD) cdaObservation.getValues().get(0), null));
            }

            if (cdaObservation.getValues().get(0) instanceof PQ) {
                observation.setValue(cdaBasicElementsConverter.createSimpleQuantity((PQ) cdaObservation.getValues().get(0)));
            }
        }

        if (!cdaObservation.getInterpretationCodes().isEmpty()) {
            cdaObservation.getInterpretationCodes().forEach(ic -> observation.getInterpretation().add(cdaBasicElementsConverter.createFHIRCodeableConcept(ic, null)));
        }

        if (!cdaObservation.getMethodCodes().isEmpty()) {
            observation.setMethod(cdaBasicElementsConverter.createFHIRCodeableConcept(cdaObservation.getMethodCodes().get(0), null));
        }

        if (!cdaObservation.getTargetSiteCodes().isEmpty()) {
            observation.setBodySite(cdaBasicElementsConverter.createFHIRCodeableConcept(cdaObservation.getTargetSiteCodes().get(0), null));
        }

        if (!cdaObservation.getAuthors().isEmpty()) {
            resources.putAll(this.convertAuthors(observation, cdaObservation.getAuthors(), headerResources));
        } else if (!outerResources.isEmpty()) {
            observation.setPerformer(outerResources.values().stream().filter(v -> v instanceof Practitioner).map(
                    ra ->
                            FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, ra.getId())
            ).collect(Collectors.toList()));
        }

        if (!cdaObservation.getReferenceRanges().isEmpty()) {
            cdaObservation.getReferenceRanges().forEach(rr -> {
                if (rr.getObservationRange() != null && rr.getObservationRange().getValue() instanceof IVL_PQ) {
                    IVL_PQ refRangeValue = (IVL_PQ) rr.getObservationRange().getValue();
                    Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent = new Observation.ObservationReferenceRangeComponent();
                    observationReferenceRangeComponent.setLow(cdaBasicElementsConverter.createSimpleQuantity(refRangeValue.getLow()));
                    observationReferenceRangeComponent.setHigh(cdaBasicElementsConverter.createSimpleQuantity(refRangeValue.getHigh()));
                    observation.getReferenceRange().add(observationReferenceRangeComponent);
                }
            });
        }

        observation.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.OBSERVATION, observation.getIdentifier()));
        resources.put(observation.getId(), observation);
        return resources;
    }

    public Map<String, Resource> convertAuthors(Resource fhirResource, List<Author> cdaAuthors, Map<String, Resource> headerResources) {
        Map<String, Resource> authors = new HashMap<>();
        cdaAuthors.forEach(author -> authors.putAll(this.convertSectionAuthor(author, headerResources)));
        if (!authors.isEmpty()) {
            if (fhirResource != null) {
                if (fhirResource instanceof DiagnosticReport) {
                    ((DiagnosticReport) fhirResource).setPerformer(authors.values().stream().filter(v -> v instanceof Practitioner).map(
                            ra ->
                                    FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, ra.getId())
                    ).collect(Collectors.toList()));
                } else if (fhirResource instanceof Observation) {
                    ((Observation) fhirResource).setPerformer(authors.values().stream().filter(v -> v instanceof Practitioner).map(
                            ra ->
                                    FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, ra.getId())
                    ).collect(Collectors.toList()));
                } else if (fhirResource instanceof AllergyIntolerance) {
                    authors.values().stream().filter(r -> r instanceof Practitioner).findFirst().ifPresent(practitioner ->
                            ((AllergyIntolerance) fhirResource).setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId())));
                } else if (fhirResource instanceof MedicationRequest) {
                    authors.values().stream().filter(r -> r instanceof Practitioner).findFirst().ifPresent(practitioner ->
                            ((MedicationRequest) fhirResource).setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId())));
                } else if (fhirResource instanceof MedicationStatement) {
                    authors.values().stream().filter(r -> r instanceof Practitioner).findFirst().ifPresent(practitioner ->
                            ((MedicationStatement) fhirResource).setInformationSource(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId())));
                } else if (fhirResource instanceof Condition) {
                    authors.values().stream().filter(r -> r instanceof Practitioner).findFirst().ifPresent(practitioner ->
                            ((Condition) fhirResource).setRecorder(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId())));
                }
            }
        }

        return authors;
    }


    public Map<String, Resource> convertProcedure(org.eclipse.mdht.uml.cda.Procedure procedureActivityProcedure, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        Procedure procedure = new Procedure();
        if (CollectionUtils.isNotEmpty(procedureActivityProcedure.getIds())) {
            procedureActivityProcedure.getIds().forEach(id -> procedure.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        if (procedureActivityProcedure.getEffectiveTime() != null && !procedureActivityProcedure.getEffectiveTime().isSetNullFlavor()) {
            Type performedDate = cdaBasicElementsConverter.convertIVLTSDate(procedureActivityProcedure.getEffectiveTime());
            procedure.setPerformed(performedDate);
        }

        if (procedureActivityProcedure.getCode() != null && !procedureActivityProcedure.getCode().isSetNullFlavor()) {
            procedure.setCode(cdaBasicElementsConverter.createFHIRCodeableConcept(procedureActivityProcedure.getCode(), null));
        }

        if (procedureActivityProcedure.getStatusCode() != null && !procedureActivityProcedure.getStatusCode().isSetNullFlavor()) {
            try {
                Coding procedureStatusCoding = cdaBasicElementsConverter.createFHIRCoding(procedureActivityProcedure.getStatusCode(), CDAtoFHIRCodeConversionType.PROCEDURE_STATUS.toValue());
                procedure.setStatus(Procedure.ProcedureStatus.fromCode(procedureStatusCoding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (!procedureActivityProcedure.getTargetSiteCodes().isEmpty()) {
            List<CodeableConcept> bodySite = procedureActivityProcedure.getTargetSiteCodes().stream().map(cd -> cdaBasicElementsConverter.createFHIRCodeableConcept(cd, null)).collect(Collectors.toList());
            procedure.setBodySite(bodySite);
        }

        if (!procedureActivityProcedure.getSpecimens().isEmpty()) {
            List<Coding> outcomeCodingList = procedureActivityProcedure.getSpecimens().stream()
                    .filter(specimen -> !specimen.isSetNullFlavor() && specimen.getSpecimenRole() != null && !specimen.getSpecimenRole().isSetNullFlavor())
                    .filter(specimen -> specimen.getSpecimenRole().getSpecimenPlayingEntity() != null && !specimen.getSpecimenRole().getSpecimenPlayingEntity().isSetNullFlavor())
                    .map(specimen -> cdaBasicElementsConverter.createFHIRCoding(specimen.getSpecimenRole().getSpecimenPlayingEntity().getCode(), null))
                    .collect(Collectors.toList());
            CodeableConcept outcome = new CodeableConcept();
            outcome.setCoding(outcomeCodingList);
            procedure.setOutcome(outcome);
        }

        if (CollectionUtils.isNotEmpty(procedureActivityProcedure.getPerformers())) {
            Map<String, Resource> procedurePerformers = new HashMap<>();
            procedureActivityProcedure.getPerformers().forEach(performer -> procedurePerformers.putAll(this.convertPerformer(performer, headerResources)));
            if (!procedurePerformers.isEmpty()) {
                List<Resource> practitioners = procedurePerformers.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations = procedurePerformers.values().stream().filter(r -> r instanceof Organization).collect(Collectors.toList());

                List<Procedure.ProcedurePerformerComponent> procedurePerformerComponents = new ArrayList<>();
                for (Resource practitioner : practitioners) {
                    Procedure.ProcedurePerformerComponent procedurePerformerComponent = new Procedure.ProcedurePerformerComponent();
                    procedurePerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));
                    procedurePerformerComponents.add(procedurePerformerComponent);
                }

                for (Resource organization : organizations) {
                    Procedure.ProcedurePerformerComponent procedurePerformerComponent = new Procedure.ProcedurePerformerComponent();
                    procedurePerformerComponent.setActor(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getId()));
                    procedurePerformerComponents.add(procedurePerformerComponent);
                }

                if (!procedurePerformerComponents.isEmpty()) {
                    procedure.setPerformer(procedurePerformerComponents);
                    resources.putAll(procedurePerformers);
                }

            }
        }

        procedure.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PROCEDURE, procedure.getIdentifier()));
        resources.put(procedure.getId(), procedure);
        return resources;
    }


    private PractitionerRole createPractitionerRole(CodeableConcept codeableConcept, Practitioner practitioner, List<Location> locations, Organization organization) {
        PractitionerRole practitionerRole = new PractitionerRole();
        if (codeableConcept != null) {
            practitionerRole.getSpecialty().add(codeableConcept);
        }
        List<String> resourcesIds = new LinkedList<>();

        locations.forEach(loc -> practitionerRole.getLocation().add(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.LOCATION, loc.getId())));
        if (organization != null) {
            practitionerRole.setOrganization(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getId()));
            resourcesIds.add(organization.getId());
        }

        if (practitioner != null) {
            practitionerRole.setPractitioner(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));
            practitionerRole.getTelecom().addAll(practitioner.getTelecom());
            resourcesIds.add(practitioner.getId());
        }

        Identifier identifier = new Identifier();
        identifier.setValue(String.join("_", resourcesIds));
        practitionerRole.getIdentifier().add(identifier);
        practitionerRole.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONERROLE, Collections.singletonList(identifier)));
        return practitionerRole;
    }


    public Map<String, Resource> convertToMedicationStatement(SubstanceAdministration medicationActivity, Map<String, Resource> headerResources) {
        Map<String, Resource> resources = new HashMap<>();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        MedicationStatement medicationStatement = new MedicationStatement();
        if (CollectionUtils.isNotEmpty(medicationActivity.getIds())) {
            medicationActivity.getIds().forEach(id -> medicationStatement.addIdentifier(cdaBasicElementsConverter.createFHIRIdentifier(id)));
        }

        medicationStatement.getDosage().add(this.processDosage(medicationActivity));

        if (medicationActivity.getConsumable() != null && medicationActivity.getConsumable().getManufacturedProduct() != null && medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
            medicationStatement.setMedication(cdaBasicElementsConverter.createFHIRCodeableConcept(medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial().getCode(), null));

        }

        if (!medicationActivity.getPerformers().isEmpty()) {
            Map<String, Resource> medicationRequestPerformers = new HashMap<>();
            medicationActivity.getPerformers().forEach(performer -> medicationRequestPerformers.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertPerformer(performer, headerResources)));
            if (!medicationRequestPerformers.isEmpty()) {
                List<Resource> practitioners = medicationRequestPerformers.values().stream().filter(r -> r instanceof Practitioner).collect(Collectors.toList());
                List<Resource> organizations = medicationRequestPerformers.values().stream().filter(r -> r instanceof Organization).collect(Collectors.toList());
                if (!practitioners.isEmpty()) {
                    medicationStatement.setInformationSource(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioners.get(0).getId()));
                } else if (!organizations.isEmpty()) {
                    medicationStatement.setInformationSource(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organizations.get(0).getId()));
                }

                resources.putAll(medicationRequestPerformers);
            }
        } else if (!medicationActivity.getAuthors().isEmpty()) {
            resources.putAll(CDACommonElementsConverter.getInstance(this.codeMappingProcessor).convertAuthors(medicationStatement, medicationActivity.getAuthors(), headerResources));
        }

        medicationStatement.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.MEDICATIONSTATEMENT, medicationStatement.getIdentifier()));
        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            medicationStatement.setSubject(reference);
        }

        Coding coding = cdaBasicElementsConverter.createFHIRCoding(medicationActivity.getStatusCode(), CDAtoFHIRCodeConversionType.MEDICATION_ACTIVITY_STATEMENT_STATUS.toValue());
        if (coding != null) {
            try {
                medicationStatement.setStatus(MedicationStatement.MedicationStatementStatus.fromCode(coding.getCode()));
            } catch (FHIRException e) {
                log.error(e.getMessage(), e);
            }
        }

        resources.put(medicationStatement.getId(), medicationStatement);
        return resources;
    }

    public Dosage processDosage(SubstanceAdministration medicationActivity) {
        Dosage dosage = new Dosage();
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        medicationActivity.getEffectiveTimes().forEach(et -> {

            if (et.isSetOperator() && et.getOperator().equals(SetOperator.A)) {
                if (et instanceof EIVL_TS) {
                    dosage.setTiming(cdaBasicElementsConverter.convertEIVL_TStoFHIRTiming((EIVL_TS) et));
                } else if (et instanceof PIVL_TS) {
                    dosage.setTiming(cdaBasicElementsConverter.convertPIVL_TStoFHIRTiming((PIVL_TS) et));
                }
            }
        });

        if (medicationActivity.getRepeatNumber() != null && medicationActivity.getRepeatNumber().getValue() != null) {
            dosage.getTiming().getRepeat().setCount(medicationActivity.getRepeatNumber().getValue().intValue());
        }

        dosage.setRoute(cdaBasicElementsConverter.createFHIRCodeableConcept(medicationActivity.getRouteCode(), null));
        dosage.setSite(cdaBasicElementsConverter.createFHIRCodeableConceptFromList(medicationActivity.getApproachSiteCodes(), null));

        Dosage.DosageDoseAndRateComponent dosageDoseAndRateComponent = new Dosage.DosageDoseAndRateComponent();


        if (medicationActivity.getRateQuantity() != null && medicationActivity.getRateQuantity().getValue() != null) {
            dosageDoseAndRateComponent.setRate(cdaBasicElementsConverter.createSimpleQuantity(medicationActivity.getRateQuantity()));
        } else if (medicationActivity.getRateQuantity() != null && medicationActivity.getRateQuantity().getLow() != null && medicationActivity.getRateQuantity().getHigh() != null) {
            dosageDoseAndRateComponent.setRate(cdaBasicElementsConverter.createRange(medicationActivity.getRateQuantity()));
        } else if (medicationActivity.getMaxDoseQuantity() != null) {
            dosageDoseAndRateComponent.setRate(cdaBasicElementsConverter.createRatio(medicationActivity.getMaxDoseQuantity()));
        }

        if (medicationActivity.getDoseQuantity() != null && medicationActivity.getDoseQuantity().getValue() != null) {
            dosageDoseAndRateComponent.setDose(cdaBasicElementsConverter.createSimpleQuantity(medicationActivity.getDoseQuantity()));
        } else if (medicationActivity.getDoseQuantity() != null) {
            dosageDoseAndRateComponent.setDose(cdaBasicElementsConverter.createRange(medicationActivity.getDoseQuantity()));
        }

        dosage.getDoseAndRate().add(dosageDoseAndRateComponent);

        return dosage;
    }


    private Coding createObservationStatusCoding(org.eclipse.mdht.uml.cda.Observation cdaObservation) {
        CDABasicElementsConverter cdaBasicElementsConverter = CDABasicElementsConverter.getInstance(this.codeMappingProcessor);
        if (cdaObservation instanceof ResultObservation2) {
            return cdaBasicElementsConverter.createFHIRCoding(cdaObservation.getStatusCode(), CDAtoFHIRCodeConversionType.RESULT_STATUS.toValue());
        } else {
            return cdaBasicElementsConverter.createFHIRCoding(cdaObservation.getStatusCode(), CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
        }
    }


}
