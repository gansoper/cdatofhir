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
    PROBLEM_TYPE("ProblemType"),
    PROCEDURE_STATUS("ProcedureStatus"),
    RESULT_STATUS("ResultStatus"),
    TELECOM_USE("TelecomUse"),
    ALLERGY_VERIFICATION_STATUS("AllergyVerificationStatus"),
    ALLERGY_CLINICAL_STATUS("AllergyClinicalStatus"),
    ALLERGY_SEVERITY("AllergySeverity"),
    ALLERGY_CRITICALITY("AllergyCriticality"),

    MEDICATION_ACTIVITY_STATUS("MedicationActivityStatus"),

    MEDICATION_DISPENSE_STATUS("MedicationDispenseStatus"),

    MEDICATION_SUPPLY_ORDER_STATUS("MedicationSupplyOrderStatus"),
    MEDICATION_ACTIVITY_STATEMENT_STATUS("MedicationActivityStatementStatus");


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
