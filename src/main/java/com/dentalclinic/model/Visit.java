package com.dentalclinic.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

public class Visit {

    private Long id;
    private Long patientId;
    private Integer tt;
    private LocalDate ngayKham;
    private String trieuChung;
    private String chanDoan;
    private String phuongPhapDieuTri;
    private String bacSiKham;
    private String ghiChu;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Visit(
            Long patientId,
            Integer tt,
            LocalDate ngayKham,
            String trieuChung,
            String chanDoan,
            String phuongPhapDieuTri,
            String bacSiKham
    ) {
        setPatientId(patientId);
        setTt(tt);
        setNgayKham(ngayKham);
        setTrieuChung(trieuChung);
        setChanDoan(chanDoan);
        setPhuongPhapDieuTri(phuongPhapDieuTri);
        setBacSiKham(bacSiKham);
    }

    public Visit(
            Long id,
            Long patientId,
            Integer tt,
            LocalDate ngayKham,
            String trieuChung,
            String chanDoan,
            String phuongPhapDieuTri,
            String bacSiKham,
            String ghiChu,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(patientId, tt, ngayKham, trieuChung, chanDoan, phuongPhapDieuTri, bacSiKham);
        this.id = id;
        this.ghiChu = ghiChu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = Objects.requireNonNull(patientId, "patientId must not be null");
    }

    public Integer getTt() {
        return tt;
    }

    public void setTt(Integer tt) {
        this.tt = Objects.requireNonNull(tt, "tt must not be null");
    }

    public LocalDate getNgayKham() {
        return ngayKham;
    }

    public void setNgayKham(LocalDate ngayKham) {
        this.ngayKham = Objects.requireNonNull(ngayKham, "ngayKham must not be null");
    }

    public String getTrieuChung() {
        return trieuChung;
    }

    public void setTrieuChung(String trieuChung) {
        this.trieuChung = Objects.requireNonNull(trieuChung, "trieuChung must not be null");
    }

    public String getChanDoan() {
        return chanDoan;
    }

    public void setChanDoan(String chanDoan) {
        this.chanDoan = Objects.requireNonNull(chanDoan, "chanDoan must not be null");
    }

    public String getPhuongPhapDieuTri() {
        return phuongPhapDieuTri;
    }

    public void setPhuongPhapDieuTri(String phuongPhapDieuTri) {
        this.phuongPhapDieuTri = Objects.requireNonNull(
                phuongPhapDieuTri,
                "phuongPhapDieuTri must not be null"
        );
    }

    public String getBacSiKham() {
        return bacSiKham;
    }

    public void setBacSiKham(String bacSiKham) {
        this.bacSiKham = Objects.requireNonNull(bacSiKham, "bacSiKham must not be null");
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
