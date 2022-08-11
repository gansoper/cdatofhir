package org.noria.cdafhirlib.cdaconverter;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.cda.Person;
import org.eclipse.mdht.uml.cda.*;
import org.eclipse.mdht.uml.hl7.datatypes.AD;
import org.eclipse.mdht.uml.hl7.datatypes.EN;
import org.eclipse.mdht.uml.hl7.datatypes.ON;
import org.eclipse.mdht.uml.hl7.rim.Participation;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.constants.BaseConstants;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Getter
public class BasicCDAElementsConverter {

    private final SimpleCDATypesConverter simpleCDATypesConverter;

    public BasicCDAElementsConverter(SimpleCDATypesConverter simpleCDATypesConverter) {
        this.simpleCDATypesConverter = simpleCDATypesConverter;
    }


    public Map<String, Resource> convertAuthor(Author author) {
        Map<String, Resource> resources = new HashMap<>();
        Practitioner practitioner = new Practitioner();
        PractitionerRole practitionerRole = new PractitionerRole();

        if (author.getAssignedAuthor() != null) {

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getIds())) {
                author.getAssignedAuthor().getIds().forEach(e -> practitioner.getIdentifier().add(this.simpleCDATypesConverter.createFHIRIdentifier(e)));
                practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
                practitionerRole.getIdentifier().addAll(practitioner.getIdentifier());
            }

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getAddrs())) {
                practitioner.setAddress(author.getAssignedAuthor().getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getTelecoms())) {
                practitioner.setTelecom(author.getAssignedAuthor().getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).collect(Collectors.toList()));
            }


            if (author.getAssignedAuthor().getAssignedPerson() != null) {

                if (author.getAssignedAuthor().getAssignedPerson() != null && author.getAssignedAuthor().getAssignedPerson().getNames() != null) {
                    practitioner.setName(author.getAssignedAuthor().getAssignedPerson().getNames().stream().map(this.simpleCDATypesConverter::createFHIRHumanName).collect(Collectors.toList()));
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
                List<Identifier> identifiers = assignedEntity.getIds().stream().map(this.getSimpleCDATypesConverter()::createFHIRIdentifier).collect(Collectors.toList());
                Practitioner existingPractitoner = ConvertedElementsHelper.findPractitionerByIdentifier(identifiers, headerResources);
                if (existingPractitoner != null) {
                    practitioner = existingPractitoner;
                    resources.put(existingPractitoner.getId(), existingPractitoner);
                    log.info("FHIR Practitioner found in Header");

                } else {
                    practitioner.getIdentifier().addAll(assignedEntity.getIds().stream().map(this.simpleCDATypesConverter::createFHIRIdentifier).filter(id->id != null).collect(Collectors.toList()));
                }
            }

            if (resources.isEmpty() && CollectionUtils.isNotEmpty(assignedEntity.getAddrs())) {
                List<Address> addresses = new ArrayList<>();
                practitioner.setAddress(assignedEntity.getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).filter(ad->ad != null).collect(Collectors.toList()));
            }

            if (resources.isEmpty() && CollectionUtils.isNotEmpty(assignedEntity.getTelecoms())) {
                practitioner.setTelecom(assignedEntity.getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).filter(tel->tel != null).collect(Collectors.toList()));
            }

            if (resources.isEmpty() && assignedEntity.getAssignedPerson() != null) {
                if (CollectionUtils.isNotEmpty(assignedEntity.getAssignedPerson().getNames())) {
                    practitioner.setName(assignedEntity.getAssignedPerson().getNames().stream().map(this.simpleCDATypesConverter::createFHIRHumanName).filter(name->name != null).collect(Collectors.toList()));
                }
            }

            if (CollectionUtils.isNotEmpty(assignedEntity.getRepresentedOrganizations())) {
                organizations = assignedEntity.getRepresentedOrganizations().stream().map(this::createFHIROrganization).filter(org->org != null).collect(Collectors.toList());
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
            codeableConcept = this.simpleCDATypesConverter.createFHIRCodeableConcept(performerWithFC.getFunctionCode(), null);
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
        Patient patient = new Patient();
        if (CollectionUtils.isNotEmpty(cdaPatientRole.getIds())) {
            cdaPatientRole.getIds().forEach(id -> patient.getIdentifier().add(this.simpleCDATypesConverter.createFHIRIdentifier(id)));
        }

        patient.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PATIENT, patient.getIdentifier()));

        if (CollectionUtils.isNotEmpty(cdaPatientRole.getAddrs())) {
            cdaPatientRole.getAddrs().forEach(address -> patient.getAddress().add(this.simpleCDATypesConverter.createFHIRAddress(address)));
        }

        if (CollectionUtils.isNotEmpty(cdaPatientRole.getTelecoms())) {
            cdaPatientRole.getTelecoms().forEach(tel -> patient.getTelecom().add(this.simpleCDATypesConverter.createContactPoint(tel)));
        }

        if (cdaPatientRole.getPatient() != null) {
            org.eclipse.mdht.uml.cda.Patient cdaPatient = cdaPatientRole.getPatient();
            if (CollectionUtils.isNotEmpty(cdaPatient.getNames())) {
                cdaPatient.getNames().forEach(name -> patient.getName().add(this.simpleCDATypesConverter.createFHIRHumanName(name)));
            }

            try {
                patient.setGender(this.simpleCDATypesConverter.getGender(cdaPatient.getAdministrativeGenderCode()));
            } catch (FHIRException e) {
                log.error("Unknown Gender Code", e);
            }

            patient.setMaritalStatus(this.simpleCDATypesConverter.createFHIRCodeableConcept(cdaPatient.getMaritalStatusCode(), null));

            if (cdaPatient.getReligiousAffiliationCode() != null) {
                patient.getExtension().add(this.simpleCDATypesConverter.createExtension(cdaPatient.getReligiousAffiliationCode(), BaseConstants.USCORE_EXTENSION_URL));
            }

            if (cdaPatient.getRaceCode() != null) {
                patient.getExtension().add(this.simpleCDATypesConverter.createExtension(cdaPatient.getRaceCode(), BaseConstants.USCORE_EXTENSION_URL));
            }

            if (cdaPatient.getBirthplace() != null && cdaPatient.getBirthplace().getPlace() != null) {
                if (CollectionUtils.isNotEmpty(cdaPatient.getBirthplace().getPlace().getAddrs())) {
                    cdaPatient.getBirthplace().getPlace().getAddrs().forEach(ad -> patient.getExtension().add(this.simpleCDATypesConverter.createExtension(ad, BaseConstants.BIRTHPLACE_EXTENSION_URL)));
                }
            }

            if (CollectionUtils.isNotEmpty(cdaPatient.getLanguageCommunications())) {
                List<Patient.PatientCommunicationComponent> patientCommunicationComponents = cdaPatient.getLanguageCommunications().stream()
                        .map(languageCommunication -> new Patient.PatientCommunicationComponent(this.simpleCDATypesConverter.createFHIRCodeableConcept(languageCommunication.getLanguageCode(), null)))
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
        if (custodian.getAssignedCustodian() != null && custodian.getAssignedCustodian().getRepresentedCustodianOrganization() != null) {
            CustodianOrganization custodianOrganization = custodian.getAssignedCustodian().getRepresentedCustodianOrganization();
            Organization organization = new Organization();
            if (CollectionUtils.isNotEmpty(custodianOrganization.getIds())) {
                custodianOrganization.getIds().forEach(e -> organization.addIdentifier(this.simpleCDATypesConverter.createFHIRIdentifier(e)));
            }

            if (custodianOrganization.getNames() != null) {
                organization.setName(custodianOrganization.getNames().stream().map(EN::getText).collect(Collectors.joining(",")));
            }

            if (CollectionUtils.isNotEmpty(custodianOrganization.getAddrs())) {
                organization.setAddress(custodianOrganization.getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(custodianOrganization.getTelecoms())) {
                organization.setTelecom(custodianOrganization.getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).collect(Collectors.toList()));
            }
            organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
            resources.put(organization.getId(), organization);
        }

        return resources;
    }

    Map<String, Resource> convertParticipant(Participant1 participant) {
        Map<String, Resource> resources = new HashMap<>();
        if (participant.getAssociatedEntity() != null && !participant.getAssociatedEntity().isSetNullFlavor()) {
            Practitioner practitioner = new Practitioner();
            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getIds())) {
                participant.getAssociatedEntity().getIds().forEach(id -> practitioner.addIdentifier(this.simpleCDATypesConverter.createFHIRIdentifier(id)));
            }

            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getAddrs())) {
                practitioner.setAddress(participant.getAssociatedEntity().getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getTelecoms())) {
                practitioner.setTelecom(participant.getAssociatedEntity().getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).collect(Collectors.toList()));
            }

            if (participant.getAssociatedEntity().getAssociatedPerson() != null && !participant.getAssociatedEntity().getAssociatedPerson().isSetNullFlavor()) {
                Person person = participant.getAssociatedEntity().getAssociatedPerson();
                if (CollectionUtils.isNotEmpty(person.getNames())) {
                    person.getNames().forEach(name -> practitioner.getName().add(this.simpleCDATypesConverter.createFHIRHumanName(name)));
                }
            }

            practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
            resources.put(practitioner.getId(), practitioner);
        }

        return resources;
    }

    public Map<String, Resource> convertSectionAuthors(List<Author> authors, Map<String, Resource> headerResources) {
        Map<String, Resource> convertedAuthors = new HashMap<>();
        authors.forEach(author -> convertedAuthors.putAll(this.convertSectionAuthor(author, headerResources)));
        return convertedAuthors;
    }

    public Map<String, Resource> convertSectionAuthor(Author cdaAuthor, Map<String, Resource> headerResources) {
        if (cdaAuthor.getAssignedAuthor() != null && !cdaAuthor.getAssignedAuthor().isSetNullFlavor() && CollectionUtils.isNotEmpty(cdaAuthor.getAssignedAuthor().getIds())) {
            List<Identifier> identifiers = cdaAuthor.getAssignedAuthor().getIds().stream().map(this.getSimpleCDATypesConverter()::createFHIRIdentifier).collect(Collectors.toList());
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
        if (CollectionUtils.isNotEmpty(cdaOrganization.getIds())) {
            cdaOrganization.getIds().forEach(e -> organization.addIdentifier(this.simpleCDATypesConverter.createFHIRIdentifier(e)));
        }

        if (cdaOrganization.getNames() != null) {
            organization.setName(cdaOrganization.getNames().stream().map(ON::getText).collect(Collectors.joining(",")));
        }

        organization.setAddress(cdaOrganization.getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).collect(Collectors.toList()));
        organization.setTelecom(cdaOrganization.getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).collect(Collectors.toList()));
        organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
        return organization;
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


}
