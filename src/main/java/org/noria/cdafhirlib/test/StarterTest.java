package org.noria.cdafhirlib.test;

import lombok.extern.log4j.Log4j2;
import org.eclipse.emf.common.util.EList;
import org.eclipse.mdht.uml.cda.ClinicalDocument;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.eclipse.mdht.uml.hl7.datatypes.AD;
import org.eclipse.mdht.uml.hl7.datatypes.ADXP;
import org.noria.cdafhirlib.cdaconverter.BasicCDAElementsConverter;
import org.openhealthtools.mdht.uml.cda.consol.*;


import java.io.FileInputStream;

@Log4j2
public class StarterTest {
    public static void main(String[] args) throws Exception {
        CDAUtil.loadPackages();
        BasicCDAElementsConverter basicCDAElementsConverter = new BasicCDAElementsConverter(null);
        //simpleCDATypesConverter.testJSON();


        // Read a Continuity of Care Document (CCD) instance, which is the official sample CCD instance
        // distributed with C-CDA 2.1 specs, with a few extensions for having a more complete document
        log.info("Start application");
        FileInputStream fis = new FileInputStream("src/test/resources/C-CDA_R2-1_CCD.xml");
        ClinicalDocument cda = CDAUtil.load(fis);
        ContinuityOfCareDocument2 ccd = (ContinuityOfCareDocument2) cda;
        MedicationsSection2 ms = ccd.getMedicationsSection2();
        EList<AD> addresses = cda.getPatientRoles().get(0).getAddrs();
        EList<ADXP> cities = addresses.get(0).getCities();
        cities.forEach(e -> System.out.println(e.getText()));
/*
        ContinuityOfCareDocument ccd = ConsolFactory.eINSTANCE.createContinuityOfCareDocument();//CCDFactory.eINSTANCE.createContinuityOfCareDocument().init();
        ConsultationNote cn = ConsolFactory.eINSTANCE.createConsultationNote();
        log.error("End");*/

    }
}
