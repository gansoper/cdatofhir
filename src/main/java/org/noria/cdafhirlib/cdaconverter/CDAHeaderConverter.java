package org.noria.cdafhirlib.cdaconverter;

import ca.uhn.fhir.model.api.IElement;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.mdht.uml.cda.Custodian;
import org.eclipse.mdht.uml.cda.CustodianOrganization;
import org.eclipse.mdht.uml.cda.Participant1;
import org.eclipse.mdht.uml.cda.PatientRole;
import org.eclipse.mdht.uml.hl7.datatypes.ON;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.noria.cdafhirlib.constants.BaseConstants;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class CDAHeaderConverter {

    private final BasicCDATypesConverter basicCDATypesConverter;
    private final SimpleCDATypesConverter simpleCDATypesConverter;

    public CDAHeaderConverter(BasicCDATypesConverter basicCDATypesConverter) {
        this.basicCDATypesConverter = basicCDATypesConverter;
        this.simpleCDATypesConverter = basicCDATypesConverter.getSimpleCDATypesConverter();
    }


    Map<String, IBaseResource> convertPatient(PatientRole cdaPatientRole) {
        Map<String, IBaseResource> resources = new HashMap<>();
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
            Organization organization = this.basicCDATypesConverter.createFHIROrganization(cdaPatientRole.getProviderOrganization());
            resources.put(organization.getId(), organization);
        }

        return resources;
    }

    Map<String, IBaseResource> convertCustodian(Custodian custodian) {
        Map<String, IBaseResource> resources = new HashMap<>();
        if (custodian.getAssignedCustodian() != null && custodian.getAssignedCustodian().getRepresentedCustodianOrganization() != null){
            CustodianOrganization custodianOrganization = custodian.getAssignedCustodian().getRepresentedCustodianOrganization();
            Organization organization = new Organization();
            if (CollectionUtils.isNotEmpty(custodianOrganization.getIds())) {
                custodianOrganization.getIds().forEach(e -> organization.addIdentifier(this.simpleCDATypesConverter.createFHIRIdentifier(e)));
            }

            if (custodianOrganization.getNames() != null) {
                organization.setName(custodianOrganization.getNames().stream().map(e-> e.getText()).collect(Collectors.joining(",")));
            }

            organization.setAddress(custodianOrganization.getAddrs().stream().map(this.simpleCDATypesConverter::createFHIRAddress).collect(Collectors.toList()));
            organization.setTelecom(custodianOrganization.getTelecoms().stream().map(this.simpleCDATypesConverter::createContactPoint).collect(Collectors.toList()));
            organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
            resources.put(organization.getId(), organization);
        }

        return resources;
    }

    Map<String, IBaseResource> convertParticipant(Participant1 participant) {
        Map<String, IBaseResource> resources = new HashMap<>();
        Practitioner practitioner = new Practitioner();
        IElement date = this.simpleCDATypesConverter.convertIVLTSDate(participant.getTime());
        if (date != null){

        }
        return resources;
    }


}
