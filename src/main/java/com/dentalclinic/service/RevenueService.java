package com.dentalclinic.service;

import com.dentalclinic.model.Revenue;
import com.dentalclinic.repository.RepositoryException;
import com.dentalclinic.repository.RevenueRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class RevenueService {

    private final RevenueRepository revenueRepository;

    public RevenueService(RevenueRepository revenueRepository) {
        this.revenueRepository = Objects.requireNonNull(revenueRepository, "revenueRepository must not be null");
    }

    public Revenue createRevenue(Revenue revenue) {
        validateRevenue(revenue);
        try {
            return revenueRepository.save(revenue);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to save revenue entry.", exception);
        }
    }

    public Revenue getRevenue(Long id) {
        requireId(id, "Revenue ID is required.");
        try {
            return revenueRepository.findById(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load revenue entry.", exception);
        }
    }

    public List<Revenue> getVisitRevenue(Long visitId) {
        requireId(visitId, "Visit ID is required.");
        try {
            return revenueRepository.findByVisitId(visitId);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load visit revenue.", exception);
        }
    }

    public List<Revenue> getRevenueByDateRange(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        try {
            return revenueRepository.findByDateRange(from, to);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load revenue for the date range.", exception);
        }
    }

    public BigDecimal calculateRevenueTotal(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        try {
            return revenueRepository.calculateTotalByDateRange(from, to);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to calculate revenue total.", exception);
        }
    }

    public void updateRevenue(Revenue revenue) {
        validateRevenue(revenue);
        try {
            revenueRepository.update(revenue);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to update revenue entry.", exception);
        }
    }

    public void deleteRevenue(Long id) {
        requireId(id, "Revenue ID is required.");
        try {
            revenueRepository.delete(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to delete revenue entry.", exception);
        }
    }

    private static void validateRevenue(Revenue revenue) {
        if (revenue == null) {
            throw new ServiceException("Revenue entry is required.");
        }
        if (revenue.getVisitId() == null) {
            throw new ServiceException("Visit ID is required.");
        }
        if (revenue.getNgayThang() == null) {
            throw new ServiceException("Revenue date is required.");
        }
        requireText(revenue.getDienGiai(), "Revenue description is required.");
        if (revenue.getSoTien() == null) {
            throw new ServiceException("Revenue amount is required.");
        }
        if (revenue.getSoTien().signum() < 0) {
            throw new ServiceException("Revenue amount must not be negative.");
        }
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ServiceException("Both start and end dates are required.");
        }
        if (from.isAfter(to)) {
            throw new ServiceException("Start date must not be after end date.");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(message);
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new ServiceException(message);
        }
    }
}
