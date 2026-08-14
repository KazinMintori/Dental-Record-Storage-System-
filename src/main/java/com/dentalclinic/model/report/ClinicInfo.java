package com.dentalclinic.model.report;

public record ClinicInfo(String name, String address, String taxCode) {

    public static ClinicInfo fromEnvironment() {
        return new ClinicInfo(
                environment("DENTAL_CLINIC_NAME", "PHÒNG KHÁM NHA KHOA"),
                environment("DENTAL_CLINIC_ADDRESS", "Chưa cấu hình"),
                environment("DENTAL_CLINIC_TAX_CODE", "Chưa cấu hình")
        );
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
