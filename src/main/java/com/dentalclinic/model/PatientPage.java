package com.dentalclinic.model;

import java.util.List;

public record PatientPage(
        List<Patient> patients,
        long totalElements,
        int pageIndex,
        int pageSize
) {
    public PatientPage {
        patients = List.copyOf(patients);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must not be negative");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) totalElements / pageSize));
    }

    public long firstDisplayedNumber() {
        return patients.isEmpty() ? 0 : (long) pageIndex * pageSize + 1;
    }

    public long lastDisplayedNumber() {
        return patients.isEmpty() ? 0 : firstDisplayedNumber() + patients.size() - 1;
    }
}
