package com.dentalclinic.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

public class Patient {

    private Long id;
    private String hoVaTen;
    private String gioiTinh;
    private LocalDate ngaySinh;
    private String soDienThoai;
    private String giayToTuyThan;
    private String soTheBhyt;
    private String diaChi;
    private String ngheNghiep;
    private String danToc;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    public Patient(String hoVaTen, String gioiTinh, LocalDate ngaySinh) {
        setHoVaTen(hoVaTen);
        setGioiTinh(gioiTinh);
        setNgaySinh(ngaySinh);
    }

    public Patient(
            Long id,
            String hoVaTen,
            String gioiTinh,
            LocalDate ngaySinh,
            String giayToTuyThan,
            String soTheBhyt,
            String diaChi,
            String ngheNghiep,
            String danToc,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, hoVaTen, gioiTinh, ngaySinh, null, giayToTuyThan, soTheBhyt,
                diaChi, ngheNghiep, danToc, createdAt, updatedAt, null);
    }

    public Patient(
            Long id,
            String hoVaTen,
            String gioiTinh,
            LocalDate ngaySinh,
            String soDienThoai,
            String giayToTuyThan,
            String soTheBhyt,
            String diaChi,
            String ngheNghiep,
            String danToc,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, hoVaTen, gioiTinh, ngaySinh, soDienThoai, giayToTuyThan, soTheBhyt,
                diaChi, ngheNghiep, danToc, createdAt, updatedAt, null);
    }

    public Patient(
            Long id,
            String hoVaTen,
            String gioiTinh,
            LocalDate ngaySinh,
            String soDienThoai,
            String giayToTuyThan,
            String soTheBhyt,
            String diaChi,
            String ngheNghiep,
            String danToc,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime deletedAt
    ) {
        this(hoVaTen, gioiTinh, ngaySinh);
        this.id = id;
        this.soDienThoai = soDienThoai;
        this.giayToTuyThan = giayToTuyThan;
        this.soTheBhyt = soTheBhyt;
        this.diaChi = diaChi;
        this.ngheNghiep = ngheNghiep;
        this.danToc = danToc;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHoVaTen() {
        return hoVaTen;
    }

    public void setHoVaTen(String hoVaTen) {
        this.hoVaTen = Objects.requireNonNull(hoVaTen, "hoVaTen must not be null");
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = Objects.requireNonNull(gioiTinh, "gioiTinh must not be null");
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = Objects.requireNonNull(ngaySinh, "ngaySinh must not be null");
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getGiayToTuyThan() {
        return giayToTuyThan;
    }

    public void setGiayToTuyThan(String giayToTuyThan) {
        this.giayToTuyThan = giayToTuyThan;
    }

    public String getSoTheBhyt() {
        return soTheBhyt;
    }

    public void setSoTheBhyt(String soTheBhyt) {
        this.soTheBhyt = soTheBhyt;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public String getNgheNghiep() {
        return ngheNghiep;
    }

    public void setNgheNghiep(String ngheNghiep) {
        this.ngheNghiep = ngheNghiep;
    }

    public String getDanToc() {
        return danToc;
    }

    public void setDanToc(String danToc) {
        this.danToc = danToc;
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

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
