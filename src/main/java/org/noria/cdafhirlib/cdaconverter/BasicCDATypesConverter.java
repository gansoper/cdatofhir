package org.noria.cdafhirlib.cdaconverter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.cda.Author;
import org.eclipse.mdht.uml.hl7.datatypes.ON;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.noria.cdafhirlib.model.CDAtoFHIRCodes;

import java.util.HashMap;
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
        if (author.getTypeId() != null) {
            practitioner.getIdentifier().add(this.simpleCDATypesConverter.createFHIRIdentifier(author.getTypeId()));
            practitionerRole.getIdentifier().add(this.simpleCDATypesConverter.createFHIRIdentifier(author.getTypeId()));
        }

        if (author.getAssignedAuthor() != null) {

            if(author.getAssignedAuthor().getAddrs() != null){
                practitioner.setAddress(author.getAssignedAuthor().getAddrs().stream().map(e -> this.simpleCDATypesConverter.createFHIRAddress(e)).collect(Collectors.toList()));
            }

            if(author.getAssignedAuthor().getTelecoms() != null){
                practitioner.setTelecom(author.getAssignedAuthor().getTelecoms().stream().map(e -> this.simpleCDATypesConverter.createContactPoint(e)).collect(Collectors.toList()));
            }


            if (author.getAssignedAuthor().getAssignedPerson() != null){

                if (author.getAssignedAuthor().getAssignedPerson() != null && author.getAssignedAuthor().getAssignedPerson().getNames() != null) {
                    practitioner.setName(author.getAssignedAuthor().getAssignedPerson().getNames().stream().map(e -> this.simpleCDATypesConverter.createFHIRHumanName(e)).collect(Collectors.toList()));
                    practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
                    resources.put(practitioner.getId(), practitioner);
                }

                if (author.getAssignedAuthor().getRepresentedOrganization() != null) {
                    Organization organization = this.createFHIROrganization(author.getAssignedAuthor().getRepresentedOrganization());
                    organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
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


    public Organization createFHIROrganization(org.eclipse.mdht.uml.cda.Organization cdaOrganization) {
        Organization organization = new Organization();
        if (cdaOrganization.getTypeId() != null) {
            organization.addIdentifier(this.simpleCDATypesConverter.createFHIRIdentifier(cdaOrganization.getTypeId()));
        }

        if (cdaOrganization.getNames() != null) {
            organization.setName(cdaOrganization.getNames().stream().map(ON::getText).collect(Collectors.joining(",")));
        }

        organization.setAddress(cdaOrganization.getAddrs().stream().map(e -> this.simpleCDATypesConverter.createFHIRAddress(e)).collect(Collectors.toList()));
        organization.setTelecom(cdaOrganization.getTelecoms().stream().map(e -> this.simpleCDATypesConverter.createContactPoint(e)).collect(Collectors.toList()));

        return organization;
    }


}
