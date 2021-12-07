package org.noria.cdafhirlib.test;

import lombok.extern.log4j.Log4j2;
import org.openhealthtools.mdht.uml.cda.ClinicalDocument;
import org.openhealthtools.mdht.uml.cda.ccd.CCDFactory;
import org.openhealthtools.mdht.uml.cda.ccd.ContinuityOfCareDocument;
import org.openhealthtools.mdht.uml.cda.consol.ConsolFactory;
import org.openhealthtools.mdht.uml.cda.consol.ConsultationNote;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;

import java.io.FileInputStream;

@Log4j2
public class StarterTest {
    public static void main(String[] args) throws Exception {
        CDAUtil.loadPackages();

        // Read a Continuity of Care Document (CCD) instance, which is the official sample CCD instance
        // distributed with C-CDA 2.1 specs, with a few extensions for having a more complete document
        log.info("Start application");
        FileInputStream fis = new FileInputStream("src/test/resources/C-CDA_R2-1_CCD.xml");
        ClinicalDocument cda = CDAUtil.load(fis);
        ContinuityOfCareDocument ccd = CCDFactory.eINSTANCE.createContinuityOfCareDocument().init();
        ConsultationNote cn  = ConsolFactory.eINSTANCE.createConsultationNote();
        log.error("End");

    }
}
