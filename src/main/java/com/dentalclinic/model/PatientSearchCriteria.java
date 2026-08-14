package com.dentalclinic.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Database search criteria for the patient directory. Every populated field is
 * combined with AND by the repository.
 */
public record PatientSearchCriteria(
        String name,
        String patientCode,
        String phone,
        LocalDate birthDate,
        String gender
) {

    public PatientSearchCriteria {
        name = normalize(name);
        patientCode = normalize(patientCode);
        phone = normalize(phone);
        gender = normalize(gender);
    }

    public static PatientSearchCriteria empty() {
        return new PatientSearchCriteria(null, null, null, null, null);
    }

    public boolean isEmpty() {
        return name == null && !hasAdvancedFilters();
    }

    public boolean hasAdvancedFilters() {
        return patientCode != null || phone != null || birthDate != null || gender != null;
    }

    public List<String> nameTokens() {
        if (name == null) {
            return List.of();
        }
        return Arrays.stream(name.split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
