package org.noria.cdafhirlib.enumerations;

public enum CDAtoFHIRCodeConversionType {


    ACTIVITY_OBSERVATION_STATUS("ActivityObservation"),
    OBSERVATION_STATUS("ObservationStatus"),
    ADDRESS_USE("AddressUse");

    private String type;

    CDAtoFHIRCodeConversionType(String type){
        this.type = type;
    }


    public static CDAtoFHIRCodeConversionType fromValue(String text) {
        for (CDAtoFHIRCodeConversionType conversionType : CDAtoFHIRCodeConversionType.values()) {
            if (conversionType.type.equalsIgnoreCase(text)) {
                return conversionType;
            }
        }
        return null;
    }

    public String toValue() {
        return this.type;
    }
}
