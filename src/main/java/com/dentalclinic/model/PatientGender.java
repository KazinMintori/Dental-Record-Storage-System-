package com.dentalclinic.model;

import java.util.List;

public enum PatientGender {
    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String displayName;

    PatientGender(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static List<String> displayNames() {
        return List.of(MALE.displayName, FEMALE.displayName, OTHER.displayName);
    }

    public static boolean isSupported(String value) {
        return value != null && displayNames().contains(value);
    }
}
