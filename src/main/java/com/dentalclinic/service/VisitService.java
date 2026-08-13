package com.dentalclinic.service;

import com.dentalclinic.model.Visit;
import com.dentalclinic.repository.RepositoryException;
import com.dentalclinic.repository.VisitRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class VisitService {

    private final VisitRepository visitRepository;

    public VisitService(VisitRepository visitRepository) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository must not be null");
    }

    public Visit createVisit(Visit visit) {
        validateVisit(visit);
        try {
            return visitRepository.save(visit);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to save visit.", exception);
        }
    }

    public Visit getVisit(Long id) {
        requireId(id, "Visit ID is required.");
        try {
            return visitRepository.findById(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load visit.", exception);
        }
    }

    public List<Visit> getPatientVisits(Long patientId) {
        requireId(patientId, "Patient ID is required.");
        try {
            return visitRepository.findByPatientId(patientId);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load patient visits.", exception);
        }
    }

    public List<Visit> getVisitsByDateRange(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        try {
            return visitRepository.findByDateRange(from, to);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load visits for the date range.", exception);
        }
    }

    public void updateVisit(Visit visit) {
        validateVisit(visit);
        try {
            visitRepository.update(visit);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to update visit.", exception);
        }
    }

    public void deleteVisit(Long id) {
        requireId(id, "Visit ID is required.");
        try {
            visitRepository.delete(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to delete visit.", exception);
        }
    }

    private static void validateVisit(Visit visit) {
        if (visit == null) {
            throw new ServiceException("Visit is required.");
        }
        if (visit.getPatientId() == null) {
            throw new ServiceException("Patient ID is required.");
        }
        if (visit.getTt() == null) {
            throw new ServiceException("Visit sequence number is required.");
        }
        if (visit.getNgayKham() == null) {
            throw new ServiceException("Visit date is required.");
        }
        requireText(visit.getTrieuChung(), "Visit symptoms are required.");
        requireText(visit.getChanDoan(), "Visit diagnosis is required.");
        requireText(visit.getPhuongPhapDieuTri(), "Visit treatment method is required.");
        requireText(visit.getBacSiKham(), "Examining dentist is required.");
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
