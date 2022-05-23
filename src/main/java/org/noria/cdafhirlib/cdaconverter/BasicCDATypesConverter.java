package org.noria.cdafhirlib.cdaconverter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.cda.Performer1;
import org.eclipse.mdht.uml.hl7.datatypes.ON;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicCDATypesConverter {

    private final CodeMappingProcessor codeMappingProcessor;

    private final SimpleCDATypesConverter simpleCDATypesConverter;

    public BasicCDATypesConverter(CDAtoFHIRCodes codeMappings, SimpleCDATypesConverter simpleCDATypesConverter) {
        this.codeMappingProcessor = CodeMappingProcessor.getInstance(codeMappings);
        this.simpleCDATypesConverter = simpleCDATypesConverter;
    }

    public Map<String, IBaseResource> convertAuthor(Author author) {
        Map<String, IBaseResource> resources = new HashMap<>();
        Practitioner practitioner = new Practitioner();
        PractitionerRole practitionerRole = new PractitionerRole();

        if (author.getAssignedAuthor() != null) {

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getIds())) {
                author.getAssignedAuthor().getIds().forEach(e -> practitioner.getIdentifier().add(this.simpleCDATypesConverter.createFHIRIdentifier(e)));
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
                    practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
                    resources.put(practitioner.getId(), practitioner);
                }

                if (author.getAssignedAuthor().getRepresentedOrganization() != null) {
                    Organization organization = this.createFHIROrganization(author.getAssignedAuthor().getRepresentedOrganization());
                    resources.put(organization.getId(), organization);
                    if (StringUtils.isNoneBlank(practitioner.getId())) {
                        practitionerRole.setTelecom(practitioner.getTelecom());
                        practitionerRole.getTelecom().addAll(organization.getTelecom());
                        practitionerRole.setOrganization(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getId()));
                        practitionerRole.setPractitioner(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getId()));
                        practitionerRole.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONERROLE, null));
                        resources.put(practitionerRole.getId(), practitionerRole);
                    }
                }

            }
        }

        return resources;
    }


    public Map<String, IBaseResource> convertPerformer(Performer1 performer) {
        Map<String, IBaseResource> resources = new HashMap<>();
        Practitioner practitioner = new Practitioner();
        List<PractitionerRole> practitionerRoles = new ArrayList<>();
        Location location = new Location();
        List<Organization> organizations = new ArrayList<>();


        if (performer.getAssignedEntity() != null) {
            if (CollectionUtils.isNotEmpty(performer.getAssignedEntity().getIds())) {
                performer.getAssignedEntity().getIds().forEach(e -> practitioner.getIdentifier().add(this.simpleCDATypesConverter.createFHIRIdentifier(e)));
            }

            if (CollectionUtils.isNotEmpty(performer.getAssignedEntity().getAddrs())) {
                practitioner.setAddress(performer.getAssignedEntity().getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(performer.getAssignedEntity().getTelecoms())) {
                practitioner.setTelecom(performer.getAssignedEntity().getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).collect(Collectors.toList()));
            }

            if (performer.getAssignedEntity().getAssignedPerson() != null) {
                if (CollectionUtils.isNotEmpty(performer.getAssignedEntity().getAssignedPerson().getNames())) {
                    practitioner.setName(performer.getAssignedEntity().getAssignedPerson().getNames().stream().map(this.simpleCDATypesConverter::createFHIRHumanName).collect(Collectors.toList()));
                }
            }

            if (CollectionUtils.isNotEmpty(performer.getAssignedEntity().getRepresentedOrganizations())) {
                organizations = performer.getAssignedEntity().getRepresentedOrganizations().stream().map(this::createFHIROrganization).collect(Collectors.toList());
            }


        }

        if (CollectionUtils.isNotEmpty(practitioner.getIdentifier()) || CollectionUtils.isNotEmpty(practitioner.getAddress()) || CollectionUtils.isNotEmpty(practitioner.getTelecom()) || CollectionUtils.isNotEmpty(practitioner.getName())) {
            practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
            resources.put(practitioner.getId(), practitioner);
        }

        if (performer.getFunctionCode() != null) {
            CodeableConcept codeableConcept = this.simpleCDATypesConverter.createFHIRCodeableConcept(performer.getFunctionCode(), null);
            location.setAddress(practitioner.getAddressFirstRep());
            location.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.LOCATION, location.getIdentifier()));
            if (CollectionUtils.isNotEmpty(organizations)) {
                practitionerRoles.addAll(organizations.stream().map(org -> this.createPractitionerRole(codeableConcept, practitioner, location, org)).collect(Collectors.toList()));
            } else {
                practitionerRoles.add(this.createPractitionerRole(codeableConcept, practitioner, location, null));
            }

        }

        resources.put(location.getId(), location);
        practitionerRoles.forEach(pr -> resources.put(pr.getId(), pr));
        organizations.forEach(org -> resources.put(org.getId(), org));

        return resources;
    }


    public Organization createFHIROrganization(org.eclipse.mdht.uml.cda.Organization cdaOrganization) {
        Organization organization = new Organization();
        if (cdaOrganization.getTypeId() != null) {
            organization.addIdentifier(this.simpleCDATypesConverter.createFHIRIdentifier(cdaOrganization.getTypeId()));
        }

        if (cdaOrganization.getNames() != null) {
            organization.setName(cdaOrganization.getNames().stream().map(ON::getText).collect(Collectors.joining(",")));
        }

        organization.setAddress(cdaOrganization.getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).collect(Collectors.toList()));
        organization.setTelecom(cdaOrganization.getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).collect(Collectors.toList()));
        organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
        return organization;
    }

    private PractitionerRole createPractitionerRole(CodeableConcept codeableConcept, Practitioner practitioner, Location location, Organization organization) {
        PractitionerRole practitionerRole = new PractitionerRole();
        practitionerRole.getSpecialty().add(codeableConcept);
        practitionerRole.getTelecom().addAll(practitioner.getTelecom());
        practitionerRole.getIdentifier().addAll(practitioner.getIdentifier());
        practitionerRole.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONERROLE, null));
        practitionerRole.getLocation().add(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.LOCATION, location.getId()));
        if (organization != null) {
            practitionerRole.setOrganization(FHIRElementsHelper.createReference(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getId()));
        }
        return practitionerRole;
    }

}
