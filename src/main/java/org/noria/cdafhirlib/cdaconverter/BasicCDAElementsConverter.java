package org.noria.cdafhirlib.cdaconverter;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.cda.Person;
import org.eclipse.mdht.uml.cda.*;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.eclipse.mdht.uml.hl7.rim.Participation;
import org.eclipse.mdht.uml.hl7.vocab.NullFlavor;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.constants.BaseConstants;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;
import org.noria.cdafhirlib.helper.ConvertedElementsHelper;
import org.noria.cdafhirlib.helper.FHIRElementsHelper;
import org.openhealthtools.mdht.uml.cda.consol.ResultObservation2;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
@Getter
public class BasicCDAElementsConverter {

    private static final Pattern CDA_DATE_PATTERN = Pattern.compile("(?<year>[0-9]{4})((?<month>[0-9]{2})((?<day>[0-9]{2})((?<hour>[0-9]{2})((?<minute>[0-9]{2})((?<second>[0-9]{2})(?<fractional>\\.[0-9]{1,4})?)?)?)?)?)?(?<timezone>(?<tzsign>[+\\-])(?<tzhour>[0-9]{2})(?<tzminute>[0-9]{2}))?");
    private static final DatatypeFactory XML_DATATYPE_FACTORY = createDataTypeFactory();
    private final CodeMappingProcessor codeMappingProcessor;

