package org.noria.cdafhirlib.enumerations;

public enum CDAtoFHIRCodeConversionType {


    ACTIVITY_OBSERVATION_STATUS("ActivityObservation"),
    OBSERVATION_STATUS("ObservationStatus"),
    ADDRESS_USE("AddressUse"),
    ENCOUNTER_STATUS("EncounterStatus"),
    ENCOUNTER_TYPE("EncounterType"),
    FAMILY_HISTORY_MEMBER_PERSON_RLT_SUBJ_GENDER("FamilyHistoryMemberPersonRelatedSubjectGender"),
    FAMILY_HISTORY_STATUS("FamilyHistoryStatus"),
    IMMUNIZATION_INDICATION_STATUS("ImmunizationIndicationStatus"),
    IMMUNIZATION_STATUS("ImmunizationStatus"),
    OBSERVATION_CATEGORY("ObservationCategory"),
    PROBLEM_STATUS("ProblemStatus"),
    PROBLEM_TYPE("ProblemType.csv"),
    PROCEDURE_STATUS("ProcedureStatus"),
    RESULT_STATUS("ResultStatus"),
    TELECOM_USE("TelecomUse");


    private final String type;

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
