package org.noria.cdafhirlib.enumerations;

public enum FHIRtoCDACodeConversionType {


    REACTION_STATUS_CODING_VERIFICATION("ReactionStatusCodingVerification");

    private final String type;

    FHIRtoCDACodeConversionType(String type){
        this.type = type;
    }


    public static FHIRtoCDACodeConversionType fromValue(String text) {
        for (FHIRtoCDACodeConversionType conversionType : FHIRtoCDACodeConversionType.values()) {
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
