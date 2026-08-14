package com.dentalclinic.model.report;

import java.time.LocalDate;

public record MedicalBookReportRow(
        int sequence,
        String patientName,
        String gender,
        LocalDate birthDate,
        String identityDocument,
        String healthInsuranceNumber,
        String address,
        String occupation,
        String ethnicity,
        String symptoms,
        String diagnosis,
        String treatment,
        String dentist,
        String note
) {
}
