package org.noria.cdafhirlib.cdaconverter;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.noria.cdafhirlib.codemapping.CodeMappingProcessor;
import org.noria.cdafhirlib.constants.BaseConstants;
import org.noria.cdafhirlib.enumerations.CDAtoFHIRCodeConversionType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
public class SimpleCDATypesConverter {

    private static final Pattern CDA_DATE_PATTERN = Pattern.compile("(?<year>[0-9]{4})((?<month>[0-9]{2})((?<day>[0-9]{2})((?<hour>[0-9]{2})((?<minute>[0-9]{2})((?<second>[0-9]{2})(?<fractional>\\.[0-9]{1,4})?)?)?)?)?)?(?<timezone>(?<tzsign>[+\\-])(?<tzhour>[0-9]{2})(?<tzminute>[0-9]{2}))?");

    private final CodeMappingProcessor codeMappingProcessor;

    private static final DatatypeFactory XML_DATATYPE_FACTORY = createDataTypeFactory();

    public static DatatypeFactory createDataTypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public SimpleCDATypesConverter(CodeMappingProcessor codeMappingProcessor) {
        this.codeMappingProcessor = codeMappingProcessor;
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
            if (code != null && code.getTranslations() != null) {
                code.getTranslations().forEach(e -> codeableConceptInner.addCoding(this.createFHIRCoding(e, conversionType)));
            }

            codeableConcept = codeableConceptInner;
        }

        return codeableConcept;
    }

    public CodeableConcept createFHIRCodeableConceptFromList(List<CD> codes, String conversionType) {
        CodeableConcept codeableConcept = null;
        List<Coding> codings = codes.stream().filter(code-> code != null).map (code-> this.createFHIRCoding(code, conversionType)).collect(Collectors.toList());
        if (!codings.isEmpty()) {
            codeableConcept = new CodeableConcept();
            codeableConcept.setCoding(codings);
        }
        return codeableConcept;
    }

    public Address createFHIRAddress(AD cdaAddress) {
        Address address = new Address();
        if (cdaAddress.getUses() != null && cdaAddress.getUses().size() != 0) {
            address.setUse(Address.AddressUse.fromCode(this.codeMappingProcessor.getStringCodeFromMapping(cdaAddress.getUses().get(0).toString(), CDAtoFHIRCodeConversionType.ADDRESS_USE.toValue())));
        }

        address.setLine(cdaAddress.getStreetAddressLines().stream().map(e -> new StringType(e.getText())).collect(Collectors.toList()));
        address.setCity(cdaAddress.getCities().stream().map(ED::getText).collect(Collectors.joining(",")));
        address.setCountry(cdaAddress.getCounties().stream().map(ED::getText).collect(Collectors.joining(",")));
        address.setPostalCode(cdaAddress.getPostalCodes().stream().map(ED::getText).collect(Collectors.joining(",")));
        address.setState(cdaAddress.getStates().stream().map(ED::getText).collect(Collectors.joining(",")));
        return address;
    }

    public ContactPoint createContactPoint(TEL telecom) {
        ContactPoint contactPoint = new ContactPoint();
        if (telecom != null) {
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

    public Enumerations.AdministrativeGender getGender(CD genderCode) throws FHIRException {
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
    public Range createRange(IVL_PQ interval){
        Range range = new Range();
        range.setHigh(this.createSimpleQuantity(interval.getHigh()));
        range.setLow(this.createSimpleQuantity(interval.getLow()));
        return range;
    }

    public SimpleQuantity createSimpleQuantity(PQ interval){
        SimpleQuantity simpleQuantity = new SimpleQuantity();
        simpleQuantity.setValue(interval.getValue());
        simpleQuantity.setUnit(interval.getUnit());
        return simpleQuantity;
    }


    public Ratio createRatio(RTO_PQ_PQ cdaRatio){
        Ratio fhirRatio = new Ratio();
        fhirRatio.setNumerator(this.createSimpleQuantity(cdaRatio.getNumerator()));
        fhirRatio.setDenominator(this.createSimpleQuantity(cdaRatio.getDenominator()));
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

            if (period.getStartElement() != null || period.getEndElement() != null) {
                return period;
            } else if (!cdaDateTime.isSetNullFlavor()) {
                return new DateTimeType(this.convertCDAToFHIRDate(cdaDateTime.getValue()));
            }
        }

        return null;
    }

    //TODO: add test for this method

    Timing convertEIVL_TStoFHIRTiming(EIVL_TS eventInterval) {
        Timing timing = new Timing();
        if (eventInterval != null) {
            Timing.TimingRepeatComponent repeatComponent = new Timing.TimingRepeatComponent();
            timing.setRepeat(repeatComponent);
            timing.setCode(this.createFHIRCodeableConcept(eventInterval.getEvent(), null));
            repeatComponent.setOffset(eventInterval.getOffset().getValue().intValue());

        }

        return timing;
    }


    //TODO: add test for this method
    Timing convertPIVL_TStoFHIRTiming(PIVL_TS periodicInterval) {
        Timing timing = new Timing();
        if (periodicInterval != null) {
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



/*
    public void testJSON() throws Exception {
        File file = new File(this.getClass().getClassLoader().getResource("CDAtoFHIRCodes.json").getFile());
        ObjectMapper om = new ObjectMapper();
        CDAtoFHIRCodes cdAtoFHIRCodes = om.readValue(file, CDAtoFHIRCodes.class);
        cdAtoFHIRCodes.getCdaFhirMappings().forEach(e -> System.out.println(e.getType()));
    }
*/
}
