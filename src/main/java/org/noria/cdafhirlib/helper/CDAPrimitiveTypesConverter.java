package org.noria.cdafhirlib.helper;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.mdht.uml.cda.*;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.eclipse.mdht.uml.hl7.rim.InfrastructureRoot;
import org.eclipse.mdht.uml.hl7.vocab.*;
import org.openhealthtools.mdht.uml.cda.consol.DrugVehicle;
import org.openhealthtools.mdht.uml.cda.consol.ProductInstance;
import org.openhealthtools.mdht.uml.cda.consol.ServiceDeliveryLocation;
import org.w3c.dom.Element;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

//TODO: Remove this class - it is taken only as example of conversion
@Deprecated
public class CDAPrimitiveTypesConverter
{
    // http://www.hl7.org/documentcenter/private/standards_temp_F83BDF5E-1C23-BA17-0CDCF9DEFC63F781/v3/edition_web/welcome/environment/index.html
    // "YYYYMMDDHHMMSS.UUUU[+|-ZZzz]" where digits can be omitted from the right side to express less precision. Common forms are "YYYYMMDD" and "YYYYMMDDHHMM", but the ability to truncate on the right side is not limited to these two variants. See the Data Types Abstract Specification for detail.
    private static final Pattern CDA_DATE_PATTERN = Pattern.compile("(?<year>[0-9]{4})((?<month>[0-9]{2})((?<day>[0-9]{2})((?<hour>[0-9]{2})((?<minute>[0-9]{2})((?<second>[0-9]{2})(?<fractional>\\.[0-9]{1,4})?)?)?)?)?)?(?<timezone>(?<tzsign>[+\\-])(?<tzhour>[0-9]{2})(?<tzminute>[0-9]{2}))?");
    private static final DatatypeFactory XML_DATATYPE_FACTORY = createDataTypeFactory();

