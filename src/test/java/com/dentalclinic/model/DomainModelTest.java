package com.dentalclinic.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DomainModelTest {

    @Test
    void patientPageCalculatesNavigationAndDisplayedRange() {
        java.util.List<Patient> patients = java.util.stream.LongStream.rangeClosed(51, 75)
                .mapToObj(id -> new Patient(id, "Bệnh nhân " + id, "Nam", LocalDate.of(1990, 1, 1),
                        null, null, null, null, null, null, null, null))
                .toList();
        PatientPage page = new PatientPage(patients, 75, 1, 50);

        assertEquals(2, page.totalPages());
        assertEquals(51, page.firstDisplayedNumber());
        assertEquals(75, page.lastDisplayedNumber());
    }

    @Test
    void patientRepresentsRequiredAndOptionalDatabaseFields() {
        LocalDate ngaySinh = LocalDate.of(1985, 4, 12);
        Patient patient = new Patient("Nguyen Van An", "Nam", ngaySinh);

        assertEquals("Nguyen Van An", patient.getHoVaTen());
        assertEquals("Nam", patient.getGioiTinh());
        assertEquals(ngaySinh, patient.getNgaySinh());
        assertNull(patient.getSoDienThoai());
        assertNull(patient.getGiayToTuyThan());
        assertNull(patient.getSoTheBhyt());
        assertNull(patient.getDiaChi());
        assertNull(patient.getNgheNghiep());
        assertNull(patient.getDanToc());
        assertNull(patient.getDeletedAt());

        patient.setSoDienThoai("0123456789");
        patient.setGiayToTuyThan("CCCD-001");
        patient.setSoTheBhyt("BHYT-001");
        patient.setDiaChi("Da Nang");
        patient.setNgheNghiep("Giao vien");
        patient.setDanToc("Kinh");

        assertEquals("0123456789", patient.getSoDienThoai());
        assertEquals("CCCD-001", patient.getGiayToTuyThan());
        assertEquals("BHYT-001", patient.getSoTheBhyt());
        assertEquals("Da Nang", patient.getDiaChi());
        assertEquals("Giao vien", patient.getNgheNghiep());
        assertEquals("Kinh", patient.getDanToc());
    }

    @Test
    void patientUsesLocalDateAndOffsetDateTime() {
        LocalDate ngaySinh = LocalDate.of(1992, 8, 20);
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 8, 13, 10, 15, 0, 0, ZoneOffset.ofHours(7));
        OffsetDateTime updatedAt = createdAt.plusHours(1);

        Patient patient = new Patient(
                1L, "Tran Thi Binh", "Nu", ngaySinh, null, null, null, null, null, createdAt, updatedAt
        );

        assertEquals(ngaySinh, patient.getNgaySinh());
        assertEquals(createdAt, patient.getCreatedAt());
        assertEquals(updatedAt, patient.getUpdatedAt());
    }

    @Test
    void visitRepresentsRequiredAndOptionalDatabaseFields() {
        LocalDate ngayKham = LocalDate.of(2026, 8, 13);
        Visit visit = new Visit(25L, 1, ngayKham, "Dau rang", "Sau rang", "Tram rang", "BS. Lan");

        assertEquals(25L, visit.getPatientId());
        assertEquals(1, visit.getTt());
        assertEquals(ngayKham, visit.getNgayKham());
        assertEquals("Dau rang", visit.getTrieuChung());
        assertEquals("Sau rang", visit.getChanDoan());
        assertEquals("Tram rang", visit.getPhuongPhapDieuTri());
        assertEquals("BS. Lan", visit.getBacSiKham());
        assertNull(visit.getGhiChu());
    }

    @Test
    void visitUsesOffsetDateTimeForDatabaseTimestamps() {
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 8, 13, 9, 0, 0, 0, ZoneOffset.ofHours(7));
        OffsetDateTime updatedAt = createdAt.plusMinutes(30);
        Visit visit = new Visit(
                7L, 25L, 2, LocalDate.of(2026, 8, 13), "Dau", "Viem", "Dieu tri", "BS. Lan",
                "Tai kham", createdAt, updatedAt
        );

        assertEquals("Tai kham", visit.getGhiChu());
        assertEquals(createdAt, visit.getCreatedAt());
        assertEquals(updatedAt, visit.getUpdatedAt());
    }

    @Test
    void revenuePreservesDecimalMoneyAndSupportsOptionalSoHieu() {
        LocalDate ngayThang = LocalDate.of(2026, 8, 13);
        BigDecimal soTien = new BigDecimal("1234567.89");
        Revenue revenue = new Revenue(7L, ngayThang, "Dieu tri rang", soTien);

        assertEquals(7L, revenue.getVisitId());
        assertEquals(ngayThang, revenue.getNgayThang());
        assertEquals("Dieu tri rang", revenue.getDienGiai());
        assertEquals(soTien, revenue.getSoTien());
        assertEquals(BigDecimal.class, revenue.getSoTien().getClass());
        assertNull(revenue.getSoHieu());

        revenue.setSoHieu("PT-001");
        assertEquals("PT-001", revenue.getSoHieu());
    }

    @Test
    void revenueDescriptionCanBeNull() {
        Revenue revenue = new Revenue(7L, LocalDate.of(2026, 8, 13), null, new BigDecimal("0"));

        assertNull(revenue.getSoHieu());
        assertNull(revenue.getDienGiai());
    }

    @Test
    void revenueUsesOffsetDateTimeForDatabaseTimestamp() {
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 8, 13, 11, 0, 0, 0, ZoneOffset.ofHours(7));
        Revenue revenue = new Revenue(
                3L, 7L, null, LocalDate.of(2026, 8, 13), "Kham rang", new BigDecimal("250000.00"), createdAt
        );

        assertNull(revenue.getSoHieu());
        assertEquals(createdAt, revenue.getCreatedAt());
    }

    @Test
    void modelsContainExactlyTheDatabaseBackedFields() {
        assertEquals(Map.ofEntries(
                Map.entry("id", Long.class),
                Map.entry("hoVaTen", String.class),
                Map.entry("gioiTinh", String.class),
                Map.entry("ngaySinh", LocalDate.class),
                Map.entry("soDienThoai", String.class),
                Map.entry("giayToTuyThan", String.class),
                Map.entry("soTheBhyt", String.class),
                Map.entry("diaChi", String.class),
                Map.entry("ngheNghiep", String.class),
                Map.entry("danToc", String.class),
                Map.entry("createdAt", OffsetDateTime.class),
                Map.entry("updatedAt", OffsetDateTime.class),
                Map.entry("deletedAt", OffsetDateTime.class)
        ), fieldsOf(Patient.class));

        assertEquals(Map.ofEntries(
                Map.entry("id", Long.class),
                Map.entry("patientId", Long.class),
                Map.entry("tt", Integer.class),
                Map.entry("ngayKham", LocalDate.class),
                Map.entry("trieuChung", String.class),
                Map.entry("chanDoan", String.class),
                Map.entry("phuongPhapDieuTri", String.class),
                Map.entry("bacSiKham", String.class),
                Map.entry("ghiChu", String.class),
                Map.entry("createdAt", OffsetDateTime.class),
                Map.entry("updatedAt", OffsetDateTime.class)
        ), fieldsOf(Visit.class));

        assertEquals(Map.of(
                "id", Long.class,
                "visitId", Long.class,
                "soHieu", String.class,
                "ngayThang", LocalDate.class,
                "dienGiai", String.class,
                "soTien", BigDecimal.class,
                "createdAt", OffsetDateTime.class
        ), fieldsOf(Revenue.class));
    }

    private static Map<String, Class<?>> fieldsOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Field::getType));
    }
}