    public BasicCDAElementsConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
    }

    public static DatatypeFactory createDataTypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Coding createFHIRCoding(CD code, String conversionType) {
        Coding coding = null;
        if (code != null) {
            coding = new Coding();
            if (conversionType != null) {
                coding = this.codeMappingProcessor.getCodeFromMapping(code.getCode(), conversionType);
            }
            if (StringUtils.isAllBlank(coding.getCode())) {
                coding.setCode(code.getCode());
                coding.setSystem(codeMappingProcessor.getFHIRCodeSystem(code.getCodeSystem()));
                coding.setDisplay(code.getDisplayName());
            }
        }

        return coding;
    }

    public CodeableConcept createFHIRCodeableConcept(CD code, String conversionType) {
        CodeableConcept codeableConcept = null;
        if (code != null) {
            CodeableConcept codeableConceptInner = new CodeableConcept();
            Coding coding = this.createFHIRCoding(code, conversionType);
            codeableConceptInner.addCoding(coding);
            if (code.getTranslations() != null) {
                code.getTranslations().forEach(e -> codeableConceptInner.addCoding(this.createFHIRCoding(e, conversionType)));
            }

            codeableConcept = codeableConceptInner;
        }

        return codeableConcept;
    }

    public CodeableConcept createFHIRCodeableConceptFromList(List<CD> codes, String conversionType) {
        CodeableConcept codeableConcept = null;
        List<Coding> codings = codes.stream().filter(Objects::nonNull).map(code -> this.createFHIRCoding(code, conversionType)).collect(Collectors.toList());
        if (!codings.isEmpty()) {
            codeableConcept = new CodeableConcept();
            codeableConcept.setCoding(codings);
        }
        return codeableConcept;
    }

    public Address createFHIRAddress(AD cdaAddress) {
        Address address = null;
        if (cdaAddress != null && cdaAddress.getNullFlavor() != NullFlavor.UNK) {
            address = new Address();
            if (cdaAddress.getUses() != null && cdaAddress.getUses().size() != 0) {
                address.setUse(Address.AddressUse.fromCode(this.codeMappingProcessor.getStringCodeFromMapping(cdaAddress.getUses().get(0).toString(), CDAtoFHIRCodeConversionType.ADDRESS_USE.toValue())));
            }

            address.setLine(cdaAddress.getStreetAddressLines().stream().map(e -> new StringType(e.getText())).collect(Collectors.toList()));
            address.setCity(cdaAddress.getCities().stream().map(ED::getText).collect(Collectors.joining(",")));
            address.setCountry(cdaAddress.getCounties().stream().map(ED::getText).collect(Collectors.joining(",")));
            address.setPostalCode(cdaAddress.getPostalCodes().stream().map(ED::getText).collect(Collectors.joining(",")));
            address.setState(cdaAddress.getStates().stream().map(ED::getText).collect(Collectors.joining(",")));
        }

        return address;
    }

    public ContactPoint createContactPoint(TEL telecom) {
        ContactPoint contactPoint = new ContactPoint();
        if (telecom != null && telecom.getNullFlavor() != NullFlavor.UNK) {
            if (telecom.getUses() != null && telecom.getUses().size() != 0) {
                contactPoint.setUse(ContactPoint.ContactPointUse.fromCode(this.codeMappingProcessor.getStringCodeFromMapping(telecom.getUses().get(0).toString(), CDAtoFHIRCodeConversionType.TELECOM_USE.toValue())));
            }

            contactPoint.setValue(telecom.getValue());
        }
        return contactPoint;
    }

    public HumanName createFHIRHumanName(PN cdaName) {
        HumanName fhirName = new HumanName();
        cdaName.getGivens().forEach(e -> fhirName.addGiven(e.getText()));
        fhirName.setFamily(cdaName.getFamilies().stream().map(ENXP::getText).collect(Collectors.joining(",")));
        return fhirName;
    }

    public Identifier createFHIRIdentifier(II cdaId) {
        Identifier identifier = new Identifier();
        identifier.setValue(cdaId.getExtension() == null ? UUID.randomUUID().toString() : cdaId.getExtension());
        Pattern p = Pattern.compile(BaseConstants.OID_REGEX_PATTERN);
        if (cdaId.getRoot() != null && p.matcher(cdaId.getRoot()).matches()) {
            identifier.setSystem(BaseConstants.URN_OID + cdaId.getRoot());
        }
        return identifier;
    }

    public Enumerations.AdministrativeGender createGender(CD genderCode) throws FHIRException {
        String fhirCode = this.codeMappingProcessor.getStringCodeFromMapping(genderCode.getCode(), CDAtoFHIRCodeConversionType.FAMILY_HISTORY_MEMBER_PERSON_RLT_SUBJ_GENDER.toValue());
        return Enumerations.AdministrativeGender.fromCode(fhirCode);
    }


    public Extension createExtension(CD code, String url) {
        Extension extension = null;
        if (code != null) {
            extension = new Extension();
            extension.setUrl(url);
            extension.setValue(this.createFHIRCoding(code, null));
        }
        return extension;
    }

    public Extension createExtension(AD address, String url) {
        Extension extension = null;
        if (address != null) {
            extension = new Extension();
            extension.setUrl(url);
            extension.setValue(this.createFHIRAddress(address));
        }

        return extension;
    }

    //TODO: Tests for these methods
    public Range createRange(IVL_PQ interval) {
        Range range = null;
        if (interval != null) {
            range = new Range();
            range.setHigh(this.createSimpleQuantity(interval.getHigh()));
            range.setLow(this.createSimpleQuantity(interval.getLow()));
        }
        return range;
    }

    public SimpleQuantity createSimpleQuantity(PQ interval) {
        SimpleQuantity simpleQuantity = null;
        if (interval != null && !interval.isSetNullFlavor()) {
            simpleQuantity = new SimpleQuantity();
            simpleQuantity.setValue(interval.getValue());
            simpleQuantity.setUnit(interval.getUnit());
        }
        return simpleQuantity;
    }


    public Ratio createRatio(RTO_PQ_PQ cdaRatio) {
        Ratio fhirRatio = null;
        if (cdaRatio != null) {
            fhirRatio = new Ratio();
            fhirRatio.setNumerator(this.createSimpleQuantity(cdaRatio.getNumerator()));
            fhirRatio.setDenominator(this.createSimpleQuantity(cdaRatio.getDenominator()));
        }
        return fhirRatio;
    }


    /*
    public Extension createDateExtension(IVL_TS date){
        IElement iElementDate = this.convertIVLTSDate(date);
        if (iElementDate != null) {
            Extension extension = new Extension();
            if (iElementDate instanceof Period) {
                extension.setUrl(url);
            }
            Extension extension = new Extension();
            extension.setUrl(url);
            extension.setValue(this.convertIVLTSDate(date));
            return extension;
        }
    }
*/


    public String convertCDAToFHIRDate(String value) {
        try {
            if (value == null)
                return null;

            XMLGregorianCalendar calendar = XML_DATATYPE_FACTORY.newXMLGregorianCalendar();
            Matcher m = CDA_DATE_PATTERN.matcher(value);
            if (!m.matches()) {
                return null;
            }

            if (m.group("year") != null) {
                calendar.setYear(Integer.parseInt(m.group("year")));
                if (m.group("month") != null) {
                    calendar.setMonth(Integer.parseInt(m.group("month")));
                    if (m.group("day") != null) {
                        calendar.setDay(Integer.parseInt(m.group("day")));
                        if (m.group("hour") != null) {
                            calendar.setHour(Integer.parseInt(m.group("hour")));
                            if (m.group("minute") != null) {
                                calendar.setMinute(Integer.parseInt(m.group("minute")));
                                if (m.group("second") != null) {
                                    calendar.setSecond(Integer.parseInt(m.group("second")));
                                    if (m.group("fractional") != null) {
                                        calendar.setFractionalSecond(new BigDecimal(m.group("fractional")));
                                    }
                                } else {
                                    calendar.setSecond(0);
                                }
                            } else {
                                calendar.setMinute(0);
                                calendar.setSecond(0);
                            }
                        }
                    }
                }
                if (m.group("timezone") != null) {
                    int timeZoneOffsetInMinutes = (int) TimeUnit.MINUTES.convert(Integer.parseInt(m.group("tzhour")), TimeUnit.HOURS) + Integer.parseInt(m.group("tzminute"));
                    if (m.group("tzsign").equals("-")) {
                        timeZoneOffsetInMinutes = -timeZoneOffsetInMinutes;
                    }
                    calendar.setTimezone(timeZoneOffsetInMinutes);
                }
            }
            return calendar.toXMLFormat();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }


    Type convertIVLTSDate(IVL_TS cdaDateTime) {
        if (cdaDateTime != null) {
            Period period = new Period();
            if (cdaDateTime.getLow() != null && !cdaDateTime.getLow().isSetNullFlavor()) {
                period.setStartElement(new DateTimeType(this.convertCDAToFHIRDate(cdaDateTime.getLow().getValue())));
            }

            if (cdaDateTime.getHigh() != null && !cdaDateTime.getHigh().isSetNullFlavor()) {
                period.setEndElement(new DateTimeType(this.convertCDAToFHIRDate(cdaDateTime.getHigh().getValue())));
            }

            if (period.hasStart() || period.hasEnd()) {
                return period;
            } else if (!cdaDateTime.isSetNullFlavor()) {
                return new DateTimeType(this.convertCDAToFHIRDate(cdaDateTime.getValue()));
            }
        }

        return null;
    }

    DateTimeType convertSXMTSDate(SXCM_TS cdaDateTime) {
        if (cdaDateTime != null) {
            return new DateTimeType(this.convertCDAToFHIRDate(cdaDateTime.getValue()));
        }
        return null;
    }


    //TODO: add test for this method

    Timing convertEIVL_TStoFHIRTiming(EIVL_TS eventInterval) {
        Timing timing = null;
        if (eventInterval != null) {
            timing = new Timing();
            Timing.TimingRepeatComponent repeatComponent = new Timing.TimingRepeatComponent();
            timing.setRepeat(repeatComponent);
            timing.setCode(this.createFHIRCodeableConcept(eventInterval.getEvent(), null));
            repeatComponent.setOffset(eventInterval.getOffset().getValue().intValue());
        }

        return timing;
    }


    Timing convertPIVL_TStoFHIRTiming(PIVL_TS periodicInterval) {
        Timing timing = null;
        if (periodicInterval != null) {
            timing = new Timing();
            Timing.TimingRepeatComponent repeatComponent = new Timing.TimingRepeatComponent();
            timing.setRepeat(repeatComponent);
            timing.getEvent().add(new DateTimeType(this.convertCDAToFHIRDate(periodicInterval.getValue())));
            if (periodicInterval.getPhase() != null) {
                Type phase = this.convertIVLTSDate(periodicInterval.getPhase());
                if (phase instanceof Period) {
                    repeatComponent.setBounds(phase);
                }
            }

            if (periodicInterval.getPeriod() != null) {
                try {
                    repeatComponent.setPeriod(periodicInterval.getPeriod().getValue().longValue());
                    repeatComponent.setPeriodUnit(Timing.UnitsOfTime.fromCode(periodicInterval.getPeriod().getUnit()));
                } catch (FHIRException e) {
                    log.warn(e.getMessage(), e);
                    log.warn("Unit can not be cast to FHIR unit set");
                }
            }
        }

        return timing;
    }


    public Map<String, Resource> convertAuthor(Author author) {
        Map<String, Resource> resources = new HashMap<>();
        Practitioner practitioner = new Practitioner();
        PractitionerRole practitionerRole = new PractitionerRole();

        if (author.getAssignedAuthor() != null) {

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getIds())) {
                author.getAssignedAuthor().getIds().forEach(e -> practitioner.getIdentifier().add(this.createFHIRIdentifier(e)));
                practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
                practitionerRole.getIdentifier().addAll(practitioner.getIdentifier());
            }

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getAddrs())) {
                practitioner.setAddress(author.getAssignedAuthor().getAddrs().stream().map(this::createFHIRAddress).collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(author.getAssignedAuthor().getTelecoms())) {
                practitioner.setTelecom(author.getAssignedAuthor().getTelecoms().stream().map(this::createContactPoint).collect(Collectors.toList()));
            }


            if (author.getAssignedAuthor().getAssignedPerson() != null) {

                if (author.getAssignedAuthor().getAssignedPerson() != null && author.getAssignedAuthor().getAssignedPerson().getNames() != null) {
                    practitioner.setName(author.getAssignedAuthor().getAssignedPerson().getNames().stream().map(this::createFHIRHumanName).collect(Collectors.toList()));
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
                List<Identifier> identifiers = assignedEntity.getIds().stream().map(this::createFHIRIdentifier).collect(Collectors.toList());
                Practitioner existingPractitoner = ConvertedElementsHelper.findPractitionerByIdentifier(identifiers, headerResources);
                if (existingPractitoner != null) {
                    practitioner = existingPractitoner;
                    resources.put(existingPractitoner.getId(), existingPractitoner);
                    log.info("FHIR Practitioner found in Header");

                } else {
                    practitioner.getIdentifier().addAll(assignedEntity.getIds().stream().map(this::createFHIRIdentifier).filter(Objects::nonNull).collect(Collectors.toList()));
                }
            }

            if (resources.isEmpty() && CollectionUtils.isNotEmpty(assignedEntity.getAddrs())) {
                practitioner.setAddress(assignedEntity.getAddrs().stream().map(this::createFHIRAddress).filter(Objects::nonNull).collect(Collectors.toList()));
            }

            if (resources.isEmpty() && CollectionUtils.isNotEmpty(assignedEntity.getTelecoms())) {
                practitioner.setTelecom(assignedEntity.getTelecoms().stream().map(this::createContactPoint).filter(Objects::nonNull).collect(Collectors.toList()));
            }

            if (resources.isEmpty() && assignedEntity.getAssignedPerson() != null) {
                if (CollectionUtils.isNotEmpty(assignedEntity.getAssignedPerson().getNames())) {
                    practitioner.setName(assignedEntity.getAssignedPerson().getNames().stream().map(this::createFHIRHumanName).filter(Objects::nonNull).collect(Collectors.toList()));
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
            codeableConcept = this.createFHIRCodeableConcept(performerWithFC.getFunctionCode(), null);
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
            cdaPatientRole.getIds().forEach(id -> patient.getIdentifier().add(this.createFHIRIdentifier(id)));
        }

        patient.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PATIENT, patient.getIdentifier()));

        if (CollectionUtils.isNotEmpty(cdaPatientRole.getAddrs())) {
            cdaPatientRole.getAddrs().forEach(address -> patient.getAddress().add(this.createFHIRAddress(address)));
        }

        if (CollectionUtils.isNotEmpty(cdaPatientRole.getTelecoms())) {
            cdaPatientRole.getTelecoms().forEach(tel -> patient.getTelecom().add(this.createContactPoint(tel)));
        }

        if (cdaPatientRole.getPatient() != null) {
            org.eclipse.mdht.uml.cda.Patient cdaPatient = cdaPatientRole.getPatient();
            if (CollectionUtils.isNotEmpty(cdaPatient.getNames())) {
                cdaPatient.getNames().forEach(name -> patient.getName().add(this.createFHIRHumanName(name)));
            }

            try {
                patient.setGender(this.createGender(cdaPatient.getAdministrativeGenderCode()));
            } catch (FHIRException e) {
                log.error("Unknown Gender Code", e);
            }

            patient.setMaritalStatus(this.createFHIRCodeableConcept(cdaPatient.getMaritalStatusCode(), null));

            if (cdaPatient.getReligiousAffiliationCode() != null) {
                patient.getExtension().add(this.createExtension(cdaPatient.getReligiousAffiliationCode(), BaseConstants.USCORE_EXTENSION_URL));
            }

            if (cdaPatient.getRaceCode() != null) {
                patient.getExtension().add(this.createExtension(cdaPatient.getRaceCode(), BaseConstants.USCORE_EXTENSION_URL));
            }

            if (cdaPatient.getBirthplace() != null && cdaPatient.getBirthplace().getPlace() != null) {
                if (CollectionUtils.isNotEmpty(cdaPatient.getBirthplace().getPlace().getAddrs())) {
                    cdaPatient.getBirthplace().getPlace().getAddrs().forEach(ad -> patient.getExtension().add(this.createExtension(ad, BaseConstants.BIRTHPLACE_EXTENSION_URL)));
                }
            }

            if (CollectionUtils.isNotEmpty(cdaPatient.getLanguageCommunications())) {
                List<Patient.PatientCommunicationComponent> patientCommunicationComponents = cdaPatient.getLanguageCommunications().stream()
                        .map(languageCommunication -> new Patient.PatientCommunicationComponent(this.createFHIRCodeableConcept(languageCommunication.getLanguageCode(), null)))
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
                custodianOrganization.getIds().forEach(e -> organization.addIdentifier(this.createFHIRIdentifier(e)));
            }

            if (custodianOrganization.getNames() != null) {
                organization.setName(custodianOrganization.getNames().stream().map(EN::getText).collect(Collectors.joining(",")));
            }

            if (CollectionUtils.isNotEmpty(custodianOrganization.getAddrs())) {
                organization.setAddress(custodianOrganization.getAddrs().stream().map(this::createFHIRAddress).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(custodianOrganization.getTelecoms())) {
                organization.setTelecom(custodianOrganization.getTelecoms().stream().map(this::createContactPoint).collect(Collectors.toList()));
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
                participant.getAssociatedEntity().getIds().forEach(id -> practitioner.addIdentifier(this.createFHIRIdentifier(id)));
            }

            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getAddrs())) {
                practitioner.setAddress(participant.getAssociatedEntity().getAddrs().stream().map(this::createFHIRAddress).collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(participant.getAssociatedEntity().getTelecoms())) {
                practitioner.setTelecom(participant.getAssociatedEntity().getTelecoms().stream().map(this::createContactPoint).collect(Collectors.toList()));
            }

            if (participant.getAssociatedEntity().getAssociatedPerson() != null && !participant.getAssociatedEntity().getAssociatedPerson().isSetNullFlavor()) {
                Person person = participant.getAssociatedEntity().getAssociatedPerson();
                if (CollectionUtils.isNotEmpty(person.getNames())) {
                    person.getNames().forEach(name -> practitioner.getName().add(this.createFHIRHumanName(name)));
                }
            }

            practitioner.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.PRACTITIONER, practitioner.getIdentifier()));
            resources.put(practitioner.getId(), practitioner);
        }

        return resources;
    }

    public Map<String, Resource> convertSectionAuthor(Author cdaAuthor, Map<String, Resource> headerResources) {
        if (cdaAuthor.getAssignedAuthor() != null && !cdaAuthor.getAssignedAuthor().isSetNullFlavor() && CollectionUtils.isNotEmpty(cdaAuthor.getAssignedAuthor().getIds())) {
            List<Identifier> identifiers = cdaAuthor.getAssignedAuthor().getIds().stream().map(this::createFHIRIdentifier).collect(Collectors.toList());
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
            cdaOrganization.getIds().forEach(e -> organization.addIdentifier(this.createFHIRIdentifier(e)));
        }

        if (cdaOrganization.getNames() != null) {
            organization.setName(cdaOrganization.getNames().stream().map(ON::getText).collect(Collectors.joining(",")));
        }

        if (CollectionUtils.isNotEmpty(cdaOrganization.getAddrs())) {
            organization.setAddress(cdaOrganization.getAddrs().stream().map(this::createFHIRAddress).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(cdaOrganization.getTelecoms())) {
            organization.setTelecom(cdaOrganization.getTelecoms().stream().map(this::createContactPoint).collect(Collectors.toList()));
        }

        organization.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.ORGANIZATION, organization.getIdentifier()));
        return organization;
    }

    public Observation createFHIRObservation(org.eclipse.mdht.uml.cda.Observation cdaObservation, Map<String, Resource> resources, Map<String, Resource> headerResources) {
        Observation observation = new Observation();
        if (CollectionUtils.isNotEmpty(cdaObservation.getIds())) {
            cdaObservation.getIds().forEach(id -> observation.addIdentifier(this.createFHIRIdentifier(id)));
        }

        if (cdaObservation.getEffectiveTime() != null) {
            Type recordedDate = this.convertIVLTSDate(cdaObservation.getEffectiveTime());
            observation.setEffective(recordedDate);
        }

        Reference reference = ConvertedElementsHelper.getPatientReference(headerResources);
        if (reference != null) {
            observation.setSubject(reference);
        }

        if (cdaObservation.getCode() != null) {
            observation.setCode(this.createFHIRCodeableConcept(cdaObservation.getCode(), null));
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
                observation.setValue(this.createFHIRCodeableConcept((CD) cdaObservation.getValues().get(0), null));
            }

            if (cdaObservation.getValues().get(0) instanceof PQ) {
                observation.setValue(this.createSimpleQuantity((PQ) cdaObservation.getValues().get(0)));
            }
        }

        if (!cdaObservation.getInterpretationCodes().isEmpty()) {
            cdaObservation.getInterpretationCodes().forEach(ic -> observation.getInterpretation().add(this.createFHIRCodeableConcept(ic, null)));
        }

        if (!cdaObservation.getMethodCodes().isEmpty()) {
            observation.setMethod(this.createFHIRCodeableConcept(cdaObservation.getMethodCodes().get(0), null));
        }

        if (!cdaObservation.getTargetSiteCodes().isEmpty()) {
            observation.setBodySite(this.createFHIRCodeableConcept(cdaObservation.getTargetSiteCodes().get(0), null));
        }

        if (!cdaObservation.getAuthors().isEmpty()) {
            resources.putAll(this.convertAuthors(observation, cdaObservation.getAuthors(), headerResources));
        }

        if (!cdaObservation.getReferenceRanges().isEmpty()) {
            cdaObservation.getReferenceRanges().forEach(rr -> {
                if (rr.getObservationRange() != null && rr.getObservationRange().getValue() instanceof IVL_PQ) {
                    IVL_PQ refRangeValue = (IVL_PQ) rr.getObservationRange().getValue();
                    Observation.ObservationReferenceRangeComponent observationReferenceRangeComponent = new Observation.ObservationReferenceRangeComponent();
                    observationReferenceRangeComponent.setLow(this.createSimpleQuantity(refRangeValue.getLow()));
                    observationReferenceRangeComponent.setHigh(this.createSimpleQuantity(refRangeValue.getHigh()));
                    observation.getReferenceRange().add(observationReferenceRangeComponent);
                }
            });
        }

        observation.setId(FHIRElementsHelper.createFHIRID(Enumerations.FHIRAllTypes.OBSERVATION, observation.getIdentifier()));

        return observation;
    }

    public Map<String, Resource> convertAuthors(Resource fhirResource, List<Author> cdaAuthors, Map<String, Resource> headerResources) {
        Map<String, Resource> authors = new HashMap<>();
        cdaAuthors.forEach(author -> authors.putAll(this.convertSectionAuthor(author, headerResources)));
        if (!authors.isEmpty()) {

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
            }
        }

        return authors;
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

    private Coding createObservationStatusCoding(org.eclipse.mdht.uml.cda.Observation cdaObservation) {
        if (cdaObservation instanceof ResultObservation2) {
            return this.createFHIRCoding(cdaObservation.getStatusCode(), CDAtoFHIRCodeConversionType.RESULT_STATUS.toValue());
        } else {
            return this.createFHIRCoding(cdaObservation.getStatusCode(), CDAtoFHIRCodeConversionType.OBSERVATION_STATUS.toValue());
        }
    }

}
