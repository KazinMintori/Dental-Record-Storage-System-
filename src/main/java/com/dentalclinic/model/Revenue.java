package com.dentalclinic.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

public class Revenue {

    private Long id;
    private Long visitId;
    private String soHieu;
    private LocalDate ngayThang;
    private String dienGiai;
    private BigDecimal soTien;
    private OffsetDateTime createdAt;

    public Revenue(Long visitId, LocalDate ngayThang, String dienGiai, BigDecimal soTien) {
        setVisitId(visitId);
        setNgayThang(ngayThang);
        setDienGiai(dienGiai);
        setSoTien(soTien);
    }

    public Revenue(
            Long id,
            Long visitId,
            String soHieu,
            LocalDate ngayThang,
            String dienGiai,
            BigDecimal soTien,
            OffsetDateTime createdAt
    ) {
        this(visitId, ngayThang, dienGiai, soTien);
        this.id = id;
        this.soHieu = soHieu;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVisitId() {
        return visitId;
    }

    public void setVisitId(Long visitId) {
        this.visitId = Objects.requireNonNull(visitId, "visitId must not be null");
    }

    public String getSoHieu() {
        return soHieu;
    }

    public void setSoHieu(String soHieu) {
        this.soHieu = soHieu;
    }

    public LocalDate getNgayThang() {
        return ngayThang;
    }

    public void setNgayThang(LocalDate ngayThang) {
        this.ngayThang = Objects.requireNonNull(ngayThang, "ngayThang must not be null");
    }

    public String getDienGiai() {
        return dienGiai;
    }

    public void setDienGiai(String dienGiai) {
        this.dienGiai = dienGiai;
    }

    public BigDecimal getSoTien() {
        return soTien;
    }

    public void setSoTien(BigDecimal soTien) {
        this.soTien = Objects.requireNonNull(soTien, "soTien must not be null");
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