    public static DatatypeFactory createDataTypeFactory()
    {
        try
        {
            return DatatypeFactory.newInstance();
        }
        catch (DatatypeConfigurationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String convertCDAToXmlDate(String value)
    {
        try
        {
            if (value == null)
                return null;

            XMLGregorianCalendar calendar = XML_DATATYPE_FACTORY.newXMLGregorianCalendar();
            Matcher m = CDA_DATE_PATTERN.matcher(value);
            if (!m.matches())
            {
                return null;
            }
            // ECDA supported date types: xs:gYear xs:gYearMonth xs:date xs:dateTime
            if (m.group("year") != null)
            {
                calendar.setYear(Integer.parseInt(m.group("year")));
                if (m.group("month") != null)
                {
                    calendar.setMonth(Integer.parseInt(m.group("month")));
                    if (m.group("day") != null)
                    {
                        calendar.setDay(Integer.parseInt(m.group("day")));
                        if (m.group("hour") != null)
                        {
                            calendar.setHour(Integer.parseInt(m.group("hour")));
                            if (m.group("minute") != null)
                            {
                                calendar.setMinute(Integer.parseInt(m.group("minute")));
                                if (m.group("second") != null)
                                {
                                    calendar.setSecond(Integer.parseInt(m.group("second")));
                                    if (m.group("fractional") != null)
                                    {
                                        calendar.setFractionalSecond(new BigDecimal(m.group("fractional")));
                                    }
                                }
                                else
                                {
                                    calendar.setSecond(0);
                                }
                            }
                            else
                            {
                                calendar.setMinute(0);
                                calendar.setSecond(0);
                            }
                        }
                    }
                }
                if (m.group("timezone") != null)
                {
                    int timeZoneOffsetInMinutes = (int) TimeUnit.MINUTES.convert(Integer.parseInt(m.group("tzhour")), TimeUnit.HOURS) + Integer.parseInt(m.group("tzminute"));
                    if (m.group("tzsign").equals("-"))
                    {
                        timeZoneOffsetInMinutes = -timeZoneOffsetInMinutes;
                    }
                    calendar.setTimezone(timeZoneOffsetInMinutes);
                }
            }
            return calendar.toXMLFormat();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    public Element addNilElement(Element parent, String elementName)
    {
        Element element = parent.getOwnerDocument().createElement(elementName);
        element.setAttribute("xsi:nil", "true");
        parent.appendChild(element);
        return element;
    }

    public void addAnyValueElements(Element parent, String elementName, List<? extends ANY> cdaANYs)
    {
        for (ANY cdaANY : cdaANYs)
            addAnyValueElement(parent, elementName, cdaANY);
    }

    public Element addAnyValueElement(Element parent, String elementName, ANY cdaANY)
    {
        if (cdaANY == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        if (cdaANY instanceof ST)
        {
            addStringElement(element, "string", (ST) cdaANY);
        }
        else if (cdaANY instanceof INT)
        {
            addIntegerElement(element, "integer", (INT) cdaANY);
        }
        else if (cdaANY instanceof REAL)
        {
            addRealElement(element, "decimal", (REAL) cdaANY);
        }
        else if (cdaANY instanceof BL)
        {
            addBooleanElement(element, "boolean", (BL) cdaANY);
        }
        else if (cdaANY instanceof ED)
        {
            addEncapsulatedDataElement(element, "encapsulatedData", (ED) cdaANY);
        }
        else if (cdaANY instanceof IVL_TS)
        {
            addPeriodElement(element, "period", (IVL_TS) cdaANY);
        }
        else if (cdaANY instanceof PIVL_TS)
        {
            addPeriodicIntervalElement(element, "periodicInterval", (PIVL_TS) cdaANY);
        }
        else if (cdaANY instanceof EIVL_TS)
        {
            addEventIntervalElement(element, "eventInterval", (EIVL_TS) cdaANY);
        }
        else if (cdaANY instanceof TS)
        {
            addDateTimeElement(element, "date", (TS) cdaANY);
        }
        else if (cdaANY instanceof CD)
        {
            addTranslatedCodingElement(element, "code", (CD) cdaANY);
        }
        else if (cdaANY instanceof II)
        {
            addIdentifierElement(element, "identifier", (II) cdaANY);
        }
        else if (cdaANY instanceof TEL)
        {
            addTelecomElement(element, "telecom", (TEL) cdaANY);
        }
        else if (cdaANY instanceof AD)
        {
            addAddressElement(element, "address", (AD) cdaANY);
        }
        else if (cdaANY instanceof EN)
        {
            addNameElement(element, "name", (EN) cdaANY);
        }
        else if (cdaANY instanceof IVL_PQ)
        {
            addQuantityIntervalElement(element, "quantityInterval", (IVL_PQ) cdaANY);
        }
        else if (cdaANY instanceof RTO_PQ_PQ)
        {
            addQuantityRatioElement(element, "quantityRatio", (RTO_PQ_PQ) cdaANY);
        }
        else if (cdaANY instanceof PQ)
        {
            addQuantityElement(element, "quantity", (PQ) cdaANY);
        }
        else if (cdaANY instanceof MO)
        {
            addMonetaryAmountElement(element, "monetaryAmount", (MO) cdaANY);
        }
        else if (cdaANY.isSetNullFlavor())
        {
            return addNilElement(parent, elementName);
        }

        parent.appendChild(element);
        return element;
    }

    public Element addAnyPeriodElement(Element parent, String elementName, SXCM_TS cdaSXCM_TS)
    {
        if (cdaSXCM_TS == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        if (cdaSXCM_TS instanceof IVL_TS)
        {
            addPeriodElement(element, "period", (IVL_TS) cdaSXCM_TS);
        }
        else if (cdaSXCM_TS instanceof PIVL_TS)
        {
            addPeriodicIntervalElement(element, "periodicInterval", (PIVL_TS) cdaSXCM_TS);
        }
        else if (cdaSXCM_TS instanceof EIVL_TS)
        {
            addEventIntervalElement(element, "eventInterval", (EIVL_TS) cdaSXCM_TS);
        }
        else if (cdaSXCM_TS.isSetNullFlavor())
        {
            return addNilElement(parent, elementName);
        }

        parent.appendChild(element);
        return element;
    }

    public Element addDateTimeElement(Element parent, String elementName, TS cdaTS)
    {
        if (cdaTS == null)
            return null;
        if (cdaTS.isSetNullFlavor())
            return addNilElement(parent, elementName);
        return addStringElement(parent, elementName, convertCDAToXmlDate(cdaTS.getValue()));
    }

    public void addCodingElements(Element parent, String elementName, List<? extends CD> cdaCDs)
    {
        for (CD cd : cdaCDs)
            addCodingElement(parent, elementName, cd);
    }

    public Element addCodingElement(Element parent, String elementName, CD cdaCD)
    {
        if (cdaCD == null)
            return null;
        if (cdaCD.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addStringElement(element, "code", cdaCD.getCode());
        addStringElement(element, "display", cdaCD.getDisplayName());
        addStringElement(element, "system", cdaCD.getCodeSystem());
        addStringElement(element, "systemName", cdaCD.getCodeSystemName());
        parent.appendChild(element);
        return element;
    }

    public Element addTranslatedCodingElement(Element parent, String elementName, CD cdaCD)
    {
        if (cdaCD == null)
            return null;
        if (cdaCD.isSetNullFlavor())
            return addNilElement(parent, elementName);

        Element element = parent.getOwnerDocument().createElement(elementName);
        addCodingElement(element, "coding", cdaCD);
        for (CD cdaTranslationCD : cdaCD.getTranslations())
            addCodingElement(element, "translation", cdaTranslationCD);
        parent.appendChild(element);
        return element;
    }

    public void addIdentifierElements(Element parent, String elementName, List<II> cdaIIs)
    {
        II[] cdaArrayIIs = cdaIIs.toArray(II[]::new);
        Arrays.sort(cdaArrayIIs, (ii1, ii2) ->
        {
            boolean nullFlavor1 = ii1.isSetNullFlavor();
            boolean nullFlavor2 = ii2.isSetNullFlavor();
            if (nullFlavor1 || nullFlavor2)
                return Boolean.compare(nullFlavor1, nullFlavor2);
            int res = StringUtils.compare(ii1.getRoot(), ii2.getRoot());
            return res != 0 ? res : StringUtils.compare(ii1.getExtension(), ii2.getExtension());
        });
        for (II id : cdaArrayIIs)
            addIdentifierElement(parent, elementName, id);
    }

    public Element addIdentifierElement(Element parent, String elementName, II cdaII)
    {
        if (cdaII == null)
            return null;
        if (cdaII.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addStringElement(element, "root", cdaII.getRoot());
        addStringElement(element, "extension", cdaII.getExtension());
        parent.appendChild(element);
        return element;
    }

    public Element addIntegerElement(Element parent, String elementName, INT cdaINT)
    {
        if (cdaINT == null)
            return null;
        if (cdaINT.isSetNullFlavor())
            return addNilElement(parent, elementName);
        return addNumberElement(parent, elementName, cdaINT.getValue());
    }

    public Element addRealElement(Element parent, String elementName, REAL cdaREAL)
    {
        if (cdaREAL == null)
            return null;
        if (cdaREAL.isSetNullFlavor())
            return addNilElement(parent, elementName);
        return addNumberElement(parent, elementName, cdaREAL.getValue());
    }

    public Element addNumberElement(Element parent, String elementName, Number value)
    {
        if (value == null)
            return null;
        return addStringElement(parent, elementName, value.toString());
    }

    public Element addBooleanElement(Element parent, String elementName, BL cdaBL)
    {
        if (cdaBL == null)
            return null;
        if (cdaBL.isSetNullFlavor())
            return addNilElement(parent, elementName);
        return addBooleanElement(parent, elementName, cdaBL.getValue());
    }

    public Element addBooleanElement(Element parent, String elementName, Boolean value)
    {
        if (value == null)
            return null;
        return addStringElement(parent, elementName, value.toString());
    }

    public Element addStringElement(Element parent, String elementName, ED cdaED)
    {
        if (cdaED == null)
            return null;
        if (cdaED.isSetNullFlavor())
            return addNilElement(parent, elementName);
        if (cdaED.getReference() != null)
            return null;
        return addStringElement(parent, elementName, cdaED.getText());
    }

    public Element addStringElement(Element parent, String elementName, StrucDocText cdaStrucDocText)
    {
        if (cdaStrucDocText == null)
            return null;
        return addStringElement(parent, elementName, cdaStrucDocText.getText());
    }

    public Element addStringElement(Element parent, String elementName, String value)
    {
        if (StringUtils.isEmpty(value))
            return null;
        Element element = parent.getOwnerDocument().createElement(elementName);
        element.setTextContent(value);
        parent.appendChild(element);
        return element;
    }

    public Element addBinElement(Element parent, String elementName, byte[] data)
    {
        if (data == null)
            return null;
        String base64Data = Base64.getEncoder().encodeToString(data);
        return addStringElement(parent, elementName, base64Data);
    }

    public Element addPeriodElement(Element parent, String elementName, IVL_TS cdaIVL_TS)
    {
        if (cdaIVL_TS == null)
            return null;
        if (cdaIVL_TS.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addDateTimeElement(element, "start", cdaIVL_TS.getLow());
        addDateTimeElement(element, "end", cdaIVL_TS.getHigh());
        parent.appendChild(element);
        return element;
    }

    public void addDateTimeOrPeriodElements(Element parent, List<? extends SXCM_TS> cdaSXCM_TSs)
    {
        for (SXCM_TS cdaSXCM_TS : cdaSXCM_TSs)
        {
            if (cdaSXCM_TS instanceof IVL_TS)
            {
                addDateTimeOrPeriodElement(parent, (IVL_TS) cdaSXCM_TS);
            }
            else
            {
                addDateTimeElement(parent, "date", cdaSXCM_TS);
            }
        }
    }

    public Element addDateTimeOrPeriodElement(Element parent, IVL_TS cdaIVL_TS)
    {
        if (cdaIVL_TS == null)
            return null;
        if (cdaIVL_TS.getLow() != null || cdaIVL_TS.getHigh() != null)
        {
            return addPeriodElement(parent, "period", cdaIVL_TS);
        }
        else
        {
            return addDateTimeElement(parent, "date", cdaIVL_TS);
        }
    }

    public Element addPeriodicIntervalElement(Element parent, String elementName, PIVL_TS cdaPIVL_TS)
    {
        if (cdaPIVL_TS == null)
            return null;
        if (cdaPIVL_TS.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addDateTimeElement(element, "date", cdaPIVL_TS);
        addPeriodElement(element, "phase", cdaPIVL_TS.getPhase());
        if (cdaPIVL_TS.getPeriod() instanceof IVL_PQ)
            addQuantityIntervalElement(element, "period", (IVL_PQ) cdaPIVL_TS.getPeriod());
        else
            addQuantityElement(element, "period", cdaPIVL_TS.getPeriod());
        parent.appendChild(element);
        return element;
    }

    public Element addEventIntervalElement(Element parent, String elementName, EIVL_TS cdaEIVL_TS)
    {
        if (cdaEIVL_TS == null)
            return null;
        if (cdaEIVL_TS.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addCodingElement(element, "event", cdaEIVL_TS.getEvent());
        addQuantityIntervalElement(element, "offset", cdaEIVL_TS.getOffset());
        parent.appendChild(element);
        return element;
    }

    public Element addQuantityElement(Element parent, String elementName, PQ cdaPQ)
    {
        if (cdaPQ == null)
            return null;
        if (cdaPQ.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addNumberElement(element, "value", cdaPQ.getValue());
        addStringElement(element, "unit", cdaPQ.getUnit());
        parent.appendChild(element);
        return element;
    }

    public Element addQuantityIntervalElement(Element parent, String elementName, IVL_PQ cdaIVL_PQ)
    {
        if (cdaIVL_PQ == null)
            return null;
        if (cdaIVL_PQ.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addNumberElement(element, "value", cdaIVL_PQ.getValue());
        if (!"1".equals(cdaIVL_PQ.getUnit()))
            addStringElement(element, "unit", cdaIVL_PQ.getUnit());
        addQuantityElement(element, "low", cdaIVL_PQ.getLow());
        addQuantityElement(element, "high", cdaIVL_PQ.getHigh());
        addQuantityElement(element, "width", cdaIVL_PQ.getWidth());
        addQuantityElement(element, "center", cdaIVL_PQ.getCenter());
        parent.appendChild(element);
        return element;
    }

    public Element addQuantityRatioElement(Element parent, String elementName, RTO_PQ_PQ cdaRTO_PQ_PQ)
    {
        if (cdaRTO_PQ_PQ == null)
            return null;
        if (cdaRTO_PQ_PQ.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addQuantityElement(element, "numerator", cdaRTO_PQ_PQ.getNumerator());
        addQuantityElement(element, "denominator", cdaRTO_PQ_PQ.getDenominator());
        parent.appendChild(element);
        return element;
    }

    public Element addMonetaryAmountElement(Element parent, String elementName, MO cdaMO)
    {
        if (cdaMO == null)
            return null;
        if (cdaMO.isSetNullFlavor())
            return addNilElement(parent, elementName);
        Element element = parent.getOwnerDocument().createElement(elementName);
        addStringElement(element, "currency", cdaMO.getCurrency());
        addNumberElement(element, "value", cdaMO.getValue());
        parent.appendChild(element);
        return element;
    }

    public void addTelecomElements(Element parent, String elementName, List<TEL> cdaTELs)
    {
        for (TEL tel : cdaTELs)
            addTelecomElement(parent, elementName, tel);
    }

    public Element addTelecomElement(Element parent, String elementName, TEL cdaTEL)
    {
        if (cdaTEL == null)
            return null;
        if (cdaTEL.isSetNullFlavor())
            return addNilElement(parent, elementName);

        Element element = parent.getOwnerDocument().createElement(elementName);

        if (CollectionUtils.isNotEmpty(cdaTEL.getUses()))
            addStringElement(element, "use", cdaTEL.getUses().get(0).getLiteral());
        addStringElement(element, "value", cdaTEL.getValue());
        for (SXCM_TS cdaSXCM_TS : cdaTEL.getUseablePeriods())
            addAnyPeriodElement(element, "useablePeriod", cdaSXCM_TS);

        parent.appendChild(element);
        return element;
    }

    public void addAddressElements(Element parent, String elementName, List<AD> cdaADs)
    {
        for (AD ad : cdaADs)
            addAddressElement(parent, elementName, ad);
    }

    public Element addAddressElement(Element parent, String elementName, AD cdaAD)
    {
        if (cdaAD == null)
            return null;
        if (cdaAD.isSetNullFlavor())
            return addNilElement(parent, elementName);

        Element element = parent.getOwnerDocument().createElement(elementName);

        if (CollectionUtils.isNotEmpty(cdaAD.getUses()))
            addStringElement(element, "use", cdaAD.getUses().get(0).getLiteral());
        for (ADXP cdaADXP : cdaAD.getStreetAddressLines())
            addStringElement(element, "line", cdaADXP);
        for (ADXP cdaADXP : cdaAD.getCities())
            addStringElement(element, "city", cdaADXP);
        for (ADXP cdaADXP : cdaAD.getStates())
            addStringElement(element, "state", cdaADXP);
        for (ADXP cdaADXP : cdaAD.getPostalCodes())
            addStringElement(element, "postalCode", cdaADXP);
        for (ADXP cdaADXP : cdaAD.getCountries())
            addStringElement(element, "country", cdaADXP);
        for (SXCM_TS cdaSXCM_TS : cdaAD.getUseablePeriods())
            addAnyPeriodElement(element, "useablePeriod", cdaSXCM_TS);

        parent.appendChild(element);
        return element;
    }

    public void addNameElements(Element parent, String elementName, List<? extends EN> cdaENs)
    {
        for (EN en : cdaENs)
            addNameElement(parent, elementName, en);
    }

    public Element addNameElement(Element parent, String elementName, EN cdaEN)
    {
        if (cdaEN == null)
            return null;
        if (cdaEN.isSetNullFlavor())
            return addNilElement(parent, elementName);

        Element element = parent.getOwnerDocument().createElement(elementName);

        addStringElement(element, "simpleName", cdaEN.getText(true));

        if (CollectionUtils.isNotEmpty(cdaEN.getUses()))
            addStringElement(element, "use", cdaEN.getUses().get(0).getLiteral());
        for (ENXP enxp : cdaEN.getFamilies())
            addQualifiedValueElement(element, "family", enxp);
        for (ENXP enxp : cdaEN.getGivens())
            addQualifiedValueElement(element, "given", enxp);
        for (ENXP enxp : cdaEN.getPrefixes())
            addQualifiedValueElement(element, "prefix", enxp);
        for (ENXP enxp : cdaEN.getSuffixes())
            addQualifiedValueElement(element, "suffix", enxp);

        parent.appendChild(element);
        return element;
    }

    public Element addQualifiedValueElement(Element parent, String elementName, ENXP cdaENXP)
    {
        if (cdaENXP == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addStringElement(element, "value", cdaENXP.getText());

        if (CollectionUtils.isNotEmpty(cdaENXP.getQualifiers()))
        {
            addStringElement(element, "qualifier", cdaENXP.getQualifiers().get(0).getLiteral());
        }

        parent.appendChild(element);
        return element;
    }

    public Element addLocationElement(Element parent, String elementName, ServiceDeliveryLocation cdaServiceDeliveryLocation)
    {
        if (cdaServiceDeliveryLocation == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addIdentifierElements(element, "id", cdaServiceDeliveryLocation.getIds());
        addCodingElement(element, "code", cdaServiceDeliveryLocation.getCode());
        addAddressElements(element, "address", cdaServiceDeliveryLocation.getAddrs());
        addTelecomElements(element, "telecom", cdaServiceDeliveryLocation.getTelecoms());

        PlayingEntity cdaPlayingEntity = cdaServiceDeliveryLocation.getPlayingEntity();
        if (cdaPlayingEntity != null && cdaPlayingEntity.getClassCode() == EntityClassRoot.PLC)
        {
            addNameElements(element, "name", cdaPlayingEntity.getNames());
        }

        parent.appendChild(element);
        return element;
    }

    public Element addDrugVehicleElement(Element parent, String elementName, DrugVehicle cdaDrugVehicle)
    {
        if (cdaDrugVehicle == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addIdentifierElements(element, "id", cdaDrugVehicle.getIds());
        addAddressElements(element, "address", cdaDrugVehicle.getAddrs());
        addTelecomElements(element, "telecom", cdaDrugVehicle.getTelecoms());

        PlayingEntity cdaPlayingEntity = cdaDrugVehicle.getPlayingEntity();
        if (cdaPlayingEntity != null)
        {
            addCodingElement(element, "entityCode", cdaPlayingEntity.getCode());
            addNameElements(element, "name", cdaPlayingEntity.getNames());
        }

        parent.appendChild(element);
        return element;
    }

    public Element addProductInstanceElement(Element parent, String elementName, ProductInstance cdaProductInstance)
    {
        if (cdaProductInstance == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addIdentifierElements(element, "id", cdaProductInstance.getIds());

        Device cdaDevice = cdaProductInstance.getPlayingDevice();
        if (cdaDevice != null)
        {
            addCodingElement(element, "playingDeviceCode", cdaDevice.getCode());
        }

        Entity cdaEntity = cdaProductInstance.getScopingEntity();
        if (cdaEntity != null)
        {
            addIdentifierElements(element, "scopingEntityId", cdaEntity.getIds());
        }

        parent.appendChild(element);
        return element;
    }

    public Element addResponsiblePersonElement(Element parent, String elementName, Participant2 cdaParticipant)
    {
        if (cdaParticipant == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        ParticipantRole cdaParticipantRole = cdaParticipant.getParticipantRole();
        if (cdaParticipantRole != null && cdaParticipantRole.getClassCode() == RoleClassRoot.ASSIGNED)
        {
            addIdentifierElements(element, "id", cdaParticipantRole.getIds());

            PlayingEntity cdaPlayingEntity = cdaParticipantRole.getPlayingEntity();
            if (cdaPlayingEntity != null && cdaPlayingEntity.getClassCode() == EntityClassRoot.PSN)
            {
                addNameElements(element, "name", cdaPlayingEntity.getNames());
            }
        }

        parent.appendChild(element);
        return element;
    }

    public Element addObservationRangeElement(Element parent, String elementName, ObservationRange cdaObservationRange)
    {
        if (cdaObservationRange == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addStringElement(element, "text", cdaObservationRange.getText());
        if (cdaObservationRange.getText() != null && cdaObservationRange.getText().getReference() != null)
        {
            addStringElement(element, "textReference", cdaObservationRange.getText().getReference().getValue());
        }
        addAnyValueElement(element, "value", cdaObservationRange.getValue());
        addCodingElement(element, "interpretation", cdaObservationRange.getInterpretationCode());

        parent.appendChild(element);
        return element;
    }

    public Element addCaregiverElement(Element parent, String elementName, Participant2 cdaParticipant)
    {
        if (cdaParticipant == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addPeriodElement(element, "period", cdaParticipant.getTime());

        ParticipantRole cdaParticipantRole = cdaParticipant.getParticipantRole();
        if (cdaParticipantRole != null && cdaParticipantRole.getClassCode() == RoleClassRoot.CAREGIVER)
        {
            addIdentifierElements(element, "id", cdaParticipantRole.getIds());
            addCodingElement(element, "code", cdaParticipantRole.getCode());
            addAddressElements(element, "address", cdaParticipantRole.getAddrs());
            addTelecomElements(element, "telecom", cdaParticipantRole.getTelecoms());
        }

        parent.appendChild(element);
        return element;
    }

    public Element addHandoffParticipantElement(Element parent, String elementName, Participant2 cdaParticipant)
    {
        if (cdaParticipant == null || cdaParticipant.getTypeCode() != ParticipationType.IRCP)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        ParticipantRole cdaParticipantRole = cdaParticipant.getParticipantRole();
        if (cdaParticipantRole != null)
        {
            addIdentifierElements(element, "id", cdaParticipantRole.getIds());
            addCodingElement(element, "code", cdaParticipantRole.getCode());
            addAddressElements(element, "address", cdaParticipantRole.getAddrs());

            PlayingEntity cdaPlayingEntity = cdaParticipantRole.getPlayingEntity();
            if (cdaPlayingEntity != null)
            {
                addNameElements(element, "name", cdaPlayingEntity.getNames());
            }
        }

        parent.appendChild(element);
        return element;
    }

    public Element addCoveredPersonElement(Element parent, String elementName, Participant2 cdaParticipant)
    {
        if (cdaParticipant == null || cdaParticipant.getTypeCode() != ParticipationType.COV)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addPeriodElement(element, "period", cdaParticipant.getTime());

        ParticipantRole cdaParticipantRole = cdaParticipant.getParticipantRole();
        if (cdaParticipantRole != null)
        {
            addIdentifierElements(element, "id", cdaParticipantRole.getIds());
            addCodingElement(element, "code", cdaParticipantRole.getCode());
            addAddressElements(element, "address", cdaParticipantRole.getAddrs());

            PlayingEntity cdaPlayingEntity = cdaParticipantRole.getPlayingEntity();
            if (cdaPlayingEntity != null)
            {
                addNameElements(element, "name", cdaPlayingEntity.getNames());
                addDateTimeElement(element, "birthDate", cdaPlayingEntity.getSDTCBirthTime());
            }
        }

        parent.appendChild(element);
        return element;
    }

    public Element addHolderElement(Element parent, String elementName, Participant2 cdaParticipant)
    {
        if (cdaParticipant == null || cdaParticipant.getTypeCode() != ParticipationType.HLD)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addPeriodElement(element, "period", cdaParticipant.getTime());

        ParticipantRole cdaParticipantRole = cdaParticipant.getParticipantRole();
        if (cdaParticipantRole != null)
        {
            addIdentifierElements(element, "id", cdaParticipantRole.getIds());
            addAddressElements(element, "address", cdaParticipantRole.getAddrs());
        }

        parent.appendChild(element);
        return element;
    }

    public Element addVerifierElement(Element parent, String elementName, Participant2 cdaParticipant)
    {
        if (cdaParticipant == null || cdaParticipant.getTypeCode() != ParticipationType.VRF)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addDateTimeElement(element, "date", cdaParticipant.getTime());

        ParticipantRole cdaParticipantRole = cdaParticipant.getParticipantRole();
        if (cdaParticipantRole != null)
        {
            addCodingElement(element, "code", cdaParticipantRole.getCode());
            addAddressElements(element, "address", cdaParticipantRole.getAddrs());

            PlayingEntity cdaPlayingEntity = cdaParticipantRole.getPlayingEntity();
            if (cdaPlayingEntity != null)
            {
                addNameElements(element, "name", cdaPlayingEntity.getNames());
            }
        }

        parent.appendChild(element);
        return element;
    }

    public Element addCustodianPersonElement(Element parent, String elementName, Participant2 cdaParticipant)
    {
        if (cdaParticipant == null || cdaParticipant.getTypeCode() != ParticipationType.CST)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        ParticipantRole cdaParticipantRole = cdaParticipant.getParticipantRole();
        if (cdaParticipantRole != null && cdaParticipantRole.getClassCode() == RoleClassRoot.AGNT)
        {
            addCodingElement(element, "code", cdaParticipantRole.getCode());
            addAddressElements(element, "address", cdaParticipantRole.getAddrs());
            addTelecomElements(element, "telecom", cdaParticipantRole.getTelecoms());

            PlayingEntity cdaPlayingEntity = cdaParticipantRole.getPlayingEntity();
            if (cdaPlayingEntity != null)
            {
                addCodingElement(element, "hcQualifier", cdaPlayingEntity.getCode());
                addNameElements(element, "name", cdaPlayingEntity.getNames());
            }
        }

        parent.appendChild(element);
        return element;
    }

    public Element addExternalDocumentElement(Element parent, String elementName, ExternalDocument cdaExternalDocument)
    {
        if (cdaExternalDocument == null || cdaExternalDocument.getClassCode() != ActClassDocument.DOCCLIN || cdaExternalDocument.getMoodCode() != ActMood.EVN)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addIdentifierElements(element, "id", cdaExternalDocument.getIds());
        addCodingElement(element, "code", cdaExternalDocument.getCode());
        addEncapsulatedDataElement(element, "data", cdaExternalDocument.getText());
        addIdentifierElement(element, "setId", cdaExternalDocument.getSetId());
        addIntegerElement(element, "version", cdaExternalDocument.getVersionNumber());

        parent.appendChild(element);
        return element;
    }

    public Element addExternalDocument2Element(Element parent, String elementName, ExternalDocument cdaExternalDocument)
    {
        if (cdaExternalDocument == null)
            return null;

        Element element = parent.getOwnerDocument().createElement(elementName);

        addIdentifierElements(element, "id", cdaExternalDocument.getIds());
        addEncapsulatedDataElement(element, "data", cdaExternalDocument.getText());

        parent.appendChild(element);
        return element;
    }

    public Element addEncapsulatedDataElement(Element parent, String elementName, ED cdaED)
    {
        if (cdaED == null)
            return null;
        if (cdaED.isSetNullFlavor())
            return addNilElement(parent, elementName);

        Element element = parent.getOwnerDocument().createElement(elementName);

        addStringElement(element, "data", cdaED.getText());
        if (cdaED.isSetCompression())
        {
            addStringElement(element, "compression", cdaED.getCompression().getLiteral());
        }
        addBinElement(element, "integrityCheck", cdaED.getIntegrityCheck());
        if (cdaED.isSetIntegrityCheckAlgorithm())
        {
            addStringElement(element, "integrityCheckAlgorithm", cdaED.getIntegrityCheckAlgorithm().getLiteral());
        }
        addStringElement(element, "language", cdaED.getLanguage());
        if (cdaED.isSetRepresentation() && cdaED.getRepresentation() != null)
        {
            addStringElement(element, "representation", cdaED.getRepresentation().getLiteral());
        }
        if (cdaED.isSetMediaType())
        {
            addStringElement(element, "mediaType", cdaED.getMediaType());
        }
        if (cdaED.getReference() != null)
        {
            addStringElement(element, "referenceValue", cdaED.getReference().getValue());
            addAnyValueElements(element, "referenceUseablePeriod", cdaED.getReference().getUseablePeriods());
        }
        addEncapsulatedDataElement(element, "thumbnail", cdaED.getThumbnail());

        parent.appendChild(element);
        return element;
    }

    public CD getCDValue(List<ANY> cdaANYs)
    {
        return getValueOf(cdaANYs, CD.class);
    }

    public <T extends ANY> T getValueOf(List<ANY> cdaANYs, Class<T> clazz)
    {
        if (CollectionUtils.isNotEmpty(cdaANYs))
        {
            ANY cdaANY = cdaANYs.get(0);
            if (clazz.isInstance(cdaANY))
            {
                return clazz.cast(cdaANY);
            }
        }
        return null;
    }

    public CE getPlayingEntityCode(List<Participant2> cdaParticipants,
                                   ParticipationType cdaTypeCode,
                                   RoleClassRoot cdaRoleClass,
                                   EntityClassRoot cdaEntityClass)
    {
        for (Participant2 cdaParticipant : cdaParticipants)
        {
            if (cdaParticipant.getTypeCode() == cdaTypeCode)
            {
                if (cdaParticipant.getParticipantRole().getClassCode() == cdaRoleClass)
                {
                    PlayingEntity cdaPlayingEntity = cdaParticipant.getParticipantRole().getPlayingEntity();
                    if (cdaPlayingEntity != null && cdaPlayingEntity.getClassCode() == cdaEntityClass)
                    {
                        return cdaPlayingEntity.getCode();
                    }
                }
            }
        }
        return null;
    }

    public <T extends InfrastructureRoot> T find(List<T> cdaItems, String templateId)
    {
        return cdaItems.stream().filter(o -> o.getTemplateIds().stream().anyMatch(ii -> templateId.equals(ii.getRoot()))).findFirst().orElse(null);
    }

    public <T extends InfrastructureRoot> Iterable<T> filter(List<T> cdaItems, String templateId)
    {
        return cdaItems.stream().filter(o -> o.getTemplateIds().stream().anyMatch(ii -> templateId.equals(ii.getRoot())))::iterator;
    }

    public Iterable<Participant2> filter(List<Participant2> cdaParticipants, ParticipationType cdaTypeCode)
    {
        return cdaParticipants.stream().filter(p -> p.getTypeCode() == cdaTypeCode)::iterator;
    }

    public Iterable<Precondition> filter(List<Precondition> cdaPreconditions, ActRelationshipType cdaTypeCode)
    {
        return cdaPreconditions.stream().filter(p -> p.getTypeCode() == cdaTypeCode)::iterator;
    }

    public Observation findObservation(List<EntryRelationship> cdaEntryRelationships, x_ActRelationshipEntryRelationship cdaTypeCode, x_ActMoodDocumentObservation cdaMoodCode)
    {
        return cdaEntryRelationships.stream()
                .filter(er -> er.getTypeCode() == cdaTypeCode)
                .map(EntryRelationship::getObservation)
                .filter(Objects::nonNull)
                .filter(o -> o.getMoodCode() == cdaMoodCode)
                .findFirst()
                .orElse(null);
    }

    public Stream<Observation> selectObservations(List<EntryRelationship> cdaEntryRelationships, x_ActRelationshipEntryRelationship cdaTypeCode, x_ActMoodDocumentObservation cdaMoodCode)
    {
        return cdaEntryRelationships.stream()
                .filter(er -> er.getTypeCode() == cdaTypeCode)
                .map(EntryRelationship::getObservation)
                .filter(Objects::nonNull)
                .filter(e -> e.getMoodCode() == cdaMoodCode);
    }

    public Stream<Encounter> selectEncounters(List<EntryRelationship> cdaEntryRelationships, x_ActRelationshipEntryRelationship cdaTypeCode, boolean inversionInd, x_DocumentEncounterMood cdaMoodCode)
    {
        return cdaEntryRelationships.stream()
                .filter(er -> er.getTypeCode() == cdaTypeCode && (er.isSetInversionInd() ? er.getInversionInd() : false) == inversionInd)
                .map(EntryRelationship::getEncounter)
                .filter(Objects::nonNull)
                .filter(e -> e.getMoodCode() == cdaMoodCode);
    }

    public Stream<Act> selectActs(List<EntryRelationship> cdaEntryRelationships, x_ActRelationshipEntryRelationship cdaTypeCode, String templateId)
    {
        return cdaEntryRelationships.stream()
                .filter(er -> er.getTypeCode() == cdaTypeCode)
                .map(EntryRelationship::getAct)
                .filter(Objects::nonNull)
                .filter(a -> a.getTemplateIds().stream().anyMatch(t -> templateId.equals(t.getRoot())));
    }

    public boolean isEmpty(ClinicalDocument cdaDocument)
    {
        return cdaDocument.getComponent() == null ||
                cdaDocument.getComponent().getStructuredBody() == null ||
                cdaDocument.getComponent().getStructuredBody().getComponents().isEmpty();
    }

    public boolean isEmpty(EObject cdaObject)
    {
        if (cdaObject instanceof Act)
        {
            return ((Act) cdaObject).getEntryRelationships().isEmpty();
        }
        else if (cdaObject instanceof Observation)
        {
            return ((Observation) cdaObject).getEntryRelationships().isEmpty();
        }
        else if (cdaObject instanceof Procedure)
        {
            return ((Procedure) cdaObject).getEntryRelationships().isEmpty();
        }
        else if (cdaObject instanceof Encounter)
        {
            return ((Encounter) cdaObject).getEntryRelationships().isEmpty();
        }
        else if (cdaObject instanceof SubstanceAdministration)
        {
            return ((SubstanceAdministration) cdaObject).getEntryRelationships().isEmpty();
        }
        else if (cdaObject instanceof Supply)
        {
            return ((Supply) cdaObject).getEntryRelationships().isEmpty();
        }
        else if (cdaObject instanceof Organizer)
        {
            return ((Organizer) cdaObject).getComponents().isEmpty();
        }
        return true;
    }

    public boolean removeIfEmpty(EObject cdaObject)
    {
        if (!isEmpty(cdaObject))
        {
            clearSelf(cdaObject);
            return false;
        }

        EObject cdaContainer = cdaObject.eContainer();
        if (cdaContainer instanceof Entry)
        {
            ((Section) cdaContainer.eContainer()).getEntries().remove(cdaContainer);
        }
        else if (cdaContainer instanceof EntryRelationship)
        {
            removeEntryRelationship((EntryRelationship) cdaContainer);
        }
        else if (cdaContainer instanceof Component4)
        {
            ((Organizer) cdaContainer.eContainer()).getComponents().remove(cdaContainer);
        }
        else
        {
            throw new RuntimeException("removeIfEmpty(cdaObject): Unknown CDA object container parameter " + cdaContainer + " for object " + cdaObject);
        }
        return true;
    }

    public void clearSelf(ClinicalDocument cdaDocument)
    {
        cdaDocument.getRealmCodes().clear();
        cdaDocument.setTypeId(null);
        cdaDocument.setId(null);
        cdaDocument.setCode(null);
        cdaDocument.setTitle(null);
        cdaDocument.setEffectiveTime(null);
        cdaDocument.setConfidentialityCode(null);
        cdaDocument.setLanguageCode(null);
        cdaDocument.setSetId(null);
        cdaDocument.setVersionNumber(null);
        cdaDocument.getRecordTargets().clear();
        cdaDocument.getAuthors().clear();
        cdaDocument.setDataEnterer(null);
        cdaDocument.getInformants().clear();
        cdaDocument.setCustodian(null);
        cdaDocument.getInformationRecipients().clear();
        cdaDocument.setLegalAuthenticator(null);
        cdaDocument.getAuthenticators().clear();
        cdaDocument.getParticipants().clear();
        cdaDocument.getInFulfillmentOfs().clear();
        cdaDocument.getDocumentationOfs().clear();
        cdaDocument.setComponentOf(null);
    }

    public void clearSelf(EObject cdaObject)
    {
        if (cdaObject instanceof Section)
        {
            Section cdaSection = (Section) cdaObject;
            cdaSection.getRealmCodes().clear();
            cdaSection.setTypeId(null);
            cdaSection.setId(null);
            cdaSection.setCode(null);
            cdaSection.setTitle(null);
            cdaSection.setText(null);
            cdaSection.setConfidentialityCode(null);
            cdaSection.setLanguageCode(null);
            cdaSection.setSubject(null);
            cdaSection.getAuthors().clear();
            cdaSection.getInformants().clear();
        }
        else if (cdaObject instanceof Act)
        {
            Act cdaAct = (Act) cdaObject;
            cdaAct.getRealmCodes().clear();
            cdaAct.setTypeId(null);
            cdaAct.getIds().clear();
            cdaAct.setCode(null);
            cdaAct.setText(null);
            cdaAct.setStatusCode(null);
            cdaAct.setEffectiveTime(null);
            cdaAct.setPriorityCode(null);
            cdaAct.setLanguageCode(null);
            cdaAct.setSubject(null);
            cdaAct.getPerformers().clear();
            cdaAct.getAuthors().clear();
            cdaAct.getInformants().clear();
            cdaAct.getParticipants().clear();
            cdaAct.getReferences().clear();
            cdaAct.getPreconditions().clear();
        }
        else if (cdaObject instanceof Observation)
        {
            Observation cdaObservation = (Observation) cdaObject;
            cdaObservation.getRealmCodes().clear();
            cdaObservation.setTypeId(null);
            cdaObservation.getIds().clear();
            cdaObservation.setCode(null);
            cdaObservation.setDerivationExpr(null);
            cdaObservation.setText(null);
            cdaObservation.setStatusCode(null);
            cdaObservation.setEffectiveTime(null);
            cdaObservation.setPriorityCode(null);
            cdaObservation.setRepeatNumber(null);
            cdaObservation.setLanguageCode(null);
            cdaObservation.getValues().clear();
            cdaObservation.getInterpretationCodes().clear();
            cdaObservation.getMethodCodes().clear();
            cdaObservation.getTargetSiteCodes().clear();
            cdaObservation.setSubject(null);
            cdaObservation.getPerformers().clear();
            cdaObservation.getAuthors().clear();
            cdaObservation.getInformants().clear();
            cdaObservation.getParticipants().clear();
            cdaObservation.getReferences().clear();
            cdaObservation.getPreconditions().clear();
        }
        else if (cdaObject instanceof Procedure)
        {
            Procedure cdaProcedure = (Procedure) cdaObject;
            cdaProcedure.getRealmCodes().clear();
            cdaProcedure.setTypeId(null);
            cdaProcedure.getIds().clear();
            cdaProcedure.setCode(null);
            cdaProcedure.setText(null);
            cdaProcedure.setStatusCode(null);
            cdaProcedure.setEffectiveTime(null);
            cdaProcedure.setPriorityCode(null);
            cdaProcedure.setLanguageCode(null);
            cdaProcedure.getMethodCodes().clear();
            cdaProcedure.getApproachSiteCodes().clear();
            cdaProcedure.getTargetSiteCodes().clear();
            cdaProcedure.setSubject(null);
            cdaProcedure.getPerformers().clear();
            cdaProcedure.getAuthors().clear();
            cdaProcedure.getInformants().clear();
            cdaProcedure.getParticipants().clear();
            cdaProcedure.getReferences().clear();
            cdaProcedure.getPreconditions().clear();
        }
        else if (cdaObject instanceof Encounter)
        {
            Encounter cdaEncounter = (Encounter) cdaObject;
            cdaEncounter.getRealmCodes().clear();
            cdaEncounter.setTypeId(null);
            cdaEncounter.getIds().clear();
            cdaEncounter.setCode(null);
            cdaEncounter.setText(null);
            cdaEncounter.setStatusCode(null);
            cdaEncounter.setEffectiveTime(null);
            cdaEncounter.getSDTCDischargeDispositionCodes().clear();
            cdaEncounter.setPriorityCode(null);
            cdaEncounter.setSubject(null);
            cdaEncounter.getPerformers().clear();
            cdaEncounter.getAuthors().clear();
            cdaEncounter.getInformants().clear();
            cdaEncounter.getParticipants().clear();
            cdaEncounter.getReferences().clear();
            cdaEncounter.getPreconditions().clear();
        }
        else if (cdaObject instanceof SubstanceAdministration)
        {
            SubstanceAdministration cdaSubstanceAdministration = (SubstanceAdministration) cdaObject;
            cdaSubstanceAdministration.getRealmCodes().clear();
            cdaSubstanceAdministration.setTypeId(null);
            cdaSubstanceAdministration.getIds().clear();
            cdaSubstanceAdministration.setCode(null);
            cdaSubstanceAdministration.setText(null);
            cdaSubstanceAdministration.setStatusCode(null);
            cdaSubstanceAdministration.getEffectiveTimes().clear();
            cdaSubstanceAdministration.setPriorityCode(null);
            cdaSubstanceAdministration.setRepeatNumber(null);
            cdaSubstanceAdministration.setRouteCode(null);
            cdaSubstanceAdministration.getApproachSiteCodes().clear();
            cdaSubstanceAdministration.setDoseQuantity(null);
            cdaSubstanceAdministration.setRateQuantity(null);
            cdaSubstanceAdministration.setMaxDoseQuantity(null);
            cdaSubstanceAdministration.setAdministrationUnitCode(null);
            cdaSubstanceAdministration.setSubject(null);
            cdaSubstanceAdministration.setConsumable(null);
            cdaSubstanceAdministration.getPerformers().clear();
            cdaSubstanceAdministration.getAuthors().clear();
            cdaSubstanceAdministration.getInformants().clear();
            cdaSubstanceAdministration.getParticipants().clear();
            cdaSubstanceAdministration.getReferences().clear();
            cdaSubstanceAdministration.getPreconditions().clear();
        }
        else if (cdaObject instanceof Supply)
        {
            Supply cdaSupply = (Supply) cdaObject;
            cdaSupply.getRealmCodes().clear();
            cdaSupply.setTypeId(null);
            cdaSupply.getIds().clear();
            cdaSupply.setCode(null);
            cdaSupply.setText(null);
            cdaSupply.setStatusCode(null);
            cdaSupply.getEffectiveTimes().clear();
            cdaSupply.getPriorityCodes().clear();
            cdaSupply.setRepeatNumber(null);
            cdaSupply.setIndependentInd(null);
            cdaSupply.setQuantity(null);
            cdaSupply.setExpectedUseTime(null);
            cdaSupply.setSubject(null);
            cdaSupply.setProduct(null);
            cdaSupply.getPerformers().clear();
            cdaSupply.getAuthors().clear();
            cdaSupply.getInformants().clear();
            cdaSupply.getParticipants().clear();
            cdaSupply.getReferences().clear();
            cdaSupply.getPreconditions().clear();
        }
        else if (cdaObject instanceof Organizer)
        {
            Organizer cdaOrganizer = (Organizer) cdaObject;
            cdaOrganizer.getRealmCodes().clear();
            cdaOrganizer.setTypeId(null);
            cdaOrganizer.getIds().clear();
            cdaOrganizer.setCode(null);
            cdaOrganizer.setStatusCode(null);
            cdaOrganizer.setEffectiveTime(null);
            cdaOrganizer.setSubject(null);
            cdaOrganizer.getPerformers().clear();
            cdaOrganizer.getAuthors().clear();
            cdaOrganizer.getInformants().clear();
            cdaOrganizer.getParticipants().clear();
            cdaOrganizer.getReferences().clear();
            cdaOrganizer.getPreconditions().clear();
        }
    }

    public void removeEntryRelationship(EntryRelationship cdaEntryRelationship)
    {
        EObject cdaContainer = cdaEntryRelationship.eContainer();
        if (cdaContainer instanceof Act)
        {
            ((Act) cdaContainer).getEntryRelationships().remove(cdaEntryRelationship);
        }
        else if (cdaContainer instanceof Observation)
        {
            ((Observation) cdaContainer).getEntryRelationships().remove(cdaEntryRelationship);
        }
        else if (cdaContainer instanceof Procedure)
        {
            ((Procedure) cdaContainer).getEntryRelationships().remove(cdaEntryRelationship);
        }
        else if (cdaContainer instanceof Encounter)
        {
            ((Encounter) cdaContainer).getEntryRelationships().remove(cdaEntryRelationship);
        }
        else if (cdaContainer instanceof SubstanceAdministration)
        {
            ((SubstanceAdministration) cdaContainer).getEntryRelationships().remove(cdaEntryRelationship);
        }
        else if (cdaContainer instanceof Supply)
        {
            ((Supply) cdaContainer).getEntryRelationships().remove(cdaEntryRelationship);
        }
    }

    public void removeIfEmpty(Section cdaSection)
    {
        if (cdaSection.getEntries().isEmpty())
            removeSection(cdaSection);
        else
            clearSelf(cdaSection);
    }

    public void removeSection(Section cdaSection)
    {
        Component3 cdaComponent3 = (Component3) cdaSection.eContainer();
        StructuredBody cdaStructuredBody = (StructuredBody) cdaComponent3.eContainer();
        cdaStructuredBody.getComponents().remove(cdaComponent3);
    }
}
